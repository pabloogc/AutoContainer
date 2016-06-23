package com.bq.autoactivity;


import android.view.KeyEvent;
import android.view.Menu;

import com.bq.autoactivity.ActivityCallback.CallSuper;

import static com.bq.autoactivity.ActivityCallback.CallSuper.*;

@Plugin
public class BobPlugin {


    @ActivityCallback
    public void onResume() {
    }

    @ActivityCallback
    public void onBackPressed(ActivityMethod<Void> m) {
        m.callActivityMethod();
    }

    @ActivityCallback
    public void onCreateOptionsMenu(ActivityMethod<Boolean> m, Menu menu) {

    }

    @ActivityCallback(callSuper = AFTER)
    public void onKeyDown(int keyCode, KeyEvent event) {

    }
}
