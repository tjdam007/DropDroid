let devices = [];
let selectedDevice = null;
let transfers = [];
let deviceStatuses = new Map();

const deviceList = document.querySelector('#deviceList');
const deviceCount = document.querySelector('#deviceCount');
const dropZone = document.querySelector('#dropZone');
const statusLine = document.querySelector('#statusLine');
const transferCount = document.querySelector('#transferCount');
const transferList = document.querySelector('#transferList');
const targetText = document.querySelector('#targetText');
const signal = document.querySelector('#signal');
const fileKind = document.querySelector('#fileKind');
const qrBox = document.querySelector('#qrBox');
const pairId = document.querySelector('#pairId');
const manualIp = document.querySelector('#manualIp');
const useManual = document.querySelector('#useManual');
const pickFile = document.querySelector('#pickFile');
const filePicker = document.querySelector('#filePicker');

if (!window.apkDrop) {
  window.apkDrop = {
    mode: 'browser',
    getPairing: async () => {
      const response = await fetch('/api/pairing');
      return response.json();
    },
    getDevices: async () => {
      const response = await fetch('/api/devices');
      return response.json();
    },
    pingDevice: async (target) => {
      const response = await fetch(`/api/ping?ip=${encodeURIComponent(target.ip)}&port=${encodeURIComponent(target.port || 47881)}`);
      return response.json();
    },
    uploadFile: async (file, target, onProgress) => {
      const hash = await fileSha256(file);
      return uploadWithProgress(file, target, hash, onProgress);
    },
    onDevices: (callback) => {
      const poll = async () => {
        callback(await window.apkDrop.getDevices());
      };
      const interval = setInterval(poll, 1500);
      poll();
      return () => clearInterval(interval);
    },
  };
}

async function fileSha256(file) {
  const buffer = await file.arrayBuffer();
  const digest = await crypto.subtle.digest('SHA-256', buffer);
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('');
}

function uploadWithProgress(file, target, hash, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const url = `/api/send?ip=${encodeURIComponent(target.ip)}&port=${encodeURIComponent(target.port)}&filename=${encodeURIComponent(file.name)}`;
    xhr.open('POST', url);
    xhr.setRequestHeader('x-file-size', String(file.size));
    xhr.setRequestHeader('x-file-sha256', hash);
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress?.(event.loaded / event.total);
    };
    xhr.onload = () => {
      let result = {};
      try {
        result = JSON.parse(xhr.responseText || '{}');
      } catch {
        result = {};
      }
      if (xhr.status >= 200 && xhr.status < 300) resolve(result);
      else reject(new Error(result.error || 'Could not send file'));
    };
    xhr.onerror = () => reject(new Error('Network error while sending file'));
    xhr.send(file);
  });
}

function addTransfer(file) {
  const transfer = {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    file,
    name: file.name,
    size: file.size,
    progress: 0,
    status: 'Queued',
    detail: 'Waiting to start',
    isApk: file.name.toLowerCase().endsWith('.apk'),
  };
  transfers = [transfer, ...transfers].slice(0, 30);
  renderTransfers();
  return transfer.id;
}

function updateTransfer(id, patch) {
  transfers = transfers.map((transfer) => (transfer.id === id ? { ...transfer, ...patch } : transfer));
  renderTransfers();
}

function renderTransfers() {
  transferCount.textContent = String(transfers.length);
  if (!transfers.length) {
    transferList.innerHTML = '<p class="empty">Sending files will appear here.</p>';
    return;
  }

  transferList.innerHTML = '';
  for (const transfer of transfers) {
    const row = document.createElement('div');
    const stateClass = transfer.status === 'Done' ? 'done' : transfer.status === 'Error' ? 'error' : '';
    row.className = 'transfer-row';
    row.innerHTML = `
      <div class="transfer-topline">
        <div class="transfer-name" title="${escapeHtml(transfer.name)}">${escapeHtml(transfer.name)}</div>
        <div class="transfer-state ${stateClass}">${escapeHtml(transfer.status)}</div>
      </div>
      <div class="transfer-meta">${escapeHtml(transfer.detail)} · ${readableSize(transfer.size)}</div>
      <div class="progress-track"><div class="progress-fill" style="width: ${Math.round(transfer.progress * 100)}%"></div></div>
      ${
        transfer.status === 'Error'
          ? `<button class="retry-button" type="button" data-transfer-id="${escapeHtml(transfer.id)}">${lucideIcon('refresh')}<span>Retry</span></button>`
          : ''
      }
    `;
    transferList.appendChild(row);
  }
}

function lucideIcon(name) {
  const icons = {
    refresh: `
      <svg class="lucide" aria-hidden="true" viewBox="0 0 24 24">
        <path d="M3 12a9 9 0 0 1 15-6.7L21 8" />
        <path d="M21 3v5h-5" />
        <path d="M21 12a9 9 0 0 1-15 6.7L3 16" />
        <path d="M3 21v-5h5" />
      </svg>
    `,
  };
  return icons[name] || '';
}

function readableSize(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  const units = ['KB', 'MB', 'GB'];
  let value = bytes;
  let unit = 'B';
  for (const next of units) {
    value /= 1024;
    unit = next;
    if (value < 1024) break;
  }
  return `${value.toFixed(1)} ${unit}`;
}

function renderDevices() {
  deviceCount.textContent = String(devices.length);
  const connectedCount = devices.filter((device) => deviceStatuses.get(device.id)?.paired).length;
  signal.textContent = connectedCount ? 'Device connected' : devices.length ? 'Pairing needed' : 'Looking for device';

  if (!devices.length) {
    deviceList.innerHTML = '<p class="empty">Open DropDroid on Android and keep both devices on the same local connection. If discovery fails, enter one of the IPs shown on the phone.</p>';
  } else {
    deviceList.innerHTML = '';
    for (const device of devices) {
      const button = document.createElement('button');
      const status = deviceStatuses.get(device.id);
      const statusClass = status?.paired ? 'connected' : status?.reachable ? 'unpaired' : status ? 'offline' : 'checking';
      const statusLabel = status?.paired ? 'Connected' : status?.reachable ? 'Not connected' : status ? 'Offline' : 'Checking';
      button.className = `device ${selectedDevice?.id === device.id ? 'selected' : ''} ${statusClass}`;
      const addressText =
        device.advertisedIp && device.advertisedIp !== device.ip
          ? `${device.ip}:${device.port} · phone shows ${device.advertisedIp}`
          : `${device.ip}:${device.port}`;
      button.innerHTML = `
        <span class="device-topline">
          <strong>${escapeHtml(device.name)}</strong>
          <span class="device-status">${escapeHtml(statusLabel)}</span>
        </span>
        <span>${escapeHtml(addressText)}</span>
        <span class="device-message">${escapeHtml(status?.message || 'Checking secure pairing...')}</span>
      `;
      button.addEventListener('click', () => {
        selectedDevice = device;
        targetText.textContent = status?.paired
          ? `Sending to ${device.name} at ${device.ip}`
          : `${device.name} is visible, but not connected to this QR session. Scan this QR in DropDroid.`;
        renderDevices();
      });
      deviceList.appendChild(button);
    }
  }
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (char) => {
    const entities = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
    return entities[char];
  });
}

function useManualDevice() {
  const ip = manualIp.value.trim();
  if (!ip) return;
  selectedDevice = {
    id: `${ip}:47881`,
    name: 'Manual device',
    ip,
    port: 47881,
  };
  if (!devices.some((device) => device.id === selectedDevice.id)) {
    devices = [selectedDevice, ...devices];
  }
  targetText.textContent = `Checking ${ip}...`;
  statusLine.textContent = 'Checking manual device';
  renderDevices();
  pingDevice(selectedDevice);
}

async function sendFile(file) {
  if (!selectedDevice) {
    statusLine.textContent = 'Scan the QR on Android, then choose a connected device';
    return;
  }

  const selectedStatus = deviceStatuses.get(selectedDevice.id);
  if (!selectedStatus?.paired) {
    await pingDevice(selectedDevice);
    const freshStatus = deviceStatuses.get(selectedDevice.id);
    if (!freshStatus?.paired) {
      statusLine.textContent = freshStatus?.reachable
        ? 'Phone found, but not connected to this QR session. Scan the QR again.'
        : 'Phone is not reachable. Keep DropDroid open and check the local connection.';
      targetText.textContent = 'Scan this QR in DropDroid before sending files.';
      renderDevices();
      return;
    }
    targetText.textContent = `Sending to ${selectedDevice.name} at ${selectedDevice.ip}`;
    renderDevices();
  }

  const isApk = file.name.toLowerCase().endsWith('.apk');
  const transferId = addTransfer(file);
  fileKind.textContent = isApk ? 'Android APK' : 'Any file';
  statusLine.textContent = `Preparing ${file.name}...`;
  try {
    const payload = window.apkDrop.mode === 'electron' ? file.path : file;
    updateTransfer(transferId, { status: 'Hashing', detail: 'Securing file', progress: 0.03 });
    const result = await window.apkDrop.uploadFile(payload, selectedDevice, (progress) => {
      updateTransfer(transferId, {
        status: 'Sending',
        detail: `${Math.round(progress * 100)}% sent`,
        progress: Math.max(0.05, progress),
      });
      statusLine.textContent = `Sending ${file.name}...`;
    });
    updateTransfer(transferId, { status: 'Done', detail: 'Sent to phone', progress: 1 });
    statusLine.textContent = isApk
      ? `Sent ${result.filename}. The phone can open the installer if its toggle is on.`
      : `Sent ${result.filename}`;
  } catch (error) {
    updateTransfer(transferId, { status: 'Error', detail: error.message || 'Could not send file' });
    statusLine.textContent = error.message || 'Could not send file';
  }
}

async function retryTransfer(id) {
  const transfer = transfers.find((item) => item.id === id);
  if (!transfer?.file) return;
  if (!selectedDevice) {
    updateTransfer(id, { status: 'Error', detail: 'Choose a connected device before retrying' });
    return;
  }
  const selectedStatus = deviceStatuses.get(selectedDevice.id);
  if (!selectedStatus?.paired) {
    await pingDevice(selectedDevice);
    if (!deviceStatuses.get(selectedDevice.id)?.paired) {
      updateTransfer(id, { status: 'Error', detail: 'Scan the QR again before retrying' });
      statusLine.textContent = 'Phone found, but not connected to this QR session.';
      return;
    }
  }

  updateTransfer(id, { status: 'Hashing', detail: 'Retrying securely', progress: 0.03 });
  statusLine.textContent = `Retrying ${transfer.name}...`;
  try {
    const payload = window.apkDrop.mode === 'electron' ? transfer.file.path : transfer.file;
    const result = await window.apkDrop.uploadFile(payload, selectedDevice, (progress) => {
      updateTransfer(id, {
        status: 'Sending',
        detail: `${Math.round(progress * 100)}% sent`,
        progress: Math.max(0.05, progress),
      });
    });
    updateTransfer(id, { status: 'Done', detail: 'Sent to phone', progress: 1 });
    statusLine.textContent = `Sent ${result.filename}`;
  } catch (error) {
    updateTransfer(id, { status: 'Error', detail: error.message || 'Could not send file' });
    statusLine.textContent = error.message || 'Could not send file';
  }
}

useManual.addEventListener('click', useManualDevice);
manualIp.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') useManualDevice();
});

pickFile.addEventListener('click', () => filePicker.click());
filePicker.addEventListener('change', () => {
  const [file] = filePicker.files;
  if (file) sendFile(file);
  filePicker.value = '';
});

dropZone.addEventListener('dragover', (event) => {
  event.preventDefault();
  dropZone.classList.add('dragging');
});

dropZone.addEventListener('dragleave', () => {
  dropZone.classList.remove('dragging');
});

dropZone.addEventListener('drop', (event) => {
  event.preventDefault();
  dropZone.classList.remove('dragging');
  const [file] = event.dataTransfer.files;
  if (file) sendFile(file);
});

transferList.addEventListener('click', (event) => {
  const button = event.target.closest('.retry-button');
  if (!button) return;
  retryTransfer(button.dataset.transferId);
});

window.apkDrop.onDevices((nextDevices) => {
  devices = nextDevices;
  if (selectedDevice) {
    selectedDevice = devices.find((device) => device.id === selectedDevice.id) || selectedDevice;
  } else {
    const connectedDevice = devices.find((device) => deviceStatuses.get(device.id)?.paired);
    if (connectedDevice) {
      selectedDevice = connectedDevice;
      targetText.textContent = `Sending to ${selectedDevice.name} at ${selectedDevice.ip}`;
    }
  }
  renderDevices();
  pingVisibleDevices();
});

window.apkDrop.getDevices().then((nextDevices) => {
  devices = nextDevices;
  renderDevices();
});

window.apkDrop.getPairing().then((pairing) => {
  qrBox.innerHTML = pairing.svg;
  pairId.textContent = `Session ${pairing.portalId}`;
  pingVisibleDevices();
});

renderDevices();
renderTransfers();

async function pingDevice(device) {
  if (!device?.ip) return;
  const pendingStatus = deviceStatuses.get(device.id);
  if (!pendingStatus) {
    deviceStatuses.set(device.id, { reachable: false, paired: false, message: 'Checking secure pairing...' });
    renderDevices();
  }
  try {
    const status = await window.apkDrop.pingDevice(device);
    const nextStatus = {
      reachable: status.reachable === true,
      paired: status.paired === true,
      message: status.message || (status.paired ? 'Connected to this portal' : 'Scan this portal QR'),
      checkedAt: status.checkedAt || Date.now(),
    };
    deviceStatuses.set(device.id, nextStatus);
    if (status.name && device.name !== status.name) {
      devices = devices.map((item) => (item.id === device.id ? { ...item, name: status.name } : item));
    }
    if (selectedDevice?.id === device.id) {
      selectedDevice = { ...selectedDevice, name: status.name || selectedDevice.name };
      targetText.textContent = nextStatus.paired
        ? `Sending to ${selectedDevice.name} at ${selectedDevice.ip}`
        : `${selectedDevice.name} is visible, but not connected to this QR session. Scan this QR in DropDroid.`;
    }
  } catch {
    deviceStatuses.set(device.id, { reachable: false, paired: false, message: 'Not reachable', checkedAt: Date.now() });
  }
  renderDevices();
}

function pingVisibleDevices() {
  for (const device of devices) {
    const status = deviceStatuses.get(device.id);
    if (!status || Date.now() - status.checkedAt > 5000) {
      pingDevice(device);
    }
  }
}

setInterval(pingVisibleDevices, 5000);
