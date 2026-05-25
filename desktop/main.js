const { app, BrowserWindow, ipcMain } = require('electron');
const crypto = require('node:crypto');
const dgram = require('node:dgram');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const QRCode = require('qrcode');

const BEACON_PORT = 47882;
const DEFAULT_DEVICE_PORT = 47881;
const LOCAL_PROBE_INTERVAL_MS = 5000;
const portalId = base64Url(crypto.randomBytes(16));
const pairingSecret = base64Url(crypto.randomBytes(32));
const devices = new Map();
let mainWindow;
let udpSocket;

function base64Url(buffer) {
  return buffer.toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

function hmacSignature({ method, pathName, filename, size, timestamp, nonce, contentSha256 }) {
  return base64Url(
    crypto
      .createHmac('sha256', Buffer.from(pairingSecret, 'utf8'))
      .update([method, pathName, filename, String(size), String(timestamp), nonce, contentSha256].join('\n'))
      .digest(),
  );
}

function localAddressCandidates(payload, reachableIp) {
  return [reachableIp, payload.ip, ...(Array.isArray(payload.addresses) ? payload.addresses : [])]
    .filter(Boolean)
    .filter((address) => /^\d{1,3}(\.\d{1,3}){3}$/.test(address))
    .filter((address) => !address.startsWith('127.') && !address.startsWith('169.254.'))
    .filter((address, index, addresses) => addresses.indexOf(address) === index);
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 920,
    height: 680,
    minWidth: 720,
    minHeight: 540,
    title: 'DropDroid',
    backgroundColor: '#f7f3ec',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  mainWindow.loadFile(path.join(__dirname, 'renderer', 'index.html'));
}

function startDiscovery() {
  udpSocket = dgram.createSocket({ type: 'udp4', reuseAddr: true });

  udpSocket.on('message', (buffer, remote) => {
    try {
      const payload = JSON.parse(buffer.toString('utf8'));
      if (payload.app !== 'DropDroid') return;

      const reachableIp = remote.address;
      const advertisedIp = payload.ip || reachableIp;
      const candidates = localAddressCandidates(payload, reachableIp);
      const key = `${reachableIp}:${payload.port || DEFAULT_DEVICE_PORT}`;
      const device = {
        id: key,
        name: payload.name || 'Android device',
        ip: reachableIp,
        advertisedIp,
        candidates,
        port: Number(payload.port || DEFAULT_DEVICE_PORT),
        lastSeen: Date.now(),
      };
      devices.set(key, device);
      sendDevices();
    } catch {
      // Ignore packets from other local network tools.
    }
  });

  udpSocket.bind(BEACON_PORT, () => {
    udpSocket.setBroadcast(true);
    startLocalNetworkProbe(udpSocket);
  });

  setInterval(() => {
    const cutoff = Date.now() - 8000;
    for (const [key, device] of devices) {
      if (device.lastSeen < cutoff) devices.delete(key);
    }
    sendDevices();
  }, 2000);
}

function startLocalNetworkProbe(socket) {
  const probe = Buffer.from(JSON.stringify({ app: 'DropDroidPortal', version: 1 }));
  const sendProbe = () => {
    socket.send(probe, 0, probe.length, BEACON_PORT, '255.255.255.255', () => {});
  };
  sendProbe();
  setInterval(sendProbe, LOCAL_PROBE_INTERVAL_MS);
}

function sendDevices() {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  mainWindow.webContents.send('devices', Array.from(devices.values()));
}

function uploadFile(filePath, target) {
  return new Promise((resolve, reject) => {
    const stat = fs.statSync(filePath);
    const filename = path.basename(filePath);
    const contentSha256 = crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex');
    const timestamp = Date.now().toString();
    const nonce = base64Url(crypto.randomBytes(16));
    const requestPath = `/upload?filename=${encodeURIComponent(filename)}`;
    const signature = hmacSignature({
      method: 'PUT',
      pathName: requestPath,
      filename,
      size: stat.size,
      timestamp,
      nonce,
      contentSha256,
    });
    const request = http.request(
      {
        method: 'PUT',
        hostname: target.ip,
        port: target.port || DEFAULT_DEVICE_PORT,
        path: requestPath,
        headers: {
          'Content-Type': 'application/octet-stream',
          'Content-Length': stat.size,
          'X-DropDroid-Portal-Id': portalId,
          'X-DropDroid-Timestamp': timestamp,
          'X-DropDroid-Nonce': nonce,
          'X-DropDroid-Content-Sha256': contentSha256,
          'X-DropDroid-Signature': signature,
        },
        timeout: 120000,
      },
      (response) => {
        let body = '';
        response.setEncoding('utf8');
        response.on('data', (chunk) => {
          body += chunk;
        });
        response.on('end', () => {
          if (response.statusCode >= 200 && response.statusCode < 300) {
            resolve({ filename, bytes: stat.size, body });
          } else {
            reject(new Error(body || `Device returned ${response.statusCode}`));
          }
        });
      },
    );

    request.on('timeout', () => {
      request.destroy(new Error('Timed out while sending the file'));
    });
    request.on('error', (error) => {
      reject(new Error(formatConnectionError(error, target)));
    });
    fs.createReadStream(filePath).pipe(request);
  });
}

function formatConnectionError(error, target) {
  if (['EACCES', 'EPERM'].includes(error.code)) {
    return `macOS blocked local network access for DropDroid. Open System Settings > Privacy & Security > Local Network and allow DropDroid, then reopen the app.`;
  }
  if (['ETIMEDOUT', 'EHOSTUNREACH', 'ENETUNREACH', 'ECONNREFUSED'].includes(error.code)) {
    return `Phone not reachable at ${target.ip}:${target.port || DEFAULT_DEVICE_PORT}. Keep DropDroid open, confirm both devices share a local connection, allow DropDroid in macOS Local Network/Firewall settings, or try the phone's shown IP manually.`;
  }
  return error.message || 'Could not send file';
}

function pingAndroidAddress(target, ip) {
  return new Promise((resolve) => {
    const request = http.request(
      {
        method: 'GET',
        hostname: ip,
        port: target.port || DEFAULT_DEVICE_PORT,
        path: `/ping?portalId=${encodeURIComponent(portalId)}`,
        timeout: 3000,
      },
      (response) => {
        let body = '';
        response.setEncoding('utf8');
        response.on('data', (chunk) => {
          body += chunk;
        });
        response.on('end', () => {
          if (response.statusCode < 200 || response.statusCode >= 300) {
            resolve({ reachable: true, paired: false, message: 'Update DropDroid, then scan this QR', checkedAt: Date.now() });
            return;
          }
          try {
            const payload = JSON.parse(body || '{}');
            resolve({
              reachable: response.statusCode >= 200 && response.statusCode < 300,
              paired: payload.paired === true,
              message: payload.message || 'Phone responded',
              name: payload.name,
              ip,
              checkedAt: Date.now(),
            });
          } catch {
            resolve({ reachable: true, paired: false, message: 'Phone responded without pairing status', checkedAt: Date.now() });
          }
        });
      },
    );

    request.on('timeout', () => request.destroy(new Error('Ping timed out')));
    request.on('error', () => {
      resolve({ reachable: false, paired: false, message: 'Not reachable', checkedAt: Date.now() });
    });
    request.end();
  });
}

async function pingAndroid(target) {
  const candidates = [target.ip, ...(Array.isArray(target.candidates) ? target.candidates : [])]
    .filter(Boolean)
    .filter((address, index, addresses) => addresses.indexOf(address) === index);

  let lastStatus = { reachable: false, paired: false, message: 'Not reachable', checkedAt: Date.now() };
  for (const ip of candidates) {
    const status = await pingAndroidAddress(target, ip);
    lastStatus = status;
    if (status.reachable) return status;
  }
  return lastStatus;
}

ipcMain.handle('upload-file', async (_event, { filePath, target }) => {
  if (!filePath || !target?.ip) throw new Error('Choose a device before sending');
  return uploadFile(filePath, target);
});

ipcMain.handle('get-devices', () => Array.from(devices.values()));
ipcMain.handle('ping-device', async (_event, target) => pingAndroid(target));
ipcMain.handle('get-pairing', async () => {
  const payload = JSON.stringify({ app: 'DropDroid', version: 1, portalId, secret: pairingSecret, createdAt: Date.now() });
  const svg = await QRCode.toString(payload, { type: 'svg', errorCorrectionLevel: 'M', margin: 2 });
  return { portalId, payload, svg };
});

app.whenReady().then(() => {
  createWindow();
  startDiscovery();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('before-quit', () => {
  udpSocket?.close();
});
