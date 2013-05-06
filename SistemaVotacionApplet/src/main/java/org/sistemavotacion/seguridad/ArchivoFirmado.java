package org.sistemavotacion.seguridad;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class ArchivoFirmado {

    private static Logger logger = LoggerFactory.getLogger(ArchivoFirmado.class);
    
    public static final int RECIBO = 0;
    public static final int AUTOFIRMADO = 1;
    public static final int ERRONEO = 2;
    private int tipoArchivo;
    private File archivo;
    private Document document;
    private String TextoFirma;
    private String DatosUsuario;
    private String DatosCA; 
    private X509Certificate certificado;
    
    private String separador = System.getProperty("line.separator");
    
    public ArchivoFirmado (File archivo) throws
            ParserConfigurationException, SAXException, IOException{
        this.archivo = archivo;
        //this.document = XmlUtils.getDocumentFromFile(archivo, true);
    }
    
    /*public int inicializarDatos () throws Exception {
        SignatureValidator signatureValidator = new SignatureValidator();
        if (signatureValidator.validateSignature(archivo, true)) {
            certificado = signatureValidator.getCertificate();
            getTextos();
        } else {
            tipoArchivo = ERRONEO;
            throw new Exception("Error en el evento");
        }
        return tipoArchivo;
    }*/
    
    private void getTextos() {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expressionTextoFirma = "//TextoFirma";
            String expressionDatosUsuario = "//DatosUsuario";
            String expressionDatosCA = "//DatosCA";
//            Node textNode = (Node)
//                xpath.evaluate(expression, doc, XPathConstants.NODE);  
            TextoFirma = (String) xpath.evaluate(
                    expressionTextoFirma, document, XPathConstants.STRING); 
            DatosUsuario = (String) xpath.evaluate(
                    expressionDatosUsuario, document, XPathConstants.STRING); 
            DatosCA = (String) xpath.evaluate(
                    expressionDatosCA, document, XPathConstants.STRING);
            if (DatosCA.equals("")) {
                tipoArchivo = AUTOFIRMADO;
            } else tipoArchivo = RECIBO;
        } catch (XPathExpressionException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public String getDatosUsuarioCertificado () {
        X500Principal x500Principal = certificado.getSubjectX500Principal();
        return splitStringConSeparador(x500Principal.toString());
    }
    
    public String getDatosCACertificado () {
        X500Principal x500IssuerPrincipal = certificado.getIssuerX500Principal();
        return splitStringConSeparador(x500IssuerPrincipal.toString());
    }
    
    public String getDatosFirmados () {
        String datos = null;
        switch (tipoArchivo) {
            case RECIBO:
            datos = getDatosRecibo();
            break;
            case AUTOFIRMADO:
            datos = getDatosAutofirmados();
            break;
        }
        return datos;
    }
    
    private String getDatosAutofirmados () {
        String datos;
        datos = TextoFirma;
        return datos;
    }
    
    private String getDatosRecibo () {        
        String datos;
        datos = "Datos del usuario:" + separador + 
                splitStringConSeparador(DatosUsuario) + separador + separador +
                "Datos de la CA del certificado de usuario: "  + separador + 
                splitStringConSeparador(DatosCA) + separador + separador +
                "Texto firmado: " + separador + TextoFirma;
        return datos;
    }
    
    public void checkCertificateValidity () throws CertificateExpiredException, 
            CertificateNotYetValidException {
        certificado.checkValidity();
    }
    
    private String splitStringConSeparador (String cadena) {
        String[] temp = cadena.split(", ");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < temp.length; i++) {
            sb.append(temp[i] + separador);
        }
        return sb.toString();
    }
    
}
