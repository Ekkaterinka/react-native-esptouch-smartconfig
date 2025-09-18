# react-native-esptouch-smartconfig

RTNSmartconfig is a React Native module that provides a native bridge for ESP-Touch SmartConfig, allowing developers to easily configure IoT devices to connect to a Wi-Fi network directly from a React Native application.

✅ Supports iOS and Android

✅ Built with React Native Codegen (compatible with RN 0.75+)

This package is useful for applications that need to quickly and securely provision smart devices onto a Wi-Fi network using Espressif's SmartConfig protocol.

## Installation


```sh
npm install react-native-esptouch-smartconfig
```


## Usage


```js
import { checkLocation, getConnectedInfo, startEspTouch, stopEspTouch } from 'react-native-esptouch-smartconfig';

// ...

  const autofillSSID = useCallback(async () => {
    await checkLocation()
      .then(async success => {
        console.log('checkLocation success', success);
        clearInterval(addIntervalRef.current);
        await getConnectedInfo()
          .then(result => {
            const { ssid, ip, is5G, bssid, state } = result;
            if (is5G) {
              console.log('Подключитесь к сети 2.4Ггц!');
              return;
            }
            console.log('ssid', ssid);
            console.log('ip', ip);
            console.log('bssid', bssid);
            console.log('state', state);
            clearInterval(addIntervalRef.current);
          })
          .catch(error => {
            console.log('autofillSSID error', error);
            if (error.message == 'NotConnected') {
              console.log('WiFi не подключен');
              addIntervalRef.current = setInterval(
                autofillSSID,
                2e3,
              ) as unknown as number;
            } else if (error.message == 'Connecting') {
              console.log('WiFi в процессе подключения');
            }
          });
      })
      .catch(error => {
        console.log('checkLocation error', error);
        if (error.message == 'NOT_DETERMINATED') {
          addIntervalRef.current = setInterval(
            autofillSSID,
            2e3,
          ) as unknown as number;
        }
        console.log('У приложения нет доступа к геопозиции');
      });
  }, []);

    const startESP = useCallback(async () => {

    await startEspTouch(name, '00:00:00:00:00:00', password)
      .then(async resolve => {
        console.log('startEspTouch success');
      })
      .catch(error => {
        console.log('startEspTouch error', error);
        console.log('Устройство не найдено или уже подключено');
        stopEspTouch();
      });
  }, [setMessage, name, password]);

```


## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
