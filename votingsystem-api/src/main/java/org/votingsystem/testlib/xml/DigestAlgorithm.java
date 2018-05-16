package org.votingsystem.testlib.xml;

public enum DigestAlgorithm {

    SHA1("SHA1", "SHA-1", "1.3.14.3.2.26", "http://www.w3.org/2000/09/xmldsig#sha1"),
    SHA224("SHA224", "SHA-224", "2.16.840.1.101.3.4.2.4", "http://www.w3.org/2001/04/xmldsig-more#sha224"),
    SHA256("SHA256", "SHA-256", "2.16.840.1.101.3.4.2.1", "http://www.w3.org/2001/04/xmlenc#sha256"),
    SHA384("SHA384", "SHA-384", "2.16.840.1.101.3.4.2.2", "http://www.w3.org/2001/04/xmldsig-more#sha384"),
    SHA512("SHA512", "SHA-512", "2.16.840.1.101.3.4.2.3", "http://www.w3.org/2001/04/xmlenc#sha512");



    private String name;
    private String javaName;
    private String oid;
    private String xmlId;

    DigestAlgorithm(String name, String javaName, String oid, String xmlId) {
        this.name = name;
        this.javaName = javaName;
        this.oid = oid;
        this.xmlId = xmlId;
    }

    public static DigestAlgorithm forName(String name) {
        for (DigestAlgorithm digestAlgorithm : DigestAlgorithm.values()) {
            if (digestAlgorithm.getName().equals(name))
                return digestAlgorithm;
        }
        throw new RuntimeException("Unsupported algorithm: " + name);
    }

    public static DigestAlgorithm forOID(String oid) {
        for (DigestAlgorithm digestAlgorithm : DigestAlgorithm.values()) {
            if (digestAlgorithm.getOid().equals(oid))
                return digestAlgorithm;
        }
        throw new RuntimeException("Unsupported oid: " + oid);
    }

    public static DigestAlgorithm forXML(String xmlName) {
        for (DigestAlgorithm digestAlgorithm : DigestAlgorithm.values()) {
            if (digestAlgorithm.getXmlId().equals(xmlName))
                return digestAlgorithm;
        }
        throw new RuntimeException("Unsupported xmlName: " + xmlName);
    }

    public String getName() {
        return this.name;
    }

    public String getOid() {
        return this.oid;
    }

    public String getXmlId() {
        return this.xmlId;
    }


    public String getJavaName() {
        return javaName;
    }

    public void setJavaName(String javaName) {
        this.javaName = javaName;
    }
}
