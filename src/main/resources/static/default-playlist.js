const escape = value => String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
export const defaultSongKey = song => (song.source || 'wy') + ':' + String(song.id);
export function defaultPlaylistRows(songs, favorites = []) {
  if (!songs.length) return '<div class="system-message">当前页没有默认歌曲</div>';
  return songs.map((song,index) => {
    const platform = song.source === 'qq' ? 'QQ' : '网易';
    const name = song.name || '歌曲 ID：' + song.id;
    const album = typeof song.album === 'string' ? song.album : song.album?.name;
    const duration = Number(song.duration);
    const time = duration > 0 ? Math.floor(duration/60000) + ':' + String(Math.floor(duration/1000)%60).padStart(2,'0') : '';
    const meta = [platform, song.artist, album, time].filter(Boolean).join(' · ');
    const picture = /^https?:\/\//i.test(song.pictureUrl || '') ? song.pictureUrl : '';
    const favorite = favorites.some(item => defaultSongKey(item) === defaultSongKey(song));
    return '<div class="result-row"><span class="result-art">' + (picture ? '<img loading="lazy" src="' + escape(picture) + '" alt="歌曲封面">' : '♪')
      + '</span><span><span class="song-title-inline"><strong title="' + escape(name) + '">' + escape(name) + '</strong>'
      + '<button class="song-id-copy compact" type="button" data-copy-song-id="' + escape(song.id) + '" aria-label="复制歌曲 ID ' + escape(song.id) + '" title="歌曲 ID：' + escape(song.id) + '，点击复制">ID</button></span><small>' + escape(meta) + '</small></span>'
      + '<span class="form-actions" style="margin:0"><button class="primary-button" type="button" data-command="default-pick" data-index="' + index + '">点歌</button>'
      + '<button class="secondary-button" type="button" data-command="default-favorite" data-index="' + index + '">' + (favorite ? '取消收藏' : '收藏') + '</button></span></div>';
  }).join('');
}
