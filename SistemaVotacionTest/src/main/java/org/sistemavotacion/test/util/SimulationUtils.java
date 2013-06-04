package org.sistemavotacion.test.util;

import org.sistemavotacion.modelo.ActorConIP;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SimulationUtils {
    
        
    public static String checkActor(ActorConIP actorConIP, ActorConIP.Tipo tipo) {
        if(tipo != actorConIP.getTipo()) return "SERVER IS NOT " + tipo.toString();
        if(actorConIP.getEnvironmentMode() == null || ActorConIP.
                EnvironmentMode.DEVELOPMENT == actorConIP.getEnvironmentMode()) {
            return null;
        } else return "SERVER NOT IN DEVELOPMENT MODE. Server mode:" + 
                actorConIP.getEnvironmentMode() + 
                " - server: " + tipo.toString();
    }
}
