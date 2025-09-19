#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(AppShield, NSObject)

RCT_EXTERN_METHOD(blockAllApps)
RCT_EXTERN_METHOD(unblockAllApps)
RCT_EXTERN_METHOD(requestRequiredPermissions:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

@end


