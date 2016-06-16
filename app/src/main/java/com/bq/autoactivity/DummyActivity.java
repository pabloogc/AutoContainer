package com.bq.autoactivity;


import dagger.Subcomponent;

@AutoActivity
public interface DummyActivity {
    AlicePlugin alicePlugin();

    BobPlugin bobPlugin();
}
