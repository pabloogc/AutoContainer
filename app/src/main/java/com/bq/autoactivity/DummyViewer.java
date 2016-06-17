package com.bq.autoactivity;


import javax.inject.Named;
import javax.inject.Singleton;

@AutoActivity(
        modules = {TestModule.class},
        dependencies = {}
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
