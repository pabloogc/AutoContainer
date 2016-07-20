package com.bq.autocontainer.app;


import android.support.v7.app.AppCompatActivity;
import com.bq.autocontainer.AutoContainer;

@AutoContainer(
        modules = {AlicePlugin.Module.class},
        dependencies = {},
        baseClass = AppCompatActivity.class,
        className = "AbstractDummyViewer"
)
public interface DummyViewer {

    AlicePlugin alicePlugin();

    BobPlugin bobPlugin();
}
