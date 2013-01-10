package org.sistemavotacion.test.tarea;

import java.util.List;
import javax.swing.SwingWorker;
import org.sistemavotacion.modelo.Respuesta;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public interface LanzadorWorker {
    
    public void process(List<String> messages);
    public void mostrarMensaje(String mensaje);
    public void mostrarResultadoOperacion(SwingWorker worker, Respuesta get);
    
}