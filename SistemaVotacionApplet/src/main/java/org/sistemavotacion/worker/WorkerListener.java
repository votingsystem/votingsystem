package org.sistemavotacion.worker;

import java.util.List;
import javax.swing.SwingWorker;
import org.sistemavotacion.modelo.Respuesta;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public interface WorkerListener {
    
    public void process(List<String> messages);
    public void showResult(SwingWorker worker, Respuesta response);
    
}