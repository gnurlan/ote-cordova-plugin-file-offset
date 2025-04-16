var exec = require('cordova/exec');

var FileOffset = {
    /**
     * Write data to a specific offset within a file
     *
     * @param {String} path - Full file path
     * @param {Number} offset - Byte offset to write at
     * @param {ArrayBuffer|String} data - Data to write
     * @param {Boolean} isBase64 - Whether the string data is base64-encoded
     * @return {Promise} - Resolves when write is complete
     */
    writeAtOffset: function(path, offset, data, isBase64) {
        return new Promise(function(resolve, reject) {
            var dataArg = data;

            // Convert ArrayBuffer to base64 if needed
            if (data instanceof ArrayBuffer) {
                var binary = '';
                var bytes = new Uint8Array(data);
                var len = bytes.byteLength;
                for (var i = 0; i < len; i++) {
                    binary += String.fromCharCode(bytes[i]);
                }
                dataArg = window.btoa(binary);
                isBase64 = true;
            }

            exec(resolve, reject, 'FileOffset', 'writeAtOffset', [path, offset, dataArg, !!isBase64]);
        });
    },

    /**
     * Helper to update MP4 duration metadata
     *
     * @param {String} path - Full path to MP4 file
     * @param {Number} durationInSeconds - New duration in seconds
     * @return {Promise} - Resolves when update is complete
     */
    updateMP4Duration: function(path, durationInSeconds) {
        return new Promise(function(resolve, reject) {
            // MP4 duration is typically at a specific offset and format
            // Convert seconds to the appropriate time scale format
            var timeScale = 1000; // Milliseconds
            var durationValue = Math.floor(durationInSeconds * timeScale);

            // Create a buffer with the duration value (4 bytes, big-endian)
            var buffer = new ArrayBuffer(4);
            var view = new DataView(buffer);
            view.setUint32(0, durationValue, false); // false = big-endian

            // The mvhd atom offset is typically around 28-40 bytes in, and duration is 20 bytes after that
            // But this is unsafe to hardcode - we'll need a proper MP4 parser in production
            // For now we'll use a placeholder offset of 36+20=56 which needs to be adjusted per file
            var durationOffset = 56;

            FileOffset.writeAtOffset(path, durationOffset, buffer, false)
                .then(resolve)
                .catch(reject);
        });
    }
};

module.exports = FileOffset;
