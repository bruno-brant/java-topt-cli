package com.dreamsforge;

/**
 * A class for handling a variety of utility things.  This was mostly made
 * because I needed to centralize dialog related constants. I foresee this class
 * being used for other code sharing across Activities in the future, however.
 *
 * @author alexei@czeskis.com (Alexei Czeskis)
 */
class Utilities {
    private static final long SECOND_IN_MILLIS = 1000;
    static final long MINUTE_IN_MILLIS = 60 * SECOND_IN_MILLIS;

    // Constructor -- Does nothing yet
    private Utilities() {
    }

    static long millisToSeconds(long timeMillis) {
        return timeMillis / 1000;
    }
}
