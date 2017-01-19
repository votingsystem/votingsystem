package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import org.votingsystem.crypto.CertUtils;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "key")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyDto {


    public enum Type {
        @JsonProperty("X509Cert")
        X509Cert;
    }

    public enum Use {
        @JsonProperty("currency-issuer")
        CURRENCY_ISSUER,
        @JsonProperty("encryption")
        ENCRYPTION,
        @JsonProperty("sign")
        SIGN;
    }

    @JacksonXmlProperty(localName = "Use", isAttribute = true)
    private Use use;
    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private Type type;
    @JacksonXmlText
    private String x509CertificateBase64;

    public KeyDto() {}


    public String getX509CertificateBase64() {
        return x509CertificateBase64;
    }

    /**
     *
     * @param x509Cert
     * @param use
     * @throws CertificateEncodingException
     */
    public KeyDto(X509Certificate x509Cert, Use use) throws CertificateEncodingException {
        this.x509CertificateBase64 = Base64.getEncoder().encodeToString(x509Cert.getEncoded());
        this.use = use;
        this.type = Type.X509Cert;

    }

    public Use getUse() {
        return use;
    }

    @JsonIgnore
    public X509Certificate getX509Certificate() throws Exception {
        if(x509CertificateBase64 == null)
            return null;
        else {
            byte[] certEncoded = Base64.getDecoder().decode(x509CertificateBase64);
            return CertUtils.loadCertificate(certEncoded);
        }
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

}
