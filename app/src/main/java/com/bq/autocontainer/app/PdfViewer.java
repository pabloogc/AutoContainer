package com.bq.autocontainer.app;


import com.bq.autocontainer.AutoContainer;
import com.bq.autocontainer.Plugin;

/**
 * Same as EpubViewer with inverted priorities
 */
@AutoContainer(
        modules = {AlicePlugin.Module.class},
        dependencies = {},
        baseClass = ViewerActivity.class,
        className = "PdfViewerActivity"
)
public interface PdfViewer {

    @Plugin(priority = 2)
    AlicePlugin alicePlugin();

    @Plugin(priority = 1)
    BobPlugin bobPlugin();

    @Plugin(priority = 0)
    CharlesPlugin charlesPlugin();
}
