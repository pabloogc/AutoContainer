package com.bq.autoactivity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final DummyViewerComponent component = DaggerDummyViewerComponent.builder()
                .dummyViewerModule(new DummyViewerModule())
                .build();
    }
}
