package org.currency.test.android.xml;

import org.kxml2.kdom.Element;
import org.votingsystem.dto.AddressDto;
import org.votingsystem.dto.indentity.SessionCertificationDto;
import org.votingsystem.testlib.util.XMLUtils;
import org.votingsystem.util.CountryEurope;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class XmlReader {

    public static SessionCertificationDto getUserCertificationRequest(byte[] xmlBytes) throws IOException,
            XmlPullParserException {
        SessionCertificationDto userCertificationRequest = new SessionCertificationDto();
        Element certRequestElement = XMLUtils.parse(xmlBytes).getRootElement();
        userCertificationRequest.setBrowserCsr(XMLUtils.getTextChild(certRequestElement, "browserCsr"));
        userCertificationRequest.setBrowserCertificate(XMLUtils.getTextChild(certRequestElement, "browserCsrSigned"));

        userCertificationRequest.setMobileCsr(XMLUtils.getTextChild(certRequestElement, "mobileCsr"));
        userCertificationRequest.setMobileCertificate(XMLUtils.getTextChild(certRequestElement, "mobileCsrSigned"));

        Element addressElement = XMLUtils.getElement(certRequestElement,"address");
        if(addressElement != null) {
            AddressDto address = new AddressDto();
            if(addressElement.getAttributeValue(null, "Id") != null) {
                address.setId(Long.valueOf(addressElement.getAttributeValue(null, "Id")));
            }
            address.setMetaInf(XMLUtils.getTextChild(addressElement, "metaInf"));
            address.setProvince(XMLUtils.getTextChild(addressElement, "province"));
            if(addressElement.getAttributeValue(null, "Country") != null) {
                address.setCountry(CountryEurope.AT.valueOf(addressElement.getAttributeValue(null, "Country")));
            }
            address.setCity(XMLUtils.getTextChild(addressElement, "city"));
            address.setAddress(XMLUtils.getTextChild(addressElement, "address"));
            address.setPostalCode(addressElement.getAttributeValue(null, "PostalCode"));
        }
        return userCertificationRequest;
    }

}
