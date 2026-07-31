const { contextBridge, ipcRenderer, webUtils } = require('electron');

contextBridge.exposeInMainWorld('apkDrop', {
  mode: 'electron',
  getFilePath: (file) => webUtils.getPathForFile(file),
  getDevices: () => ipcRenderer.invoke('get-devices'),
  getPairing: () => ipcRenderer.invoke('get-pairing'),
  unpair: () => ipcRenderer.invoke('unpair'),
  pingDevice: (target) => ipcRenderer.invoke('ping-device', target),
  uploadFile: (filePath, target) => ipcRenderer.invoke('upload-file', { filePath, target }),
  onDevices: (callback) => {
    const listener = (_event, devices) => callback(devices);
    ipcRenderer.on('devices', listener);
    return () => ipcRenderer.removeListener('devices', listener);
  },
});
