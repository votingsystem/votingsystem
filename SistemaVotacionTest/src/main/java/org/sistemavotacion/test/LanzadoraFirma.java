package org.sistemavotacion.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.concurrent.Callable;
import javax.mail.internet.MimeMessage;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.modelo.InfoFirma;
import org.sistemavotacion.modelo.Respuesta;
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
            MimeMessage mimeMessage = signedMailGenerator.genMimeMessage(infoFirma.getFrom(), 
                infoFirma.getEvento().getControlAcceso().getNombreNormalizado(), 
                EnvioFirmas.obtenerFirmaParaEventoJSON(infoFirma.getEvento()),
                asuntoMensaje, null); 
            mimeMessage.writeTo(new FileOutputStream(docFirmado));
            String urlRecepcionFirmas = infoFirma.getEvento().getControlAcceso().getServerURL() + 
            		"/recolectorFirma/guardarAdjuntandoValidacion"; 
            respuesta = Contexto.getHttpHelper().sendFile(
                    docFirmado, Contexto.SIGNED_CONTENT_TYPE, urlRecepcionFirmas);
            if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(null,
                        new ByteArrayInputStream(respuesta.getMensaje().getBytes()),
                        docFirmado.getName());
                respuesta = new Respuesta(respuesta.getCodigoEstado(), dnieMimeMessage);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        infoFirma.setRespuesta(respuesta);
        return infoFirma;
    }
    
    
}