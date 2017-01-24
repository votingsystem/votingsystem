package org.votingsystem.androidcurrency.xml;

import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.votingsystem.dto.AddressDto;
import org.votingsystem.dto.indentity.SessionCertificationDto;

import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class XmlWriter {

    private static final Logger log = Logger.getLogger(org.votingsystem.android.xml.XmlWriter.class.getName());

/*
    private AddressDto address;

    private String browserCsr;
    private String browserCsrSigned;

    private String mobileCsr;
    private String mobileCsrSigned;

    private String token;
    private String userUUID;

*/

    public static byte[] write(SessionCertificationDto certRequest) throws Exception {
        Document doc = new Document();
        Element certificationRequestElement = doc.createElement("", "CertificationRequest");
        if(certRequest.getBrowserCsr() != null) {
            Element browserCsr = doc.createElement("", "browserCsr");
            browserCsr.addChild(Node.TEXT, certRequest.getBrowserCsr());
            certificationRequestElement.addChild(Node.ELEMENT, browserCsr);
        }
        if(certRequest.getBrowserCsrSigned() != null) {
            Element browserCsrSigned = doc.createElement("", "browserCsrSigned");
            browserCsrSigned.addChild(Node.TEXT, certRequest.getBrowserCsrSigned());
            certificationRequestElement.addChild(Node.ELEMENT, browserCsrSigned);
        }
        if(certRequest.getMobileCsr() != null) {
            Element mobileCsr = doc.createElement("", "mobileCsr");
            mobileCsr.addChild(Node.TEXT, certRequest.getMobileCsr());
            certificationRequestElement.addChild(Node.ELEMENT, mobileCsr);
        }
        if(certRequest.getMobileCsrSigned() != null) {
            Element mobileCsrSigned = doc.createElement("", "mobileCsrSigned");
            mobileCsrSigned.addChild(Node.TEXT, certRequest.getMobileCsrSigned());
            certificationRequestElement.addChild(Node.ELEMENT, mobileCsrSigned);
        }
        if(certRequest.getUserUUID() != null) {
            Element userUUIDElement = doc.createElement("", "userUUID");
            userUUIDElement.addChild(Node.TEXT, certRequest.getUserUUID());
            certificationRequestElement.addChild(Node.ELEMENT, userUUIDElement);
        }
        if(certRequest.getToken() != null) {
            Element tokenElement = doc.createElement("", "token");
            tokenElement.addChild(Node.TEXT, certRequest.getToken());
            certificationRequestElement.addChild(Node.ELEMENT, tokenElement);
        }
        if(certRequest.getAddress() != null) {
            certificationRequestElement.addChild(Node.ELEMENT, getAddressElement(certRequest.getAddress()));
        }
        doc.addChild(Node.ELEMENT, certificationRequestElement);
        return org.votingsystem.util.XMLUtils.serialize(doc);
    }

    public static Element getAddressElement(AddressDto address) {
        Document doc = new Document();
        Element addressElement = doc.createElement("", "Address");
        if(address.getId() != null) {
            addressElement.setAttribute(null, "Id", address.getId().toString());
        }
        if(address.getMetaInf() != null) {
            Element metaInfElement = doc.createElement("", "metaInf");
            metaInfElement.addChild(Node.TEXT, address.getMetaInf());
            addressElement.addChild(Node.ELEMENT, metaInfElement);
        }
        if(address.getProvince() != null) {
            Element provincElement = doc.createElement("", "province");
            provincElement.addChild(Node.TEXT, address.getProvince());
            addressElement.addChild(Node.ELEMENT, provincElement);
        }
        if(address.getCountry() != null) {
            Element countryElement = doc.createElement("", "country");
            countryElement.addChild(Node.TEXT, address.getCountry().name());
            addressElement.addChild(Node.ELEMENT, countryElement);
        }
        if(address.getCity() != null) {
            Element cityElement = doc.createElement("", "city");
            cityElement.addChild(Node.TEXT, address.getCity());
            addressElement.addChild(Node.ELEMENT, cityElement);
        }
        if(address.getAddress() != null) {
            Element addressNameElement = doc.createElement("", "address");
            addressNameElement.addChild(Node.TEXT, address.getAddress());
            addressNameElement.addChild(Node.ELEMENT, addressNameElement);
        }
        if(address.getPostalCode() != null) {
            addressElement.setAttribute(null, "PostalCode", address.getPostalCode());
        }
        return addressElement;
    }

}
