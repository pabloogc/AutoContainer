package com.bq.autocontainer.app;

import android.os.Bundle;
import android.support.annotation.Nullable;

public class DummyViewerActivity extends AbstractDummyViewer {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        final DummyViewerComponent component = DaggerDummyViewerComponent.builder()
                .dummyViewerModule(new DummyViewerModule(this))
                .build();

        component.inject(this);

        super.onCreate(savedInstanceState);
    }
}
