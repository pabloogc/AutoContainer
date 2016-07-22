package com.bq.autocontainer.app;


import android.os.Bundle;
import android.support.annotation.Nullable;
import com.bq.autocontainer.Callback;
import com.bq.autocontainer.Plugin;
import dagger.Provides;

import javax.inject.Inject;

@Plugin
public class AlicePlugin {

    @Inject BobPlugin bobPlugin;
    @Inject ViewerActivity activity;

    private final AliceSuperObject superObject = new AliceSuperObject();

    @Callback
    public void onCreate(@Nullable Bundle savedInstanceState) {

    }

    @Callback
    public void onResume() {

    }


    public static final class AliceSuperObject {
        void sayHello() {
            System.out.println("Hello");
        }
    }

    @dagger.Module
    static final class Module {
        @Provides
        static AliceSuperObject provideMySuperObject(AlicePlugin alicePlugin) {
            return alicePlugin.superObject;
        }
    }
}
