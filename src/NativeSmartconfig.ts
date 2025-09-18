import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  checkLocation(): Promise<string>;

  getConnectedInfo(): Promise<{
    ssid: string;
    bssid: string;
    state: string;
    ip?: string;
    is5G?: boolean;
  }>;

  startEspTouch(
    apSsid: string,
    apBssid: string,
    apPassword: string
  ): Promise<string>;

  stopEspTouch(): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('Smartconfig');
