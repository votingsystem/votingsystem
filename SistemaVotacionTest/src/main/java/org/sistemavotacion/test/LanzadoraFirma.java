package org.sistemavotacion.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.util.concurrent.Callable;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.modelo.InfoFirma;
import org.sistemavotacion.test.modelo.Respuesta;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class LanzadoraFirma  implements Callable<InfoFirma> {
    
    private static Logger logger = LoggerFactory.getLogger(LanzadoraFirma.class);

    public static final String ASUNTO_MENSAJE_FIRMA_DOCUMENTO = "[Firma]-"; 
    
    private InfoFirma infoFirma;
    
    public LanzadoraFirma (InfoFirma infoFirma) throws Exception { 
        this.infoFirma = infoFirma;
    }
    
    
    @Override
    public InfoFirma call() throws Exception { 
        Respuesta respuesta = null;         
        try {
            String asuntoMensaje = ASUNTO_MENSAJE_FIRMA_DOCUMENTO 
                    + infoFirma.getEvento().getAsunto();  
            File file = new File(ContextoPruebas.getUserKeyStorePath(infoFirma.getFrom()));
            KeyStore mockDnie = KeyStoreHelper.crearMockDNIe(infoFirma.getFrom(), file);
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.VOTE_SIGN_MECHANISM);
            String rutaDocFirmado = ContextoPruebas.getUserDirPath(infoFirma.getFrom()) +
                    "archivoFirmado";            
            File docFirmado = new File(rutaDocFirmado);           
            docFirmado = signedMailGenerator.genFile(infoFirma.getFrom(), 
                infoFirma.getEvento().getControlAcceso().getNombreNormalizado(), 
                EnvioFirmas.obtenerFirmaParaEventoJSON(infoFirma.getEvento()),
                asuntoMensaje, null, SignedMailGenerator.Type.USER, docFirmado); 
            String urlRecepcionFirmas = infoFirma.getEvento().getControlAcceso().getServerURL() + 
            		"/recolectorFirma/guardarAdjuntandoValidacion"; 
            HttpResponse response = Contexto.getHttpHelper().enviarArchivoFirmado(
                    docFirmado, urlRecepcionFirmas);
            if (200 == response.getStatusLine().getStatusCode()) {
                SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(null,
                        new ByteArrayInputStream(EntityUtils.toByteArray(response.getEntity())),
                        docFirmado.getName());
                respuesta = new Respuesta(
                    response.getStatusLine().getStatusCode(), dnieMimeMessage);
            } else {
                respuesta = new Respuesta(response.getStatusLine().getStatusCode(),
                    EntityUtils.toString(response.getEntity()));
            }
            EntityUtils.consume(response.getEntity());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        infoFirma.setRespuesta(respuesta);
        return infoFirma;
    }
    
    
}