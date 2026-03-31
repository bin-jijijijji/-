const App = {
  token: null,
  roleNames: [],
  videoAssets: [],
  currentVideoAssetId: null,
  currentRoi: null, // {id,x1,y1,x2,y2}
  roiList: [],
  draw: {
    dragging: false,
    startX: 0,
    startY: 0,
    curX: 0,
    curY: 0,
    rect: null
  },
  stompClient: null,

  async login() {
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    document.getElementById('loginMsg').textContent = '登录中...';

    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const json = await res.json();
    if (!res.ok || json.code !== 0) {
      document.getElementById('loginMsg').textContent = json.msg || '登录失败';
      return;
    }

    this.token = json.data.token;
    this.roleNames = json.data.roles || [];
    localStorage.setItem('token', this.token);
    localStorage.setItem('roles', JSON.stringify(this.roleNames));
    this.initAfterLogin();
  },

  async initAfterLogin() {
    document.getElementById('loginPanel').style.display = 'none';
    document.getElementById('appPanel').style.display = 'block';
    await this.loadVideoAssets();
    this.setupWebSocket();
    this.hydrateIdentityList();
  },

  loadAuthHeaders() {
    return { 'Authorization': 'Bearer ' + this.token };
  },

  async loadVideoAssets() {
    const res = await fetch('/api/video-assets', { headers: this.loadAuthHeaders() });
    const json = await res.json();
    if (!res.ok || json.code !== 0) {
      alert(json.msg || '加载视频列表失败');
      return;
    }
    this.videoAssets = json.data || [];
    const grid = document.getElementById('videoGrid');
    grid.innerHTML = '';

    this.videoAssets.forEach(a => {
      const v = document.createElement('video');
      v.src = a.fileUrl;
      v.controls = true;
      v.dataset.id = String(a.id);
      v.title = a.title + ' (' + a.cameraName + ')';
      v.addEventListener('click', () => this.selectVideoAsset(a.id));
      grid.appendChild(v);
    });

    if (this.videoAssets.length > 0) {
      await this.selectVideoAsset(this.videoAssets[0].id);
    }
  },

  async selectVideoAsset(videoAssetId) {
    this.currentVideoAssetId = videoAssetId;
    const asset = this.videoAssets.find(x => x.id === videoAssetId);
    document.getElementById('currentVideoTitle').textContent = asset ? asset.title : ('Asset ' + videoAssetId);

    const mainVideo = document.getElementById('mainVideo');
    mainVideo.src = asset.fileUrl;
    mainVideo.currentTime = 0;

    // clear and load rois
    this.draw.rect = null;
    this.currentRoi = null;
    await this.loadRois(videoAssetId);
    this.refreshRoiSelect();
    this.setupRoiCanvas();
  },

  async loadRois(videoAssetId) {
    const res = await fetch(`/api/video-assets/${videoAssetId}/rois`, { headers: this.loadAuthHeaders() });
    const json = await res.json();
    if (!res.ok || json.code !== 0) {
      alert(json.msg || '加载ROI失败');
      return;
    }
    this.roiList = json.data || [];
    this.currentRoi = this.roiList.length ? {
      id: this.roiList[0].id,
      x1: this.roiList[0].x1, y1: this.roiList[0].y1,
      x2: this.roiList[0].x2, y2: this.roiList[0].y2,
    } : null;
  },

  refreshRoiSelect() {
    const sel = document.getElementById('roiSelect');
    sel.innerHTML = '';
    if (!this.roiList.length) return;

    this.roiList.forEach(roi => {
      const opt = document.createElement('option');
      opt.value = String(roi.id);
      opt.textContent = roi.name + ` [${roi.x1.toFixed(2)},${roi.y1.toFixed(2)}]`;
      sel.appendChild(opt);
    });
    sel.onchange = () => {
      const rid = Number(sel.value);
      const roi = this.roiList.find(r => r.id === rid);
      if (roi) {
        this.currentRoi = { id: roi.id, x1: roi.x1, y1: roi.y1, x2: roi.x2, y2: roi.y2 };
        this.drawRectOnCanvas(this.currentRoi);
      }
    };
    sel.value = String(this.currentRoi.id);
    this.drawRectOnCanvas(this.currentRoi);
  },

  setupRoiCanvas() {
    const canvas = document.getElementById('roiCanvas');
    const video = document.getElementById('mainVideo');
    const wrap = document.getElementById('roiWrap');

    const syncSize = () => {
      const rect = video.getBoundingClientRect();
      // Use client pixels for overlay, normalized coords are scale-invariant.
      canvas.width = Math.max(1, Math.round(rect.width));
      canvas.height = Math.max(1, Math.round(rect.height));
      canvas.style.width = canvas.width + 'px';
      canvas.style.height = canvas.height + 'px';
      this.drawRectOnCanvas(this.currentRoi);
    };

    // When metadata loads we know the video element can be sized.
    video.addEventListener('loadedmetadata', syncSize, { once: true });
    window.addEventListener('resize', syncSize);

    canvas.onmousedown = (e) => {
      if (!this.currentVideoAssetId) return;
      const rect = canvas.getBoundingClientRect();
      const x = (e.clientX - rect.left) / rect.width;
      const y = (e.clientY - rect.top) / rect.height;
      this.draw.dragging = true;
      this.draw.startX = x;
      this.draw.startY = y;
      this.draw.curX = x;
      this.draw.curY = y;
      this.draw.rect = { x1: x, y1: y, x2: x, y2: y };
      this.redraw();
    };

    canvas.onmousemove = (e) => {
      if (!this.draw.dragging) return;
      const rect = canvas.getBoundingClientRect();
      const x = (e.clientX - rect.left) / rect.width;
      const y = (e.clientY - rect.top) / rect.height;
      this.draw.curX = x;
      this.draw.curY = y;
      this.draw.rect = { x1: this.draw.startX, y1: this.draw.startY, x2: x, y2: y };
      this.redraw();
    };

    canvas.onmouseup = () => {
      this.draw.dragging = false;
      if (!this.draw.rect) return;
      const x1 = Math.min(this.draw.rect.x1, this.draw.rect.x2);
      const y1 = Math.min(this.draw.rect.y1, this.draw.rect.y2);
      const x2 = Math.max(this.draw.rect.x1, this.draw.rect.x2);
      const y2 = Math.max(this.draw.rect.y1, this.draw.rect.y2);
      const rect = { id: null, x1, y1, x2, y2 };
      if ((x2 - x1) < 0.001 || (y2 - y1) < 0.001) {
        this.draw.rect = null;
        this.redraw();
        return;
      }
      this.draw.rect = rect;
      this.currentRoi = { id: this.currentRoi ? this.currentRoi.id : null, x1, y1, x2, y2 };
      this.drawRectOnCanvas(this.draw.rect);
    };
  },

  redraw() {
    const canvas = document.getElementById('roiCanvas');
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    // show current rectangle being dragged
    const rect = this.draw.rect;
    if (!rect) return;
    this.paintNormalizedRect(ctx, rect.x1, rect.y1, rect.x2, rect.y2, 'rgba(255,0,0,0.8)');
  },

  drawRectOnCanvas(roi) {
    const canvas = document.getElementById('roiCanvas');
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    if (!roi) return;
    this.paintNormalizedRect(ctx, roi.x1, roi.y1, roi.x2, roi.y2, 'rgba(0,255,0,0.8)');
  },

  paintNormalizedRect(ctx, x1, y1, x2, y2, color) {
    const w = ctx.canvas.width;
    const h = ctx.canvas.height;
    const px1 = Math.round(x1 * w);
    const py1 = Math.round(y1 * h);
    const px2 = Math.round(x2 * w);
    const py2 = Math.round(y2 * h);
    ctx.strokeStyle = color;
    ctx.lineWidth = 2;
    ctx.strokeRect(px1, py1, px2 - px1, py2 - py1);
  },

  async saveRoi() {
    if (!this.currentVideoAssetId) return;
    if (!this.draw.rect) {
      alert('请先拖拽绘制ROI矩形');
      return;
    }
    const roiName = document.getElementById('roiName').value || 'ROI-1';
    const payload = {
      name: roiName,
      x1: this.draw.rect.x1,
      y1: this.draw.rect.y1,
      x2: this.draw.rect.x2,
      y2: this.draw.rect.y2
    };

    const res = await fetch(`/api/video-assets/${this.currentVideoAssetId}/rois`, {
      method: 'POST',
      headers: Object.assign({}, this.loadAuthHeaders(), { 'Content-Type': 'application/json' }),
      body: JSON.stringify(payload)
    });
    const json = await res.json();
    if (!res.ok || json.code !== 0) {
      alert(json.msg || '保存ROI失败');
      return;
    }

    await this.loadRois(this.currentVideoAssetId);
    this.refreshRoiSelect();
    document.getElementById('identityMsg').textContent = 'ROI保存成功';
  },

  async startJob() {
    if (!this.currentVideoAssetId || !this.currentRoi || !this.currentRoi.id) {
      alert('请先选择ROI');
      return;
    }
    const threshold = Number(document.getElementById('threshold').value);
    const payload = {
      videoAssetId: this.currentVideoAssetId,
      roiRectangleId: this.currentRoi.id,
      threshold: threshold
    };

    const res = await fetch('/api/jobs/start', {
      method: 'POST',
      headers: Object.assign({}, this.loadAuthHeaders(), { 'Content-Type': 'application/json' }),
      body: JSON.stringify(payload)
    });
    const json = await res.json();
    if (!res.ok || json.code !== 0) {
      alert(json.msg || '开始识别失败');
      return;
    }
    alert('识别任务已创建：jobId=' + json.data.id);
  },

  async uploadIdentity() {
    const name = document.getElementById('idName').value;
    const listType = document.getElementById('idListType').value;
    const input = document.getElementById('idImages');
    const files = input.files;
    if (!files || files.length === 0) {
      alert('请至少上传一张图片');
      return;
    }

    const form = new FormData();
    form.append('name', name);
    form.append('listType', listType);
    for (let i = 0; i < files.length; i++) {
      form.append('images', files[i]);
    }

    const res = await fetch('/api/identities', {
      method: 'POST',
      headers: this.loadAuthHeaders(),
      body: form
    });

    const json = await res.json();
    const msgEl = document.getElementById('identityMsg');
    if (!res.ok || json.code !== 0) {
      msgEl.textContent = json.msg || '上传失败';
      return;
    }
    msgEl.textContent = '上传完成（请等待识别服务处理 embedding）';
  },

  async uploadVideoAsset() {
    const roles = this.roleNames || [];
    if (!roles.includes('ADMIN')) {
      document.getElementById('videoUploadMsg').textContent = '需要 ADMIN 权限才能上传视频';
      return;
    }

    const cameraName = document.getElementById('videoCameraName').value;
    const title = document.getElementById('videoTitle').value;
    const input = document.getElementById('videoFileInput');
    const file = input.files && input.files.length ? input.files[0] : null;
    if (!file) {
      document.getElementById('videoUploadMsg').textContent = '请选择视频文件';
      return;
    }

    const form = new FormData();
    form.append('cameraName', cameraName);
    form.append('title', title);
    form.append('video', file);

    document.getElementById('videoUploadMsg').textContent = '上传中...';
    const res = await fetch('/api/video-assets', {
      method: 'POST',
      headers: this.loadAuthHeaders(),
      body: form
    });
    const json = await res.json();
    if (!res.ok || json.code !== 0) {
      document.getElementById('videoUploadMsg').textContent = json.msg || '上传失败';
      return;
    }
    document.getElementById('videoUploadMsg').textContent = '上传成功，正在刷新视频列表...';
    await this.loadVideoAssets();
    document.getElementById('videoUploadMsg').textContent = '上传成功';
  },

  async hydrateIdentityList() {
    // For thesis demo, we keep it minimal; identity list can be extended.
  },

  setupWebSocket() {
    if (this.stompClient) return;
    const wsStatusEl = document.getElementById('wsStatus');
    const stompFactory = (window.StompJs && window.StompJs.Stomp) || window.Stomp;
    if (typeof SockJS === 'undefined' || !stompFactory) {
      if (wsStatusEl) wsStatusEl.textContent = 'WebSocket: 依赖加载失败（SockJS/Stomp）';
      return;
    }
    const sock = new SockJS('/ws');
    const client = stompFactory.over(sock);
    // Disable noisy logs in browser console.
    client.debug = () => {};
    client.connect({}, () => {
      if (wsStatusEl) wsStatusEl.textContent = 'WebSocket: 已连接';
      client.subscribe('/topic/alarms', (frame) => {
        const msg = JSON.parse(frame.body);
        this.onAlarm(msg);
      });
    }, (err) => {
      if (wsStatusEl) wsStatusEl.textContent = 'WebSocket: 连接失败';
      console.warn('WebSocket连接失败', err);
    });
    this.stompClient = client;
  },

  onAlarm(msg) {
    const list = document.getElementById('alarmList');
    const div = document.createElement('div');
    div.className = 'alarm';
    div.style.background = msg.alert ? 'rgba(255,0,0,0.08)' : 'rgba(0,255,0,0.06)';

    const name = msg.matchedName || 'Unknown';
    const type = msg.eventType || '';
    div.innerHTML = `
      <div style="display:flex; gap:10px; align-items:flex-start;">
        <div style="min-width:120px;">
          ${msg.snapshotUrl ? `<img src="${msg.snapshotUrl}" />` : ''}
        </div>
        <div>
          <div style="font-weight:700;">${type}：${name}</div>
          <div class="muted">score=${(msg.score || 0).toFixed(3)}, ts=${msg.timestampMs}</div>
          <div class="muted">jobId=${msg.jobId}</div>
        </div>
      </div>
    `;

    list.prepend(div);
    // keep max 30 items
    while (list.children.length > 30) list.removeChild(list.lastChild);
  }
};

// bootstrap
(function init() {
  const token = localStorage.getItem('token');
  if (token) {
    App.token = token;
    try {
      const roles = localStorage.getItem('roles');
      App.roleNames = roles ? JSON.parse(roles) : [];
    } catch (e) { }
    document.getElementById('loginPanel').style.display = 'none';
    document.getElementById('appPanel').style.display = 'block';
    App.initAfterLogin();
  }
})();

