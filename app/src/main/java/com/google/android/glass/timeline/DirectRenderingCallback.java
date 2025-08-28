package com.google.android.glass.timeline;

import android.view.SurfaceHolder;

public interface DirectRenderingCallback extends SurfaceHolder.Callback {
    void renderingPaused(SurfaceHolder surfaceHolder, boolean z);
}
