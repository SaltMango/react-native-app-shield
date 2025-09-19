// TurboModule codegen wrapper - forwards to Swift implementation
#import "AppShield.h"
#import "AppShield-Swift.h"
#import <React/RCTLog.h>

#if __has_include(<AppShieldSpec/AppShieldSpecJSI.h>)
#import <AppShieldSpec/AppShieldSpecJSI.h>
#endif

@implementation AppShield {
  AppShieldSwift *_swift;
}

RCT_EXPORT_MODULE(AppShield)

- (instancetype)init
{
  if (self = [super init]) {
    _swift = [AppShieldSwift new];
  }
  return self;
}

- (void)blockAllApps
{
  [_swift blockAllApps];
}

- (void)unblockAllApps
{
  [_swift unblockAllApps];
}

- (void)requestRequiredPermissions:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject
{
  [_swift requestRequiredPermissions:resolve rejecter:reject];
}

#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params
{
#if __has_include(<AppShieldSpec/AppShieldSpecJSI.h>)
  return std::make_shared<facebook::react::NativeAppShieldSpecJSI>(params);
#else
  return nullptr;
#endif
}
#endif

@end
