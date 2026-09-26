export function describeRoomState(settings) {
  if (!settings) return '房间状态待同步';
  const rate = typeof settings.voteRate === 'number' && Number.isFinite(settings.voteRate)
    ? String(Math.round(settings.voteRate * 10000) / 100) + '%' : '待同步';
  const flag = (key) => settings[key] === true ? '开启' : settings[key] === false ? '关闭' : '待同步';
  return '切歌率 ' + rate + ' · 点赞排序 ' + flag('goodModel')
    + ' · 随机播放 ' + flag('randomModel') + ' · 单曲循环 ' + flag('musicCircleModel')
    + ' · 列表循环 ' + flag('listCircleModel')
    + ' · ' + (settings.enableSearch === true ? '允许点歌' : settings.enableSearch === false ? '禁止点歌' : '点歌待同步')
    + ' · ' + (settings.enableSwitch === true ? '允许切歌' : settings.enableSwitch === false ? '禁止切歌' : '切歌待同步');
}
