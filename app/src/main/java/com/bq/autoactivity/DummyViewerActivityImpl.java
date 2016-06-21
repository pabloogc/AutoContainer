package com.bq.autoactivity;

import android.os.Bundle;
import android.support.annotation.Nullable;

public class DummyViewerActivityImpl extends DummyViewerActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {

        final DummyViewerComponent component = DaggerDummyViewerComponent.builder()
              .dummyViewerModule(new DummyViewerModule(null))
              .build();

        component.inject(this);

        super.onCreate(savedInstanceState);
    }
}
