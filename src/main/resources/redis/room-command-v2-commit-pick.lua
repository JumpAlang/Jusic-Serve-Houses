-- Redis v2 atomic pick command. This script must only receive v2 keys that
-- share one Redis Cluster hash slot. It never reads or writes legacy keys.
--
-- KEYS:
--   1 room state HASH
--   2 pick queue LIST (plain JSON values)
--   3 blacklisted song ids SET
--   4 queued/playing song ids SET
--   5 idempotency result HASH
--   6 idempotency insertion-order LIST
--   7 room event STREAM
-- ARGV:
--   1 maximum non-default queue size
--   2 request id
--   3 song id
--   4 plain music JSON
--   5 maximum retained idempotency results
-- RETURN:
--   {status, originalStatus, queueSize, roomVersion, eventId}
-- Codes: 0 accepted, 1 replay, 2 capacity, 3 blacklisted, 4 duplicate,
--        5 room not ready, 6 corrupt state, 7 invalid arguments.

local ACCEPTED = 0
local REPLAY = 1
local CAPACITY_REACHED = 2
local BLACKLISTED = 3
local DUPLICATE = 4
local ROOM_NOT_READY = 5
local CORRUPT_STATE = 6
local INVALID_ARGUMENTS = 7

local function result(code, originalCode, queueSize, roomVersion, eventId)
    return {code, originalCode, queueSize, roomVersion, eventId or ''}
end

local function redis_type(key)
    local reply = redis.call('TYPE', key)
    if type(reply) == 'table' then
        return reply['ok']
    end
    return reply
end

local function is_positive_decimal(value)
    return value ~= false and string.match(value, '^[1-9][0-9]*$') ~= nil
end

local function is_non_negative_decimal(value)
    return value ~= false
            and (value == '0' or string.match(value, '^[1-9][0-9]*$') ~= nil)
end

local function contains_forbidden_identifier_character(value)
    if string.find(value, '[%c{}]') ~= nil then
        return true
    end
    for index = 1, string.len(value) do
        if string.byte(value, index) > 127 then
            return true
        end
    end
    return false
end

if #KEYS ~= 7 or #ARGV ~= 5 then
    return result(INVALID_ARGUMENTS, INVALID_ARGUMENTS, 0, 0, '')
end

if not is_positive_decimal(ARGV[1])
        or tonumber(ARGV[1]) > 100000
        or ARGV[2] == ''
        or string.len(ARGV[2]) > 128
        or contains_forbidden_identifier_character(ARGV[2])
        or ARGV[3] == ''
        or string.len(ARGV[3]) > 512
        or contains_forbidden_identifier_character(ARGV[3])
        or ARGV[4] == ''
        or string.len(ARGV[4]) > 1048576
        or not is_positive_decimal(ARGV[5])
        or tonumber(ARGV[5]) > 100000 then
    return result(INVALID_ARGUMENTS, INVALID_ARGUMENTS, 0, 0, '')
end

local stateType = redis_type(KEYS[1])
if stateType == 'none' then
    return result(ROOM_NOT_READY, ROOM_NOT_READY, 0, 0, '')
end
if stateType ~= 'hash' then
    return result(CORRUPT_STATE, CORRUPT_STATE, 0, 0, '')
end
if redis.call('HGET', KEYS[1], 'schemaVersion') ~= '2' then
    return result(ROOM_NOT_READY, ROOM_NOT_READY, 0, 0, '')
end

local expectedTypes = {'list', 'set', 'set', 'hash', 'list', 'stream'}
for index = 2, 7 do
    local actualType = redis_type(KEYS[index])
    local expectedType = expectedTypes[index - 1]
    if actualType ~= 'none' and actualType ~= expectedType then
        return result(CORRUPT_STATE, CORRUPT_STATE, 0, 0, '')
    end
end

local roomVersionText = redis.call('HGET', KEYS[1], 'roomVersion')
if not is_non_negative_decimal(roomVersionText) then
    return result(CORRUPT_STATE, CORRUPT_STATE, 0, 0, '')
end
local roomVersion = tonumber(roomVersionText)

-- A valid previously persisted result wins even if the room became inactive.
local replayRecord = redis.call('HGET', KEYS[5], ARGV[2])
if replayRecord then
    local originalCodeText, queueSizeText, versionText, eventId =
        string.match(replayRecord, '^([0-9]+)|([0-9]+)|([0-9]+)|(.*)$')
    if not originalCodeText
            or not is_non_negative_decimal(queueSizeText)
            or not is_non_negative_decimal(versionText) then
        return result(CORRUPT_STATE, CORRUPT_STATE, 0, roomVersion, '')
    end
    local originalCode = tonumber(originalCodeText)
    if originalCode ~= ACCEPTED
            and originalCode ~= CAPACITY_REACHED
            and originalCode ~= BLACKLISTED
            and originalCode ~= DUPLICATE then
        return result(CORRUPT_STATE, CORRUPT_STATE, 0, roomVersion, '')
    end
    return result(REPLAY, originalCode, tonumber(queueSizeText), tonumber(versionText), eventId)
end

if redis.call('HGET', KEYS[1], 'status') ~= 'ACTIVE' then
    return result(ROOM_NOT_READY, ROOM_NOT_READY, 0, roomVersion, '')
end

local maxQueueSize = tonumber(ARGV[1])
local maxIdempotencyRecords = tonumber(ARGV[5])
local queueSize = redis.call('LLEN', KEYS[2])

local function remember(code, committedQueueSize, committedVersion, eventId)
    local record = tostring(code)
            .. '|' .. tostring(committedQueueSize)
            .. '|' .. tostring(committedVersion)
            .. '|' .. (eventId or '')
    redis.call('HSET', KEYS[5], ARGV[2], record)
    redis.call('RPUSH', KEYS[6], ARGV[2])
    local overflow = redis.call('LLEN', KEYS[6]) - maxIdempotencyRecords
    for index = 1, overflow do
        local expiredRequestId = redis.call('LPOP', KEYS[6])
        if expiredRequestId then
            redis.call('HDEL', KEYS[5], expiredRequestId)
        end
    end
    return result(code, code, committedQueueSize, committedVersion, eventId)
end

if queueSize >= maxQueueSize then
    return remember(CAPACITY_REACHED, queueSize, roomVersion, '')
end
if redis.call('SISMEMBER', KEYS[3], ARGV[3]) == 1 then
    return remember(BLACKLISTED, queueSize, roomVersion, '')
end
local playingSongId = redis.call('HGET', KEYS[1], 'playingSongId')
if playingSongId == ARGV[3]
        or redis.call('SISMEMBER', KEYS[4], ARGV[3]) == 1 then
    return remember(DUPLICATE, queueSize, roomVersion, '')
end

redis.call('LPUSH', KEYS[2], ARGV[4])
redis.call('SADD', KEYS[4], ARGV[3])
local committedVersion = redis.call('HINCRBY', KEYS[1], 'roomVersion', 1)
local eventId = redis.call(
        'XADD', KEYS[7], 'MAXLEN', '~', 10000, '*',
        'eventType', 'MUSIC_PICKED',
        'roomVersion', tostring(committedVersion),
        'requestId', ARGV[2],
        'songId', ARGV[3],
        'musicJson', ARGV[4])

return remember(ACCEPTED, queueSize + 1, committedVersion, eventId)
