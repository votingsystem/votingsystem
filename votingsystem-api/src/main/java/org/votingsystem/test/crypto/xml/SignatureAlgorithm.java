package org.votingsystem.test.crypto.xml;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum SignatureAlgorithm {

    RSA_SHA_256("SHA256withRSA", "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"),
    RSA_SHA_512("SHA512withRSA", "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512");


    private String name;
    private String xmlId;

    SignatureAlgorithm(String name, String xmlId) {
        this.name = name;
        this.xmlId = xmlId;
    }

    public static SignatureAlgorithm forName(String name) {
        for (SignatureAlgorithm sigantureAlgorithm : SignatureAlgorithm.values()) {
            if (sigantureAlgorithm.getName().equals(name))
                return sigantureAlgorithm;
        }
        throw new RuntimeException("Unsupported signature name: " + name);
    }

    public static SignatureAlgorithm forXML(String xmlName) {
        for (SignatureAlgorithm sigantureAlgorithm : SignatureAlgorithm.values()) {
            if (sigantureAlgorithm.getXmlId().equals(xmlName))
                return sigantureAlgorithm;
        }
        throw new RuntimeException("Unsupported signature xmlName: " + xmlName);
    }

    public String getName() {
        return name;
    }

    public String getXmlId() {
        return xmlId;
    }

}
