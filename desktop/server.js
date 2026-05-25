const dgram = require('node:dgram');
const crypto = require('node:crypto');
const fs = require('node:fs');
const http = require('node:http');
const os = require('node:os');
const path = require('node:path');
const QRCode = require('qrcode');
const { URL } = require('node:url');

const APP_PORT = Number(process.env.PORT || 38531);
const BEACON_PORT = 47882;
const DEFAULT_DEVICE_PORT = 47881;
const LOCAL_PROBE_INTERVAL_MS = 5000;
const portalId = base64Url(crypto.randomBytes(16));
const pairingSecret = base64Url(crypto.randomBytes(32));
const devices = new Map();
const publicDir = path.join(__dirname, 'renderer');
let discoveryProbeInterval;

function base64Url(buffer) {
  return buffer
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '');
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

function localBroadcastAddresses() {
  const broadcasts = new Set(['255.255.255.255']);
  for (const entries of Object.values(os.networkInterfaces())) {
    for (const entry of entries || []) {
      if (entry.family !== 'IPv4' || entry.internal || !entry.address || !entry.netmask) continue;
      const address = ipv4ToInt(entry.address);
      const netmask = ipv4ToInt(entry.netmask);
      if (address == null || netmask == null) continue;
      broadcasts.add(intToIpv4((address & netmask) | (~netmask >>> 0)));
    }
  }
  return Array.from(broadcasts);
}

function ipv4ToInt(address) {
  const parts = address.split('.').map((part) => Number(part));
  if (parts.length !== 4 || parts.some((part) => !Number.isInteger(part) || part < 0 || part > 255)) return null;
  return parts.reduce((value, part) => ((value << 8) + part) >>> 0, 0);
}

function intToIpv4(value) {
  return [24, 16, 8, 0].map((shift) => (value >>> shift) & 255).join('.');
}

function startDiscovery() {
  const socket = dgram.createSocket({ type: 'udp4', reuseAddr: true });

  socket.on('error', (error) => {
    console.warn(`DropDroid discovery error: ${error.message}`);
  });

  socket.on('message', (buffer, remote) => {
    try {
      const payload = JSON.parse(buffer.toString('utf8'));
      if (payload.app !== 'DropDroid') return;

      const reachableIp = remote.address;
      const advertisedIp = payload.ip || reachableIp;
      const candidates = localAddressCandidates(payload, reachableIp);
      const device = {
        id: `${reachableIp}:${payload.port || DEFAULT_DEVICE_PORT}`,
        name: payload.name || 'Android device',
        ip: reachableIp,
        advertisedIp,
        candidates,
        port: Number(payload.port || DEFAULT_DEVICE_PORT),
        lastSeen: Date.now(),
      };
      devices.set(device.id, device);
    } catch {
      // Ignore packets from other local network tools.
    }
  });

  socket.bind(BEACON_PORT, () => {
    try {
      socket.setBroadcast(true);
    } catch (error) {
      console.warn(`Could not enable DropDroid broadcast discovery: ${error.message}`);
    }
    startLocalNetworkProbe(socket);
  });

  setInterval(() => {
    const cutoff = Date.now() - 8000;
    for (const [id, device] of devices) {
      if (device.lastSeen < cutoff) devices.delete(id);
    }
  }, 2000);
}

function startLocalNetworkProbe(socket) {
  const probe = Buffer.from(JSON.stringify({ app: 'DropDroidPortal', version: 1 }));
  const sendProbe = () => {
    for (const address of localBroadcastAddresses()) {
      socket.send(probe, 0, probe.length, BEACON_PORT, address, (error) => {
        if (error) console.warn(`DropDroid local network probe failed for ${address}: ${error.message}`);
      });
    }
  };
  sendProbe();
  discoveryProbeInterval = setInterval(sendProbe, LOCAL_PROBE_INTERVAL_MS);
}

process.on('SIGINT', () => {
  if (discoveryProbeInterval) clearInterval(discoveryProbeInterval);
  process.exit(0);
});

function uploadToAndroid(request, target, filename, size, contentSha256) {
  return new Promise((resolve, reject) => {
    const timestamp = Date.now().toString();
    const nonce = base64Url(crypto.randomBytes(16));
    const pathName = `/upload?filename=${encodeURIComponent(filename)}`;
    const signature = hmacSignature({
      method: 'PUT',
      pathName,
      filename,
      size,
      timestamp,
      nonce,
      contentSha256,
    });
    const outbound = http.request(
      {
        method: 'PUT',
        hostname: target.ip,
        port: target.port || DEFAULT_DEVICE_PORT,
        path: pathName,
        headers: {
          'Content-Type': 'application/octet-stream',
          'Content-Length': size,
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
          if (response.statusCode >= 200 && response.statusCode < 300) resolve(body);
          else reject(new Error(body || `Device returned ${response.statusCode}`));
        });
      },
    );

    outbound.on('timeout', () => outbound.destroy(new Error('Timed out while sending the file')));
    outbound.on('error', (error) => {
      reject(new Error(formatConnectionError(error, target)));
    });
    request.pipe(outbound);
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

function serveStatic(response, pathname) {
  const normalized = pathname === '/' ? '/index.html' : pathname;
  const filePath = path.normalize(path.join(publicDir, normalized));
  if (!filePath.startsWith(publicDir)) {
    response.writeHead(403);
    response.end('Forbidden');
    return;
  }

  fs.readFile(filePath, (error, data) => {
    if (error) {
      response.writeHead(404);
      response.end('Not found');
      return;
    }

    const ext = path.extname(filePath);
    const contentType =
      {
        '.html': 'text/html; charset=utf-8',
        '.css': 'text/css; charset=utf-8',
        '.js': 'text/javascript; charset=utf-8',
      }[ext] || 'application/octet-stream';
    response.writeHead(200, { 'Content-Type': contentType });
    response.end(data);
  });
}

function startUiServer() {
  const server = http.createServer(async (request, response) => {
    const url = new URL(request.url, `http://localhost:${APP_PORT}`);

    if (url.pathname === '/api/pairing') {
      const payload = JSON.stringify({
        app: 'DropDroid',
        version: 1,
        portalId,
        secret: pairingSecret,
        createdAt: Date.now(),
      });
      const svg = await QRCode.toString(payload, {
        type: 'svg',
        errorCorrectionLevel: 'M',
        margin: 2,
        color: {
          dark: '#142120',
          light: '#ffffff',
        },
      });
      response.writeHead(200, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify({ portalId, payload, svg }));
      return;
    }

    if (url.pathname === '/api/devices') {
      response.writeHead(200, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify(Array.from(devices.values())));
      return;
    }

    if (url.pathname === '/api/ping') {
      const target = {
        ip: url.searchParams.get('ip'),
        port: Number(url.searchParams.get('port') || DEFAULT_DEVICE_PORT),
      };
      if (!target.ip) {
        response.writeHead(400, { 'Content-Type': 'application/json' });
        response.end(JSON.stringify({ reachable: false, paired: false, message: 'Missing phone IP' }));
        return;
      }
      const result = await pingAndroid(target);
      response.writeHead(200, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify(result));
      return;
    }

    if (url.pathname === '/api/send' && request.method === 'POST') {
      try {
        const target = {
          ip: url.searchParams.get('ip'),
          port: Number(url.searchParams.get('port') || DEFAULT_DEVICE_PORT),
        };
        const filename = url.searchParams.get('filename') || 'shared-file';
        const size = Number(request.headers['x-file-size']);
        const contentSha256 = String(request.headers['x-file-sha256'] || '');
        if (!target.ip || !size || !contentSha256) throw new Error('Missing target, file size, or file hash');

        await uploadToAndroid(request, target, filename, size, contentSha256);
        response.writeHead(200, { 'Content-Type': 'application/json' });
        response.end(JSON.stringify({ filename, bytes: size }));
      } catch (error) {
        response.writeHead(500, { 'Content-Type': 'application/json' });
        response.end(JSON.stringify({ error: error.message || 'Could not send file' }));
      }
      return;
    }

    serveStatic(response, url.pathname);
  });

  server.listen(APP_PORT, () => {
    console.log(`DropDroid desktop tool: http://localhost:${APP_PORT}`);
  });
}

startDiscovery();
startUiServer();
