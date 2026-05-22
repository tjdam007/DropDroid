const { app, BrowserWindow, ipcMain } = require('electron');
const dgram = require('node:dgram');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');

const BEACON_PORT = 47882;
const DEFAULT_DEVICE_PORT = 47881;
const devices = new Map();
let mainWindow;
let udpSocket;

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

      const key = `${payload.ip || remote.address}:${payload.port || DEFAULT_DEVICE_PORT}`;
      const device = {
        id: key,
        name: payload.name || 'Android device',
        ip: payload.ip || remote.address,
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
    const request = http.request(
      {
        method: 'PUT',
        hostname: target.ip,
        port: target.port || DEFAULT_DEVICE_PORT,
        path: `/upload?filename=${encodeURIComponent(filename)}`,
        headers: {
          'Content-Type': 'application/octet-stream',
          'Content-Length': stat.size,
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
    request.on('error', reject);
    fs.createReadStream(filePath).pipe(request);
  });
}

ipcMain.handle('upload-file', async (_event, { filePath, target }) => {
  if (!filePath || !target?.ip) throw new Error('Choose a device before sending');
  return uploadFile(filePath, target);
});

ipcMain.handle('get-devices', () => Array.from(devices.values()));

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
