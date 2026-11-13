package com.wfs.storyboost;

import android.net.Uri;

/** In-process instrumentation seam; no IPC, never compiled into release. */
final class ExportQaCheckpoint {
    private ExportQaCheckpoint() {}
    interface Observer { void reached(MediaSaver.Stage stage, String name, Uri row); }
    static volatile Observer observer;
    static void reached(MediaSaver.Stage stage, String name, Uri row) {
        Observer active = observer;
        if (active != null) active.reached(stage, name, row);
    }
 }
