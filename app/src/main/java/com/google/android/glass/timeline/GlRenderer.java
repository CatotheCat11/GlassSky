package com.google.android.glass.timeline;

import javax.microedition.khronos.egl.EGLConfig;

public interface GlRenderer {
    void onDrawFrame();

    void onSurfaceChanged(int i, int i2);

    void onSurfaceCreated(EGLConfig eGLConfig);
}
