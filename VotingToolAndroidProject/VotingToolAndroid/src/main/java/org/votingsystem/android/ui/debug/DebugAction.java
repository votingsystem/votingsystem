package org.votingsystem.android.ui.debug;

import android.content.Context;

/**
 * Simple generic interface around debug actions.
 * Debug actions that implement this interface can be easily added as buttons to the
 * DebugActionRunnerFragment and have their output status, timing and message
 * logged into the log area.
 */
public interface DebugAction {
    void run(Context context, Callback callback);
    String getLabel();

    public interface Callback {
        void done(boolean success, String message);
    }
}
