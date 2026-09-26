const OK = '20000';

export class SystemAdminApi {
  constructor(baseUrl, fetcher = (...args) => globalThis.fetch(...args)) {
    this.baseUrl = String(baseUrl).replace(/\/$/, '');
    this.fetcher = fetcher;
    this.authorization = '';
  }

  get loggedIn() { return Boolean(this.authorization); }

  logout() { this.authorization = ''; }

  async login(username, password) {
    if (!username || !password) throw new Error('请输入系统用户名和密码');
    const bytes = new TextEncoder().encode(`${username}:${password}`);
    const authorization = `Basic ${btoa(String.fromCharCode(...bytes))}`;
    const data = await this.request('/house/showRetainKey', 'POST', {}, authorization);
    this.authorization = authorization;
    return data;
  }

  async request(path, method = 'POST', body, authorization = this.authorization, includeResponse = false) {
    if (!authorization) throw new Error('请先登录系统管理员账号');
    const options = {
      method,
      headers: { Authorization: authorization, ...(method === 'POST' ? { 'Content-Type': 'application/json' } : {}) },
      ...(method === 'POST' ? { body: JSON.stringify(body ?? {}) } : {})
    };
    let response;
    const controller=new AbortController();
    options.signal=controller.signal;
    const timeout=setTimeout(()=>controller.abort(),12000);
    try { response = await this.fetcher(`${this.baseUrl}${path}`, options); }
    catch (error) {
      if(error?.name==='AbortError') throw new Error('系统接口请求超时，请检查后端地址和网络');
      throw new Error('系统接口连接失败，请检查后端地址、跨域和网络');
    } finally { clearTimeout(timeout); }
    if (response.status === 401 || response.status === 403) {
      if (authorization === this.authorization) this.logout();
      throw new Error('系统账号验证失败或无权限');
    }
    if (!response.ok) throw new Error(`系统接口返回 HTTP ${response.status}`);
    let result;
    try { result = await response.json(); }
    catch (_) { throw new Error('系统接口未返回有效 JSON'); }
    if (String(result.code) !== OK) throw new Error(result.message || '系统操作失败');
    return includeResponse ? result : result.data;
  }

  listRetainKeys() { return this.request('/house/showRetainKey', 'POST'); }
  addRetainKey(key) { return this.request(`/house/addRetainKey/${encodeURIComponent(key)}`, 'POST'); }
  removeRetainKey(key) { return this.request(`/house/removeRetainKey/${encodeURIComponent(key)}`, 'GET'); }
  getUsers(houseId = '') { return this.request('/user/get', 'POST', { houseId }); }
  setRoomLimit(size) { return this.request(`/house/setSize/${size}`, 'GET', undefined, this.authorization, true); }
  editRoom(changes) { return this.request('/house/edit', 'POST', changes); }
  setNeteaseCookie(cookie) { return this.request('/netease/setCookiePost', 'POST', { cookie }); }
}

export function roomEditPayload(values, original = {}) {
  const id = String(values.id || '').trim();
  if (!id) throw new Error('请先选择房间');
  if (values.canDestroy === 'true') return { id, canDestroy: true };
  const payload = { id };
  const name = String(values.name || '').trim();
  const desc = String(values.desc || '').trim();
  if (!name || name.length > 33) throw new Error('房间名称不能为空且不能超过 33 个字符');
  if (desc.length > 133) throw new Error('房间描述不能超过 133 个字符');
  if (name !== original.name) payload.name = name;
  if (desc !== original.desc) payload.desc = desc;
  for (const field of ['needPwd', 'enableStatus', 'forbiddenModiPwd']) {
    if (values[field] === 'true' || values[field] === 'false') payload[field] = values[field] === 'true';
  }
  const password = String(values.password || '').trim();
  if (values.needPwd === 'true' && !password) throw new Error('启用房间密码时，请输入新密码');
  if (values.needPwd === 'false' && password) throw new Error('关闭房间密码时，请清空新密码输入框');
  if (password) {
    if (/[\s?%#&=+]/.test(password)) throw new Error('房间密码不能包含空格、?、%、#、&、=、+');
    payload.password = password;
  }
  const adminPwd = String(values.adminPwd || '').trim();
  if (adminPwd) payload.adminPwd = adminPwd;
  if (Object.keys(payload).length === 1) throw new Error('请先修改至少一项房间设置');
  if (values.canDestroy === 'false') payload.canDestroy = false;
  return payload;
}
