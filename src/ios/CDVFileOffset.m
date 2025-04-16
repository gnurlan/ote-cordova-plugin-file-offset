#import "CDVFileOffset.h"

@implementation CDVFileOffset

- (void)writeAtOffset:(CDVInvokedUrlCommand*)command {
    [self.commandDelegate runInBackground:^{
        NSString* path = [command.arguments objectAtIndex:0];
        NSNumber* offset = [command.arguments objectAtIndex:1];
        NSString* data = [command.arguments objectAtIndex:2];
        NSNumber* isBase64 = [command.arguments objectAtIndex:3];

        CDVPluginResult* result = nil;
        NSError* error = nil;

        @try {
            // Convert file:// URL to native path if needed
            if ([path hasPrefix:@"file://"]) {
                path = [path substringFromIndex:7];
            }

            NSFileHandle* fileHandle = [NSFileHandle fileHandleForUpdatingAtPath:path];
            if (fileHandle == nil) {
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                          messageAsString:@"Could not open file for writing"];
                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
                return;
            }

            [fileHandle seekToFileOffset:[offset unsignedLongLongValue]];

            NSData* dataToWrite;
            if ([isBase64 boolValue]) {
                dataToWrite = [[NSData alloc] initWithBase64EncodedString:data options:0];
            } else {
                dataToWrite = [data dataUsingEncoding:NSUTF8StringEncoding];
            }

            [fileHandle writeData:dataToWrite];
            [fileHandle closeFile];

            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        }
        @catch (NSException* exception) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                      messageAsString:[exception reason]];
        }

        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

@end