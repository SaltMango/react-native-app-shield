#import <AppShieldSpec/AppShieldSpec.h>
#import <React/RCTBridgeModule.h>

// ObjC wrapper that conforms to the generated NativeAppShieldSpec and forwards to Swift
@interface AppShield : NSObject <NativeAppShieldSpec>
@end
