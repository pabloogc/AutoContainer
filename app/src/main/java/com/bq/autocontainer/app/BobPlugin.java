package com.bq.autocontainer.app;


import android.view.KeyEvent;
import android.view.Menu;
import com.bq.autocontainer.Callback;
import com.bq.autocontainer.CallbackMethod;
import com.bq.autocontainer.Plugin;

import javax.inject.Inject;

import static com.bq.autocontainer.Callback.CallSuper.AFTER;

@Plugin
public class BobPlugin {


    @Inject
    AlicePlugin.MySuperObject mySuperObject;

    @Callback
    public void onResume() {
    }

    @Callback
    public void onBackPressed(CallbackMethod<Void> m) {
        m.call();
    }

    @Callback
    public void onCreateOptionsMenu(CallbackMethod<Boolean> m, Menu menu) {
        if (m.overridden()) return;
        // Do something with the menu
        m.override(true); // Return true in the callback method
    }

    @Callback(callSuper = AFTER)
    public void onKeyDown(int keyCode, KeyEvent event) {

    }
}
