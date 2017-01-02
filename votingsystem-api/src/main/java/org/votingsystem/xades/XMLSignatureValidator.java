package org.votingsystem.xades;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class with XML signature validation utils.
 *
 * @author oesia
 */
public class XMLSignatureValidator {

    private static final Logger log = Logger.getLogger(XMLSignatureValidator.class.getName());

    public static X509Certificate validate(byte[] documentSignedBytes) throws ParserConfigurationException, IOException,
            SAXException, XMLSignatureException, MarshalException {
        // Instantiate the document to be validated
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(documentSignedBytes));
        // Find BinaryContentRule element
        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nl.getLength() == 0) {
            throw new XMLSignatureException("Cannot find BinaryContentRule element");
        }
        // Create a DOM XMLSignatureFactory that will be used to unmarshal the document containing the XMLSignature
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        // Create a DOMValidateContext and specify a KeyValue KeySelector and document context
        KeyValueKeySelector keyValueKeySelector = new KeyValueKeySelector();
        DOMValidateContext valContext = new DOMValidateContext (keyValueKeySelector, nl.item(0));
        // unmarshal the XMLSignature
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        // Validate the XMLSignature (generated above)
        boolean coreValidity = signature.validate(valContext);
        // Check core validation status
        if (coreValidity == false) {
            boolean sv = signature.getSignatureValue().validate(valContext);
            String message = "BinaryContentRule failed core validation - signature validation status: " + sv;
            // check the validation status of each Reference
            Iterator i = signature.getSignedInfo().getReferences().iterator();
            for (int j = 0; i.hasNext(); j++) {
                Reference ref = (Reference) i.next();
                boolean refValid = ref.validate(valContext);
                message += " - ref["+j+"] validity status: " + refValid;
                throw new XMLSignatureException(message);
            }
            throw new XMLSignatureException(message);
        }
        return keyValueKeySelector.getSigningCert();
    }

    /**
     * KeySelector which retrieves the public key out of the KeyValue element and returns it.
     * NOTE: If the key algorithm doesn't match signature algorithm, then the public key will be ignored.
     */
    private static class KeyValueKeySelector extends KeySelector {

        private X509Certificate signingCert;

        public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method,
                                        XMLCryptoContext context) throws KeySelectorException {
            if (keyInfo == null) {
                throw new KeySelectorException("Null KeyInfo object!");
            }
            SignatureMethod sm = (SignatureMethod) method;
            List list = keyInfo.getContent();

            for (int i = 0; i < list.size(); i++) {
                XMLStructure xmlStructure = (XMLStructure) list.get(i);
                if (xmlStructure instanceof KeyValue) {
                    PublicKey pk = null;
                    try {
                        pk = ((KeyValue)xmlStructure).getPublicKey();
                    } catch (KeyException ke) {
                        throw new KeySelectorException(ke);
                    }
                    return new SimpleKeySelectorResult(pk);
                } else if (xmlStructure instanceof X509Data) {
                    PublicKey pk = null;
                    for (Object data : ((X509Data) xmlStructure).getContent()) {
                        if (data instanceof X509Certificate) {
                            signingCert = (X509Certificate) data;
                            if(pk == null) pk = signingCert.getPublicKey();
                        }
                    }
                    log.log(Level.FINE, "signatureMethod algoritm: " + sm.getAlgorithm() + " - public key algoritm: " +
                            pk.getAlgorithm() + " - signer:" + signingCert.getSubjectDN().toString());
                    return new SimpleKeySelectorResult(pk);
                }
            }
            throw new KeySelectorException("No KeyValue element found!");
        }

        public X509Certificate getSigningCert() {
            return signingCert;
        }
    }

    private static class SimpleKeySelectorResult implements KeySelectorResult {
        private PublicKey pk;
        SimpleKeySelectorResult(PublicKey pk) {
            this.pk = pk;
        }
        public Key getKey() { return pk; }
    }

}
