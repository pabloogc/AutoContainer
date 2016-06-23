package com.bq.autoactivity;


import android.os.Bundle;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;

@Plugin
public class AlicePlugin {
    @Inject
    BobPlugin bobPlugin;

    @ActivityCallback
    public void onCreate(@Nullable Bundle savedInstanceState) {

    }

    @ActivityCallback
    public void onResume() {

    }
}
