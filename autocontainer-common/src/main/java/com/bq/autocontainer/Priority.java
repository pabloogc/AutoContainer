package com.bq.autocontainer;

/**
 * Default priorities separated in three buckets, HIGH, MID, LOW separated by 100.
 * <p>
 * Priorities are plain integers, you don't have to use this values.
 *
 * @see Plugin#priority()
 */
public interface Priority {

    int HIGH_HIGH = 100;
    int HIGH = 200;
    int HIGH_LOW = 300;

    int MID_HIGH = 400;
    int MID = 500;
    int MID_DOWN = 600;

    int LOW_HIGH = 700;
    int LOW = 800;
    int LOW_LOW = 900;
}
