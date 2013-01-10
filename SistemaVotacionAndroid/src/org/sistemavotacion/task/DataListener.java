package org.sistemavotacion.task;

public interface DataListener<T> {

	  void updateData(int statusCode, T data);
	  void setException(String exceptionMsg);
	  
}
