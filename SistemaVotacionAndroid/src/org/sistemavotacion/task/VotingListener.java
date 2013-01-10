package org.sistemavotacion.task;

import java.io.File;
import org.sistemavotacion.modelo.ReciboVoto;
import android.os.AsyncTask;

public interface VotingListener {

	  void proccessAccessRequest(File accessRequest);
	  void setRunningTask(AsyncTask runningTask);
	  void proccessReceipt(ReciboVoto receipt);
	  void setException(String exceptionMsg);
	  
}
