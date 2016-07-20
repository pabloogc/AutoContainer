package com.bq.autocontainer.app;


import android.os.Bundle;
import android.support.annotation.Nullable;
import com.bq.autocontainer.Callback;
import com.bq.autocontainer.Plugin;
import dagger.Provides;

import javax.inject.Inject;

@Plugin
public class AlicePlugin {
    @Inject
    BobPlugin bobPlugin;

    private final MySuperObject superObject = new MySuperObject();

    @Callback
    public void onCreate(@Nullable Bundle savedInstanceState) {

    }

    @Callback
    public void onResume() {

    }


    public static final class MySuperObject {
        int x = 33;
    }

    @dagger.Module
    public static final class Module {
        @Provides
        public static MySuperObject provideMySuperObject(AlicePlugin alicePlugin) {
            return alicePlugin.superObject;
        }
    }
}
