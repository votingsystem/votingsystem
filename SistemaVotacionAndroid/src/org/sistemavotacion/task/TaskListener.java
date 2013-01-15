package org.sistemavotacion.task;

import java.util.List;
import android.os.AsyncTask;

public interface TaskListener {

    public void processTaskMessages(List<String> messages, AsyncTask task);
    public void showTaskResult(AsyncTask task);

}
