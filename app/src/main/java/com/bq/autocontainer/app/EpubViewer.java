package com.bq.autocontainer.app;


import com.bq.autocontainer.AutoContainer;
import com.bq.autocontainer.Plugin;

@AutoContainer(
        modules = {AlicePlugin.Module.class},
        dependencies = {},
        baseClass = ViewerActivity.class,
        className = "EpubViewerActivity"
)
public interface EpubViewer {

    @Plugin(priority = 0)
    AlicePlugin alicePlugin();

    @Plugin(priority = 1)
    BobPlugin bobPlugin();

    @Plugin(priority = 2)
    CharlesPlugin charlesPlugin();
}
