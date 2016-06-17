package com.bq.autoactivity;


import javax.inject.Inject;
import javax.inject.Named;

public class AlicePlugin {
    @Inject @Named("Carl") BobPlugin bobPlugin;
}
