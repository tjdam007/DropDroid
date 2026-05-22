const { app, BrowserWindow, ipcMain } = require('electron');
const crypto = require('node:crypto');
const dgram = require('node:dgram');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const QRCode = require('qrcode');

const BEACON_PORT = 47882;
const DEFAULT_DEVICE_PORT = 47881;
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
      const key = `${reachableIp}:${payload.port || DEFAULT_DEVICE_PORT}`;
      const device = {
        id: key,
        name: payload.name || 'Android device',
        ip: reachableIp,
        advertisedIp,
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
  });

  setInterval(() => {
    const cutoff = Date.now() - 8000;
    for (const [key, device] of devices) {
      if (device.lastSeen < cutoff) devices.delete(key);
    }
    sendDevices();
  }, 2000);
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
  if (['ETIMEDOUT', 'EHOSTUNREACH', 'ENETUNREACH', 'ECONNREFUSED'].includes(error.code)) {
    return `Phone not reachable at ${target.ip}:${target.port || DEFAULT_DEVICE_PORT}. Keep DropDroid open, confirm both devices share a local connection, or try the phone's shown IP manually.`;
  }
  return error.message || 'Could not send file';
}

ipcMain.handle('upload-file', async (_event, { filePath, target }) => {
  if (!filePath || !target?.ip) throw new Error('Choose a device before sending');
  return uploadFile(filePath, target);
});

ipcMain.handle('get-devices', () => Array.from(devices.values()));
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
