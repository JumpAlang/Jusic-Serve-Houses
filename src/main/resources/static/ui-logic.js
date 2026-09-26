export function sortRoomsLikeLegacy(rooms = []) {
  return [...rooms].sort((left, right) => {
    const leftActive = Number(left?.population || left?.count || 0) > 0;
    const rightActive = Number(right?.population || right?.count || 0) > 0;
    if (leftActive !== rightActive) return leftActive ? -1 : 1;
    const leftLocked = Boolean(left?.needPwd ?? left?.locked);
    const rightLocked = Boolean(right?.needPwd ?? right?.locked);
    if (leftLocked !== rightLocked) return leftLocked ? 1 : -1;
    return Number(right?.population || right?.count || 0) - Number(left?.population || left?.count || 0);
  });
}

const MASKED_IPV4_SOURCE='(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)';
const RAW_MASKED_IPV4=new RegExp(`^${MASKED_IPV4_SOURCE}$`);
const WRAPPED_MASKED_IPV4=new RegExp(`\\((${MASKED_IPV4_SOURCE})\\)`,'g');
const MASKED_IPV4_SUFFIX=new RegExp(`((?:\\(${MASKED_IPV4_SOURCE}\\))+)$`);

function splitGeneratedIdentity(value=''){
  const name=String(value).trim().replace(/(?:\(\*\))+$/g,'').trim();
  if(RAW_MASKED_IPV4.test(name))return {base:'',ip:name,wrapped:false};
  const suffix=name.match(MASKED_IPV4_SUFFIX)?.[1] || '';
  if(!suffix)return null;
  const ips=[...suffix.matchAll(WRAPPED_MASKED_IPV4)].map((match)=>match[1]);
  if(!ips.length)return null;
  return {base:name.slice(0,-suffix.length).trim(),ip:ips.at(-1),wrapped:true};
}

export function generatedIdentity(value = '') {
  if(value==null)return '';
  const identity=splitGeneratedIdentity(value);
  if(!identity)return '';
  return identity.base?`${identity.base}(${identity.ip})`:(identity.wrapped?`(${identity.ip})`:identity.ip);
}

export function normalizedNickname(value = '') {
  if (value == null) return '';
  const name=String(value).trim().replace(/(?:\(\*\))+$/g, '').trim();
  if(/^(?:null|undefined|anonymous)$/i.test(name))return '';
  return generatedIdentity(name) || name;
}

export function persistentNickname(value = '') {
  if(value==null)return '';
  const raw=String(value).trim().replace(/(?:\(\*\))+$/g,'').trim();
  const identity=splitGeneratedIdentity(raw);
  const name=identity?identity.base:normalizedNickname(raw);
  return /^(?:null|undefined|anonymous)$/i.test(name)?'':name;
}

export function historyBelongsToUser(song = {}, nickname = '') {
  if (song.mine === true) return true;
  const current = normalizedNickname(nickname);
  const picker = normalizedNickname(song.nickName || song.picker || song.userName || '');
  return Boolean(current && picker && current === picker);
}

export function filterHistoryItems(items = [], { mode = 'all', nickname = '', query = '' } = {}) {
  const keyword = String(query).trim().toLowerCase();
  return items.filter((song) => {
    if (mode === 'mine' && !historyBelongsToUser(song, nickname)) return false;
    const picker = song.nickName || song.picker || song.userName || '';
    return `${song.name || ''} ${song.artist || ''} ${picker}`.toLowerCase().includes(keyword);
  });
}

export function isMusicChatMessage(message = {}) {
  const content = String(message.content ?? message.text ?? '');
  return message.category === 'music' || content.startsWith('点歌 ') || content === '点歌成功';
}

export function filterChatMessages(messages = [], mode = 'all') {
  if (mode === 'all') return messages;
  if (mode === 'music') return messages.filter(isMusicChatMessage);
  if (mode === 'chat') return messages.filter((message) => !isMusicChatMessage(message));
  return messages;
}

export function isSelfChatMessage(message = {}, { sessionId = '', nickname = '' } = {}) {
  if (message.me === true) return true;
  const currentSession = String(sessionId || '').trim();
  if (currentSession && message.sessionId) return String(message.sessionId).trim() === currentSession;
  const currentName = normalizedNickname(nickname);
  const messageName = normalizedNickname(message.name || message.nickName || '');
  return Boolean(currentName && messageName && currentName === messageName);
}

export function sessionIdFromPickedQueue(queue = [], pickedIds = new Set(), nickname = '') {
  const ownIds = new Set([...pickedIds].map(String));
  const matches = (Array.isArray(queue) ? queue : []).filter((song) =>
    song?.sessionId && ((song?.id != null && ownIds.has(String(song.id))) || (song?.name && ownIds.has(String(song.name))))
  );
  const ownName = normalizedNickname(nickname);
  const namedMatches = ownName
    ? matches.filter((song) => persistentNickname(song.nickName || song.picker) === ownName || normalizedNickname(song.nickName || song.picker) === ownName)
    : matches;
  const sessions = [...new Set(namedMatches.map((song) => String(song.sessionId).trim()).filter(Boolean))];
  return sessions.length === 1 ? sessions[0] : '';
}

export function matchesPendingPickChat(message = {}, pick = {}, now = Date.now()) {
  if (!pick || !Number.isFinite(pick.at) || now - pick.at < 0 || now - pick.at > 15000) return false;
  const content = String(message.content ?? message.text ?? '').trim();
  if (!/^点歌\s+/.test(content)) return false;
  const detail = content.replace(/^点歌\s+/, '').trim();
  const id = String(pick.id ?? '').trim();
  const name = String(pick.name ?? '').trim();
  return Boolean((id && (detail === id || detail.startsWith(`${id} `) || detail.includes(`《${id}》`))) ||
    (name && (detail === name || detail.startsWith(`${name} `) || detail.includes(`《${name}》`))));
}

export function waitingQueueItems(queue = []) {
  return Array.isArray(queue) ? queue.slice(1) : [];
}
