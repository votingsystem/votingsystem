package org.sistemavotacion.test.tarea;

import java.util.List;
import javax.swing.SwingWorker;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public interface LanzadorWorker {
    
    public void process(List<String> messages, SwingWorker worker);
    public void mostrarResultadoOperacion(SwingWorker worker);
    
}