const MESSAGE_TYPES = new Set([
  'NOTICE','ONLINE','CHAT','PICK','MUSIC','SETTING_NAME','AUTH','AUTH_ROOT','AUTH_ADMIN',
  'SEARCH','SEARCH_PICTURE','VOLUMN','GOODMODEL','SEARCH_HOUSE','ENTER_HOUSE',
  'ENTER_HOUSE_START','ADD_HOUSE','ADD_HOUSE_START','SEARCH_SONGLIST','SEARCH_USER',
  'ANNOUNCEMENT','HOUSE_USER','CIRCLEMODEL','LISTMODEL','ROOM_STATE','DEFAULT_PLAYLIST','DEFAULT_PLAYLIST_CHANGED'
]);

const SOURCE_CODES = { '网易':'wy', 'QQ':'qq', '酷我':'kw', '酷狗':'kg', '咪咕':'mg' };
const cleanNickname = (value = '') => {
  if(value==null)return '';
  const name=String(value).trim().replace(/(?:\(\*\))+$/g, '').trim();
  if(/^(?:null|undefined|anonymous)$/i.test(name))return '';
  const ip='(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)';
  if(new RegExp(`^${ip}$`).test(name))return '';
  return name.replace(new RegExp(`(?:\\(${ip}\\))+$`),'').trim();
};

export class JusicBackend {
  constructor(options = {}) {
    this.baseUrl = (options.baseUrl || this.resolveBaseUrl()).replace(/\/$/, '');
    this.listeners = new Map();
    this.socket = null;
    this.stomp = null;
    this.room = null;
    this.connected = false;
    this.manualClose = false;
    this.reconnectTimer = null;
    this.reconnectAttempts = 0;
  }

  resolveBaseUrl() {
    const configured = new URLSearchParams(location.search).get('api');
    if (configured) return configured;
    return ['localhost','127.0.0.1'].includes(location.hostname)
      ? 'https://tx.alang.run/api'
      : 'https://tx.alang.run/api';
  }

  on(event, listener) {
    if (!this.listeners.has(event)) this.listeners.set(event, new Set());
    this.listeners.get(event).add(listener);
    return () => this.listeners.get(event)?.delete(listener);
  }

  emit(event, payload) {
    this.listeners.get(event)?.forEach((listener) => listener(payload));
    this.listeners.get('*')?.forEach((listener) => listener({ event, payload }));
  }

  async api(path, payload = {}) {
    const response = await fetch(`${this.baseUrl}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', AccessToken: 'token' },
      body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const result = await response.json();
    if (String(result.code) !== '20000') throw new Error(result.message || '服务器请求失败');
    return result.data;
  }

  async getRooms() { return this.api('/house/search', {}); }
  async getRoom(id) { return this.api('/house/get', { id }); }
  async getMiniCode(id) { return this.api('/house/getMiniCode', { id }); }
  async createRoom(payload) { return this.api('/house/add', payload); }
  async verifyRoom(id, password = '') { return this.api('/house/enter', { id, password }); }

  adoptRoom(room) {
    this.room = { ...(this.room || {}), ...room };
  }

  async connect(room) {
    if (!window.SockJS || !window.Stomp) throw new Error('实时通信组件加载失败');
    await this.disconnect(true);
    this.room = { ...room };
    this.manualClose = false;
    this.emit('connection', { status: 'connecting', room: this.room });
    const query = new URLSearchParams({
      houseId: room.id || '', housePwd: room.password || '', connectType: room.connectType || 'enter'
    });
    const socket = new window.SockJS(`${this.baseUrl}/server?${query}`);
    const stomp = window.Stomp.over(socket);
    stomp.debug = () => {};
    this.socket = socket;
    this.stomp = stomp;
    socket.addEventListener('message', (event) => { if (this.socket === socket) this.handleRawMessage(event.data); });
    socket.addEventListener('close', (event) => { if (this.socket === socket) this.handleClose(event); });

    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('连接房间超时')), 10000);
      stomp.connect({}, () => {
        clearTimeout(timeout);
        this.connected = true;
        this.reconnectAttempts = 0;
        stomp.subscribe('/topic/chat', (response) => {
          try {
            const body = JSON.parse(response.body);
            if (body?.data || body?.message) this.emit('notice', body.data || body.message);
          } catch (_) {}
        });
        stomp.subscribe('/topic/music/order', () => {});
        this.emit('connection', { status: 'connected', room: this.room });
        const nickname = cleanNickname(localStorage.getItem('USER_NAME'));
        if (nickname) this.command('setting.name', { name: nickname }).catch(() => {});
        resolve(this.room);
      }, (error) => {
        clearTimeout(timeout);
        this.connected = false;
        this.emit('connection', { status: 'error', error });
        reject(new Error('房间连接失败'));
      });
    });
  }

  async disconnect(manual = true) {
    this.manualClose = manual;
    clearTimeout(this.reconnectTimer);
    const stomp = this.stomp;
    this.stomp = null;
    this.connected = false;
    if (stomp?.connected) {
      await new Promise((resolve) => { try { stomp.disconnect(resolve); } catch (_) { resolve(); } });
    }
    try { this.socket?.close(); } catch (_) {}
    this.socket = null;
    if (manual) this.emit('connection', { status: 'disconnected', room: this.room });
  }

  handleClose(event) {
    this.connected = false;
    this.emit('connection', { status: 'disconnected', room: this.room, event });
    if (this.manualClose || !this.room) return;
    clearTimeout(this.reconnectTimer);
    const wait = Math.min(8000, 800 * (2 ** this.reconnectAttempts++));
    this.emit('connection', { status: 'reconnecting', delay: wait, room: this.room });
    this.reconnectTimer = setTimeout(() => this.connect(this.room).catch(() => {}), wait);
  }

  handleRawMessage(source) {
    if (typeof source !== 'string') return;
    const lines = source.split('\n');
    const type = lines[0];
    if (!MESSAGE_TYPES.has(type)) return;
    let content;
    try { content = JSON.parse(lines[lines.length - 1]); } catch (_) { return; }
    const data = content?.data;
    const map = {
      DEFAULT_PLAYLIST:'default-playlist', DEFAULT_PLAYLIST_CHANGED:'default-playlist-changed', ROOM_STATE:'room-state', ONLINE:'online', CHAT:'chat', PICK:'queue', MUSIC:'music', VOLUMN:'volume',
      NOTICE:'notice-message', ANNOUNCEMENT:'announcement', SEARCH:'search-results',
      SEARCH_SONGLIST:'playlist-results', SEARCH_USER:'user-results',
      SEARCH_HOUSE:'rooms', SEARCH_PICTURE:'picture-results', HOUSE_USER:'members',
      SETTING_NAME:'nickname', AUTH_ADMIN:'auth-admin', AUTH_ROOT:'auth-root',
      GOODMODEL:'good-mode', CIRCLEMODEL:'circle-mode', LISTMODEL:'list-mode',
      ADD_HOUSE:'room-created', ADD_HOUSE_START:'room-create-start', ENTER_HOUSE:'room-entered'
    };
    this.emit(map[type] || type.toLowerCase(), { ...content, data });
  }

  requireConnection() {
    if (!this.connected || !this.stomp?.connected) throw new Error('请先进入并连接房间');
  }

  send(destination, body = '') {
    this.requireConnection();
    this.stomp.send(destination, {}, typeof body === 'string' ? body : JSON.stringify(body));
    return Promise.resolve({ ok: true, destination });
  }

  command(type, payload = {}) {
    const source = SOURCE_CODES[payload.source] || payload.source || 'wy';
    const now = Date.now();
    const commands = {
      'chat.send': ['/chat', { content: payload.text, sendTime: now }],
      'chat.notice': ['/chat/announce', { content: payload.text }],
      'chat.contactAdmin': ['/mail/send', { content: payload.text, sendTime: now }],
      'chat.picture': ['/chat', { content: `picture:${payload.url}`, sendTime: now }],
      'picture.search': ['/chat/picture/search', { content: payload.keyword || '', pageSize: payload.pageSize || 12, pageIndex: payload.pageIndex || 1, sendTime: now }],
      'music.skipVote': ['/music/skip/vote', {}],
      'music.search': ['/music/search', { name: payload.keyword || '', source, pageIndex: payload.pageIndex || 1, pageSize: payload.pageSize || 20, sendTime: now }],
      'music.pick': ['/music/pick', { name: payload.name, id: payload.id, source, quality: payload.quality || '320k', sendTime: now }],
      // 旧版删除面板始终把输入框中的歌曲 ID 作为字符串发送；后端的点歌归属表也按字符串键匹配。
      'music.delete': ['/music/delete', { id: String(payload.id ?? ''), sendTime: now }],
      'music.top': ['/music/top', { id: payload.id, sendTime: now }],
      'music.good': [`/music/good/${payload.id}`, {}],
      'music.clear': ['/music/clear', ''],
      'player.volume': [`/music/volumn/${payload.value}`, ''],
      'setting.name': ['/setting/name', { name: cleanNickname(payload.name), sendTime: now }],
      'house.enter': ['/house/enter', { id:payload.id, password:payload.password || '' }],
      'house.create': ['/house/add', { name:payload.name, desc:payload.desc, needPwd:Boolean(payload.needPwd), password:payload.password || '', enableStatus:Boolean(payload.enableStatus), retainKey:payload.retainKey || '' }],
      'house.members': ['/house/houseuser', {}],
      'playlist.search': ['/music/searchsonglist', { name: payload.keyword || '', source, pageIndex: payload.pageIndex || 1, pageSize: payload.pageSize || 20, sendTime: now }],
      'user.search': ['/music/searchuser', { nickname: payload.keyword || '', source, pageIndex: payload.pageIndex || 1, pageSize: payload.pageSize || 20, sendTime: now }],
      'playlist.default': ['/music/defaultPlaylist', {pageIndex:payload.pageIndex || 1,pageSize:payload.pageSize || 30,requestId:payload.requestId}],
      'playlist.append': ['/music/setDefaultPlaylist', { id: payload.id, source }],
      'auth.admin': ['/auth/admin', { password: payload.password, sendTime: now }],
      'auth.root': ['/auth/root', { password: payload.password, sendTime: now }],
      // 旧版后端直接从 STOMP destination 读取通知文本；URL 编码会把中文原样广播成 %E...。
      'chat.globalNotice': [`/chat/notice/${String(payload.text || '')}`, {}],
      'blacklist.music': ['/music/blackmusic', ''],
      'blacklist.user': ['/chat/blackuser', ''],
      'auth.adminPassword': [`/auth/adminpwd/${encodeURIComponent(payload.password)}`, ''],
      'admin.vote': [`/music/vote/${payload.value}`, ''],
      'admin.raw': [payload.destination, payload.body || '']
    };
    const config = commands[type];
    if (!config) return Promise.reject(new Error(`未知命令：${type}`));
    return this.send(config[0], config[1]);
  }
}
