package com.bq.autoactivity;


import android.view.KeyEvent;

import com.bq.autoactivity.ActivityCallback.CallSuper;

import static com.bq.autoactivity.ActivityCallback.CallSuper.*;

@Plugin
public class BobPlugin {


    @ActivityCallback
    protected void onResume() {
    }

    @ActivityCallback
    protected void onBackPressed(ActivityMethod<Void> m) {
        m.callActivityMethod();
    }

    @ActivityCallback(callSuper = AFTER)
    public void onKeyDown(int keyCode, KeyEvent event) {

    }
}
