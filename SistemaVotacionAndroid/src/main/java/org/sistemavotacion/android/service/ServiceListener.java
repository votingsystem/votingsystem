package org.sistemavotacion.android.service;

import org.sistemavotacion.modelo.Respuesta;

public interface ServiceListener {
	
	void proccessResponse(Integer requestId, Respuesta response);

}
