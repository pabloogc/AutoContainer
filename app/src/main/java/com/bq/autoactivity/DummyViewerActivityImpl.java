package com.bq.autoactivity;

import android.os.Bundle;
import android.support.annotation.Nullable;

public class DummyViewerActivityImpl extends DummyViewerActivity {

    private final ActivityMethod<String> m = new ActivityMethod<String>() {
        @Override
        public void captureArguments(Object... args) {

        }

        @Override
        public void releaseArguments() {

        }

        @Override
        public String callActivityMethod() {
            return null;
        }
    };

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {

        final DummyViewerComponent component = DaggerDummyViewerComponent.builder()
              .dummyViewerModule(new DummyViewerModule(this))
              .build();

        component.inject(this);

        super.onCreate(savedInstanceState);
    }
}
