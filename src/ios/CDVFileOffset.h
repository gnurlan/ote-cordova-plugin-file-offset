#import <Cordova/CDVPlugin.h>

@interface CDVFileOffset : CDVPlugin

- (void)writeAtOffset:(CDVInvokedUrlCommand*)command;

@end