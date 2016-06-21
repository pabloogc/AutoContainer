package com.bq.autoactivity;


import javax.inject.Inject;
import javax.inject.Named;

@Plugin
public class AlicePlugin {
    @Inject BobPlugin bobPlugin;
}
