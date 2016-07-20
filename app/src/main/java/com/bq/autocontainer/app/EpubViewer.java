package com.bq.autocontainer.app;


import com.bq.autocontainer.AutoContainer;

@AutoContainer(
        modules = {AlicePlugin.Module.class},
        dependencies = {},
        baseClass = ViewerActivity.class,
        className = "EpubViewerActivity"
)
public interface EpubViewer {

    AlicePlugin alicePlugin();

    BobPlugin bobPlugin();

    CharlesPlugin charlesPlugin();
}
