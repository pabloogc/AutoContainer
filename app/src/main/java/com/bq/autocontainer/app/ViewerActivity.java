package com.bq.autocontainer.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class ViewerActivity extends AppCompatActivity {


    //Proxy class for Epub
    public static class EpubViewerActivityImpl extends EpubViewerActivity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            final EpubViewerComponent component = DaggerEpubViewerComponent.builder()
                    .epubViewerModule(new EpubViewerModule(this))
                    .build();
            init(component);
            super.onCreate(savedInstanceState);
        }
    }
}
