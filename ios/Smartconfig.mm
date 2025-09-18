#import "Smartconfig.h"

@interface EspTouchDelegateImpl : NSObject<ESPTouchDelegate>
@property (nonatomic, weak) id <ESPTouchDelegate> commandDelegate;

@end
NSString *callback_ID;
@implementation EspTouchDelegateImpl

-(void) onEsptouchResultAddedWithResult: (ESPTouchResult *) result
{
    NSString *InetAddress=[ESP_NetUtil descriptionInetAddr4ByData:result.ipAddrData];
    NSDictionary *dic =@{@"bssid":result.bssid,@"ip":InetAddress};
    NSLog(@"收到onEsptouchResultAddedWithResult bssid:%@ ip:%@",result.bssid,InetAddress);
}


@end

@implementation Smartconfig

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(checkLocation: (RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    if (@available(iOS 13.0, *)) {
        CLLocationManager* cllocation = [[CLLocationManager alloc] init];

        switch ([CLLocationManager authorizationStatus]) {
            case kCLAuthorizationStatusDenied:
            case kCLAuthorizationStatusRestricted: {
             dispatch_async(dispatch_get_main_queue(), ^{
                UIViewController* rootController = [UIApplication sharedApplication].delegate.window.rootViewController;

                UIAlertController* alert = [UIAlertController alertControllerWithTitle:@"У приложения нет доступа к геопозиции."
                                                                               message:@"Разрешите доступ к Вашей геопозиции в настройках устройства."
                                                                        preferredStyle:UIAlertControllerStyleAlert];

                UIAlertAction* settingsAction = [UIAlertAction actionWithTitle:@"Перейти в настройки"
                                                                         style:UIAlertActionStyleDefault
                                                                       handler:^(UIAlertAction* action)
                                                 {
                    NSURL *settingsURL = [NSURL URLWithString:UIApplicationOpenSettingsURLString];
                    UIApplication *application = [UIApplication sharedApplication];
                    [application openURL:settingsURL options:@{} completionHandler:nil];
                }];

                [alert addAction:settingsAction];

                UIAlertAction* cancelAction = [UIAlertAction actionWithTitle:@"Позже"
                                                                       style:UIAlertActionStyleCancel
                                                                     handler:^(UIAlertAction* action)
                                               {
                    reject(@"checkLocation",@"NOT_GRANTED", nil);
                }];

                [alert addAction:cancelAction];

                  [rootController presentViewController:alert animated:YES completion:nil];
                [NSTimer scheduledTimerWithTimeInterval:5 repeats:NO block:^(NSTimer * _Nonnull timer) {
                    [rootController dismissViewControllerAnimated:YES completion:nil];
                }];
                });

                break;}
            case kCLAuthorizationStatusNotDetermined: {
                [cllocation requestWhenInUseAuthorization];
                reject(@"checkLocation",@"NOT_DETERMINATED", nil);
                break;}
            case kCLAuthorizationStatusAuthorizedAlways:
            case kCLAuthorizationStatusAuthorizedWhenInUse:{
                resolve(@"GRANTED");
                break;}
            default:
                break;
        }
    }
}
RCT_EXPORT_METHOD(getConnectedInfo: (RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
 {
    NetworkStatus networkStatus = [[ESPReachability reachabilityForInternetConnection] currentReachabilityStatus];
    if (networkStatus == ReachableViaWiFi) {

            NSDictionary *wifiDic = [NSDictionary dictionaryWithObjectsAndKeys:
                                 ESPTools.getCurrentWiFiSsid, @"ssid",
                                 ESPTools.getCurrentBSSID,@"bssid",
                                 @"Connected", @"message",
                                 nil];

        resolve(wifiDic);
    } else {
        reject(@"getConnectedInfo",@"NotConnected", nil);
    }
}

RCT_EXPORT_METHOD(startEspTouch: (NSString *)apSsid
apBssid:(NSString *)apBssid
apPassword:(NSString *)apPassword
resolve:(RCTPromiseResolveBlock)resolve
reject:(RCTPromiseRejectBlock)reject)
{dispatch_queue_t  queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);

 dispatch_async(queue, ^{
  NSLog(@"startEspTouchstartEspTouch");


        [self._condition lock];
        int taskCount = 1;
        NSString *broadcastData = @"1";
        BOOL isbroadcastData = true;
        if([broadcastData compare:@"1"]==NSOrderedSame){
            isbroadcastData=false;
        }

        NSLog(@"ssid: %@, bssid: %@, apPwd: %@", apSsid, apBssid, apPassword);

        self._esptouchTask =
        [[ESPTouchTask alloc]initWithApSsid:apSsid andApBssid:apBssid andApPwd:apPassword];
        EspTouchDelegateImpl *esptouchDelegate=[[EspTouchDelegateImpl alloc]init];
        [self._esptouchTask setEsptouchDelegate:esptouchDelegate];

        [self._esptouchTask setPackageBroadcast:YES]; // if YES send broadcast packets, else send multicast packets
        [self._condition unlock];
        NSArray * esptouchResultArray = [self._esptouchTask executeForResults:taskCount];

        dispatch_async(queue, ^{
            // show the result to the user in UI Main Thread
            dispatch_async(dispatch_get_main_queue(), ^{


                ESPTouchResult *firstResult = [esptouchResultArray objectAtIndex:0];
                // check whether the task is cancelled and no results received
                if (!firstResult.isCancelled)
                {
                    if ([firstResult isSuc])
                    {
                        ESPTouchResult *resultInArray = [esptouchResultArray objectAtIndex:0];
                        NSString *ipaddr = [ESP_NetUtil descriptionInetAddr4ByData:resultInArray.ipAddrData];
                        resolve(resultInArray.bssid);
                    }
                    else
                    {
                    reject(@"startEspTouch",@"Esptouch fail", nil);
                    }
                }

            });
        });
    });
}

RCT_EXPORT_METHOD(stopEspTouch: (RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
     [self._condition lock];
     if (self._esptouchTask != nil) {
          [self._esptouchTask interrupt];
     }
     [self._condition unlock];
     resolve(@"cansel success");
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeSmartconfigSpecJSI>(params);
}

@end

