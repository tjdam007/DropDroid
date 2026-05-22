let devices = [];
let selectedDevice = null;

const deviceList = document.querySelector('#deviceList');
const deviceCount = document.querySelector('#deviceCount');
const dropZone = document.querySelector('#dropZone');
const statusLine = document.querySelector('#statusLine');
const targetText = document.querySelector('#targetText');
const signal = document.querySelector('#signal');
const fileKind = document.querySelector('#fileKind');
const manualIp = document.querySelector('#manualIp');
const useManual = document.querySelector('#useManual');
const pickFile = document.querySelector('#pickFile');
const filePicker = document.querySelector('#filePicker');

if (!window.apkDrop) {
  window.apkDrop = {
    mode: 'browser',
    getDevices: async () => {
      const response = await fetch('/api/devices');
      return response.json();
    },
    uploadFile: async (file, target) => {
      const response = await fetch(
        `/api/send?ip=${encodeURIComponent(target.ip)}&port=${encodeURIComponent(target.port)}&filename=${encodeURIComponent(file.name)}`,
        {
          method: 'POST',
          headers: { 'x-file-size': String(file.size) },
          body: file,
        },
      );
      const result = await response.json();
      if (!response.ok) throw new Error(result.error || 'Could not send file');
      return result;
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

function renderDevices() {
  deviceCount.textContent = String(devices.length);
  signal.textContent = devices.length ? 'Device found' : 'Looking for device';

  if (!devices.length) {
    deviceList.innerHTML = '<p class="empty">Open DropDroid on Android and keep both devices on the same Wi-Fi.</p>';
  } else {
    deviceList.innerHTML = '';
    for (const device of devices) {
      const button = document.createElement('button');
      button.className = `device ${selectedDevice?.id === device.id ? 'selected' : ''}`;
      button.innerHTML = `<strong>${escapeHtml(device.name)}</strong><span>${escapeHtml(device.ip)}:${device.port}</span>`;
      button.addEventListener('click', () => {
        selectedDevice = device;
        targetText.textContent = `Sending to ${device.name} at ${device.ip}`;
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
  targetText.textContent = `Sending to ${ip}`;
  statusLine.textContent = 'Manual device selected';
  renderDevices();
}

async function sendFile(file) {
  if (!selectedDevice) {
    statusLine.textContent = 'Choose a device first';
    return;
  }

  const isApk = file.name.toLowerCase().endsWith('.apk');
  fileKind.textContent = isApk ? 'Android APK' : 'Any file';
  statusLine.textContent = `Sending ${file.name}...`;
  try {
    const payload = window.apkDrop.mode === 'electron' ? file.path : file;
    const result = await window.apkDrop.uploadFile(payload, selectedDevice);
    statusLine.textContent = isApk
      ? `Sent ${result.filename}. The phone can open the installer if its toggle is on.`
      : `Sent ${result.filename}`;
  } catch (error) {
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

window.apkDrop.onDevices((nextDevices) => {
  devices = nextDevices;
  if (selectedDevice) {
    selectedDevice = devices.find((device) => device.id === selectedDevice.id) || selectedDevice;
  } else if (devices.length === 1) {
    selectedDevice = devices[0];
    targetText.textContent = `Sending to ${selectedDevice.name} at ${selectedDevice.ip}`;
  }
  renderDevices();
});

window.apkDrop.getDevices().then((nextDevices) => {
  devices = nextDevices;
  renderDevices();
});

renderDevices();
