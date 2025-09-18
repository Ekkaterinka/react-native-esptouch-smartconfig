import { Text, View, StyleSheet } from 'react-native';
import { useEffect, useState, useRef, useCallback } from 'react';
import {
  checkLocation,
  getConnectedInfo,
} from 'react-native-esptouch-smartconfig';

export default function App() {
  const [resultConnected, setResultConnected] = useState('Ждем результат ...');

  const addIntervalRef = useRef<number>(0);

  const autofillSSID = useCallback(async () => {
    await checkLocation()
      .then(async (success) => {
        console.log('checkLocation success', success);
        clearInterval(addIntervalRef.current);
        await getConnectedInfo()
          .then((result) => {
            const { ssid, ip, is5G, bssid, state } = result;
            if (is5G) {
              setResultConnected('Подключитесь к сети 2.4Ггц!');
              return;
            }
            setResultConnected(`Ваш ssid: ${ssid}, ip: ${ip}, 
              bssid: ${bssid}, state: ${state}`);
            clearInterval(addIntervalRef.current);
          })
          .catch((error) => {
            console.log('autofillSSID error', error);
            if (error.message === 'NotConnected') {
              console.log('WiFi не подключен');
              addIntervalRef.current = setInterval(
                autofillSSID,
                2e3
              ) as unknown as number;
              setResultConnected('WiFi не подключен');
            } else if (error.message === 'Connecting') {
              console.log('WiFi в процессе подключения');
              setResultConnected('WiFi в процессе подключения');
            }
          });
      })
      .catch((error) => {
        console.log('checkLocation error', error);
        if (error.message === 'NOT_DETERMINATED') {
          addIntervalRef.current = setInterval(
            autofillSSID,
            2e3
          ) as unknown as number;
        }
        setResultConnected('У приложения нет доступа к геопозиции');
      });
  }, []);

  useEffect(() => {
    autofillSSID();
    return () => clearInterval(addIntervalRef.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {resultConnected}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FFFACD',
  },
});
