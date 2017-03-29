var test = {}

test.createSelfSignedCert = function () {
    // generate a keypair and create an X.509v3 certificate
    var keys = forge.pki.rsa.generateKeyPair(1024);
    var cert = forge.pki.createCertificate();
    cert.publicKey = keys.publicKey;
// alternatively set public key from a csr
//cert.publicKey = csr.publicKey;
    cert.serialNumber = '01';
    cert.validity.notBefore = new Date();
    cert.validity.notAfter = new Date();
    cert.validity.notAfter.setFullYear(cert.validity.notBefore.getFullYear() + 1);
    var attrs = [{
        name: 'commonName',
        value: 'votingsystem.org'
    }, {
        name: 'countryName',
        value: 'countryNameVS'
    }, {
        shortName: 'ST',
        value: 'UserShortNameVS'
    }, {
        name: 'localityName',
        value: 'UserLocalityNameVS'
    }, {
        name: 'organizationName',
        value: 'VotingSystem Test'
    }, {
        shortName: 'OU',
        value: 'TestVS'
    }];
    cert.setSubject(attrs);
    // alternatively set subject from a csr
    //cert.setSubject(csr.subject.attributes);
    cert.setIssuer(attrs);
    cert.setExtensions([{
        name: 'basicConstraints',
        cA: true
    }, {
        name: 'keyUsage',
        keyCertSign: false,
        digitalSignature: true,
        nonRepudiation: true,
        keyEncipherment: true,
        dataEncipherment: true
    }, {
        name: 'extKeyUsage',
        serverAuth: true,
        clientAuth: true,
        codeSigning: true,
        emailProtection: true,
        timeStamping: true
    }, {
        name: 'nsCertType',
        client: true,
        server: true,
        email: true,
        objsign: true,
        sslCA: true,
        emailCA: true,
        objCA: true
    }, {
        name: 'subjectAltName',
        altNames: [{
            type: 6, // URI
            value: 'https://voting.ddns.net/CurrencyServer/rest/usreVS/123456'
        }, {
            type: 7, // IP
            ip: '127.0.0.1'
        }]
    }, {
        name: 'subjectKeyIdentifier'
    }]);
    /* alternatively set extensions from a csr
     var extensions = csr.getAttribute({name: 'extensionRequest'}).extensions;
     // optionally add more extensions
     extensions.push.apply(extensions, [{
     name: 'basicConstraints',
     cA: true
     }, {
     name: 'keyUsage',
     keyCertSign: true,
     digitalSignature: true,
     nonRepudiation: true,
     keyEncipherment: true,
     dataEncipherment: true
     }]);
     cert.setExtensions(extensions);
     */
    // self-sign certificate
    cert.sign(keys.privateKey);
    return {cert:cert, privateKey:keys.privateKey};
}

test.createCMSSignedMessage = function(certPrivateKeyPair, contentToSign, timeStampTokenASN1) {
    var cmsSignedMessage = forge.pkcs7.createSignedData();
    cmsSignedMessage.content = forge.util.createBuffer(contentToSign, 'utf8');
    var cert = certPrivateKeyPair.cert;
    cmsSignedMessage.addCertificate(cert);
    var authenticatedAttributes = [{
        type: forge.pki.oids.contentType,
        value: forge.pki.oids.data
    }, {
        type: forge.pki.oids.messageDigest
        // value will be auto-populated at signing time
    }, {
        type: forge.pki.oids.signingTime,
        // value can also be auto-populated at signing time
        value: new Date()
    }]
    if(timeStampTokenASN1) {
        authenticatedAttributes.push({
            type: forge.pki.oids.signatureTimeStampToken,
            value: timeStampTokenASN1
        })
    }
    cmsSignedMessage.addSigner({
        key: certPrivateKeyPair.privateKey,
        certificate: cert,
        digestAlgorithm: forge.pki.oids.sha256,
        authenticatedAttributes: authenticatedAttributes });

    cmsSignedMessage.sign();
    return cmsSignedMessage
}

