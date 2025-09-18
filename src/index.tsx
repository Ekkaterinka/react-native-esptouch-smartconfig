import Smartconfig from './NativeSmartconfig';

export function checkLocation(): Promise<string> {
  return Smartconfig.checkLocation();
}

export function getConnectedInfo(): Promise<{
  ssid: string;
  bssid: string;
  state: string;
  ip?: string;
  is5G?: boolean;
}> {
  return Smartconfig.getConnectedInfo();
}

export function startEspTouch(
  apSsid: string,
  apBssid: string,
  apPassword: string
): Promise<string> {
  return Smartconfig.startEspTouch(apSsid, apBssid, apPassword);
}

export function stopEspTouch(): Promise<string> {
  return Smartconfig.stopEspTouch();
}
