package com.bq.autoactivity;


import android.support.v7.app.AppCompatActivity;

import javax.inject.Named;
import javax.inject.Singleton;

@AutoActivity(
        modules = {TestModule.class},
        dependencies = {},
        baseActivity = AppCompatActivity.class
)
@Singleton
public interface DummyViewer {

    AlicePlugin alicePlugin();

    @Named("Bob")
    @Singleton
    BobPlugin bobPlugin();

    @Named("Carl")
    BobPlugin carlPlugin();
}
