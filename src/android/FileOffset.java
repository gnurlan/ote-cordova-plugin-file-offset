package org.ote.cordova;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;
import android.util.Base64;
import android.util.Log;

public class FileOffset extends CordovaPlugin {
    private static final String TAG = "FileOffset";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("writeAtOffset")) {
            String path = args.getString(0);
            int offset = args.getInt(1);
            String data = args.getString(2);
            boolean isBase64 = args.getBoolean(3);

            this.writeAtOffset(path, offset, data, isBase64, callbackContext);
            return true;
        }
        return false;
    }

    private void writeAtOffset(String path, int offset, String data, boolean isBase64,
                               CallbackContext callbackContext) {

        // Convert file:// URL to native path if needed
        final String processedPath;
        if (path.startsWith("file://")) {
            processedPath = path.substring(7);
        } else {
            processedPath = path;
        }

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    // Get file
                    File file = new File(processedPath);
                    if (!file.exists()) {
                        callbackContext.error("File does not exist: " + processedPath);
                        return;
                    }

                    // Decode data if base64
                    byte[] bytes;
                    if (isBase64) {
                        bytes = Base64.decode(data, Base64.DEFAULT);
                    } else {
                        bytes = data.getBytes("UTF-8");
                    }

                    // Use RandomAccessFile to write at specific offset
                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                    randomAccessFile.seek(offset);
                    randomAccessFile.write(bytes);
                    randomAccessFile.close();

                    callbackContext.success();
                } catch (Exception e) {
                    Log.e(TAG, "Error writing to file at offset: " + e.getMessage());
                    callbackContext.error("Error writing to file: " + e.getMessage());
                }
            }
        });
    }
}