const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('apkDrop', {
  mode: 'electron',
  getDevices: () => ipcRenderer.invoke('get-devices'),
  getPairing: () => ipcRenderer.invoke('get-pairing'),
  uploadFile: (filePath, target) => ipcRenderer.invoke('upload-file', { filePath, target }),
  onDevices: (callback) => {
    const listener = (_event, devices) => callback(devices);
    ipcRenderer.on('devices', listener);
    return () => ipcRenderer.removeListener('devices', listener);
  },
});
