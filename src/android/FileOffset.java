package org.ote.cordova;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import android.content.ContentResolver;
import android.net.Uri;
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
        } else if (action.equals("rewriteWithHeader")) {
            String originalFileUri = args.getString(0);
            String headerFileUri = args.getString(1);
            int clusterOffset = args.getInt(2);

            this.rewriteWithHeader(originalFileUri, headerFileUri, clusterOffset, callbackContext);
            return true;
        }
        return false;
    }

    private void rewriteWithHeader(String originalFileUri, String headerFileUri, int clusterOffset, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                File privateTempFile = null;
                Log.d(TAG, "Starting rewriteWithHeader");
                try {
                    ContentResolver resolver = cordova.getActivity().getContentResolver();
                    Uri originalUri = Uri.parse(originalFileUri);
                    Uri headerUri = Uri.parse(headerFileUri);

                    // --- Create a temporary file in the app's private cache directory ---
                    File privateCacheDir = cordova.getActivity().getCacheDir();
                    String fileName = new File(originalUri.getPath()).getName();
                    privateTempFile = new File(privateCacheDir, fileName + ".tmp");
                    Log.d(TAG, "Private temp file path: " + privateTempFile.getAbsolutePath());

                    // --- Write the corrected video to the private temporary file ---
                    Log.d(TAG, "Writing to private temp file...");
                    try (
                        InputStream headerStream = resolver.openInputStream(headerUri);
                        InputStream originalStream = resolver.openInputStream(originalUri);
                        OutputStream tempOutStream = new FileOutputStream(privateTempFile)
                    ) {
                        if (headerStream == null || originalStream == null) {
                            throw new Exception("Failed to open input streams.");
                        }
                        // 1. Write the new header
                        byte[] headerBuffer = new byte[8192];
                        int headerBytesRead;
                        while ((headerBytesRead = headerStream.read(headerBuffer)) != -1) {
                            tempOutStream.write(headerBuffer, 0, headerBytesRead);
                        }
                        Log.d(TAG, "Header written to temp file.");

                        // 2. Append the video data (clusters)
                        originalStream.skip(clusterOffset);
                        byte[] clusterBuffer = new byte[8192];
                        int clusterBytesRead;
                        while ((clusterBytesRead = originalStream.read(clusterBuffer)) != -1) {
                            tempOutStream.write(clusterBuffer, 0, clusterBytesRead);
                        }
                        Log.d(TAG, "Clusters written to temp file.");
                    }

                    // --- Now, copy the corrected temp file to the public directory ---
                    Log.d(TAG, "Copying temp file to public storage: " + originalUri);
                    try (
                        InputStream tempInStream = new FileInputStream(privateTempFile);
                        OutputStream finalOutStream = resolver.openOutputStream(originalUri, "w") // "w" truncates the file
                    ) {
                        if (finalOutStream == null) {
                            throw new Exception("Failed to open output stream for original file URI.");
                        }
                        byte[] moveBuffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = tempInStream.read(moveBuffer)) != -1) {
                            finalOutStream.write(moveBuffer, 0, bytesRead);
                        }
                        Log.d(TAG, "Successfully copied temp file to public storage.");
                    }

                    callbackContext.success();
                    Log.d(TAG, "rewriteWithHeader successful.");

                } catch (Exception e) {
                    Log.e(TAG, "Error rewriting file: " + e.toString());
                    e.printStackTrace();
                    callbackContext.error("Error rewriting file: " + e.toString());
                } finally {
                    if (privateTempFile != null && privateTempFile.exists()) {
                        privateTempFile.delete();
                        Log.d(TAG, "Cleaned up private temp file.");
                    }
                }
            }
        });
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
