package com.bq.autoactivity;


import android.view.KeyEvent;

import javax.inject.Singleton;

@Plugin
public class BobPlugin {

//
//    @Callback
//    protected void onResume() {
//    }

    @Callback
    protected void onBackPressed(ActivityMethod<Void> m) {
        m.callActivityMethod();
    }

    @Callback
    public void onKeyDown(int keyCode, KeyEvent event) {

    }
}
