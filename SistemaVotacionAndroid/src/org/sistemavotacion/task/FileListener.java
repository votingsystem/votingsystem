package org.sistemavotacion.task;

public interface FileListener {

	  void porcessFileData(byte[] fileData);
	  void setException(String exceptionMsg);
	  
}
