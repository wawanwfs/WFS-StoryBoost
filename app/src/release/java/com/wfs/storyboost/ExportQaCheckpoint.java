package com.wfs.storyboost;

import android.net.Uri;

/** No instrumentation callbacks in release builds. */
final class ExportQaCheckpoint {
    private ExportQaCheckpoint() {}
    static void reached(MediaSaver.Stage stage, String name, Uri row) {}
 }
