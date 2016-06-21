package com.bq.autoactivity;


import android.support.v7.app.AppCompatActivity;

@AutoActivity(
      modules = {TestModule.class},
      dependencies = {},
      baseActivity = AppCompatActivity.class,
      scope = DummyViewerScope.class
)
public interface DummyViewer {

    AlicePlugin alicePlugin();

    BobPlugin bobPlugin();
}
