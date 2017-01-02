package org.votingsystem.dto.metadata;

import org.votingsystem.dto.UserDto;
import org.votingsystem.http.SystemEntityType;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MetadataUtils {

    public static MetadataDto initMetadata(SystemEntityType entityType, String entityId, Properties properties,
           X509Certificate signingCert, X509Certificate encryptCert) throws IOException, CertificateEncodingException {
        KeyDto encryptX509Cert = new KeyDto(signingCert, KeyDto.Use.ENCRYPTION);
        KeyDto signX509Cert = new KeyDto(encryptCert, KeyDto.Use.SIGN);
        MetadataDto metadata = new MetadataDto(entityType, entityId,
                new HashSet<>(Arrays.asList(encryptX509Cert, signX509Cert)));

        //Location data
        String countryCode = (String) properties.get("countryCode");
        String languageCode = (String) properties.get("languageCode");
        String city = (String) properties.get("city");
        String address = (String) properties.get("address");
        String postalCode = (String) properties.get("postalCode");
        org.votingsystem.util.Country country = org.votingsystem.util.Country.valueOf(countryCode);
        LocationDto location = new LocationDto(city, address, postalCode, country, languageCode);
        //Contact person data
        String company = (String) properties.get("company");

        String givenName = (String) properties.get("givenName");
        String surName = (String) properties.get("surName");
        String emailAddress = (String) properties.get("emailAddress");
        String telephoneNumber = (String) properties.get("telephoneNumber");
        UserDto contactPerson = new UserDto(company, givenName, surName, emailAddress,
                telephoneNumber, null);

        String organizationName = (String) properties.get("organizationName");
        String organizationUnit = (String) properties.get("organizationUnit");
        String organizationURL = (String) properties.get("organizationURL");
        OrganizationDto organization = new OrganizationDto(organizationName, organizationUnit, organizationURL);

        metadata.setLanguage(languageCode).setLocation(location).setContactPerson(contactPerson)
                .setOrganization(organization);
        return metadata;
    }

}
