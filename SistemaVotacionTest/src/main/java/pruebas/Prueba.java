package pruebas;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.json.DeJSONAObjeto;


public class Prueba {

        public static void main(String args[]) {
            String prueba = "{\"hashSolicitudAccesoBase64\":\"5RSB9Jpyu2AS/xLTTGfh5Gb92XE=\",\"origenHashCertificadoVoto\":\"2863bb54-3522-4024-9852-900b265e5fcb\",\"origenHashSolicitudAcceso\":\"1658152e-66ed-438d-9372-4e53474ef308\",\"hashCertificadoVotoBase64\":\"PvK4VRl01d2Vgi48T/r+0rf3w/A=\"}";
        try {
            DeJSONAObjeto.obtenerSolicitudAcceso(prueba);
        } catch (IOException ex) {
            Logger.getLogger(Prueba.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Prueba.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
}
