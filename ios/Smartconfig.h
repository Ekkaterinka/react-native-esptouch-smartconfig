#import <SmartconfigSpec/SmartconfigSpec.h>
#import <CoreLocation/CoreLocation.h>
#import "ESPTouchResult.h"
#import "ESP_NetUtil.h"
#import "ESPTouchDelegate.h"
#import "ESPTools.h"
#import "ESPTouchTask.h"
#import "reachability.h"

@interface Smartconfig : NSObject <NativeSmartconfigSpec>
@property(nonatomic, strong) NSCondition *_condition;
@property(atomic, strong) ESPTouchTask *_esptouchTask;

@end
