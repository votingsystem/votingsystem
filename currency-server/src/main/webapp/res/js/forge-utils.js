forge.oids['2.5.4.42'] = "givenName"
forge.oids["givenName"] = '2.5.4.42'
forge.oids['2.5.4.4'] = "surname"
forge.oids["surname"] = '2.5.4.4'

var RSAUtil = function(keyLength, privateKeyPEM, x509CertificatePEM) {
    if(privateKeyPEM && x509CertificatePEM) {
        this.privateKeyPEM = privateKeyPEM
        this.x509CertificatePEM = x509CertificatePEM
        this.x509Certificate = forge.pki.certificateFromPem(x509CertificatePEM);
        this.publicKeyPEM = forge.pki.publicKeyToPem(this.x509Certificate.publicKey)
        this.keypair = {privateKey:forge.pki.privateKeyFromPem(privateKeyPEM), publicKey: this.x509Certificate.publicKey}
    } else {
        if(!keyLength) keyLength = 2048
        this.rsa = forge.pki.rsa;
        // generate an RSA key pair synchronously
        console.log("RSAUtil - keyLength: " + keyLength)
        this.keypair = this.rsa.generateKeyPair(keyLength);
        this.publicKeyPEM = forge.pki.publicKeyToPem(this.keypair.publicKey),
        this.privateKeyPEM = forge.pki.privateKeyToPem(this.keypair.privateKey);
        this.publicKeyBase64 = vs.getPublicKeyBase64(this.keypair.publicKey);
    }
}

RSAUtil.prototype.encrypt = function(plainText) {
    var encrypted = this.keypair.publicKey.encrypt(plainText, 'RSAES-PKCS1-V1_5');
    return forge.util.encode64(encrypted);
}

RSAUtil.prototype.decrypt = function(encryptedBase64) {
    var encrypted = forge.util.decode64(encryptedBase64)
    return this.keypair.privateKey.decrypt(encrypted, 'RSAES-PKCS1-V1_5');
}

RSAUtil.prototype.decryptCMS = function(encryptedPEM) {
    var encryptedCMSMsg = forge.pkcs7.messageFromPem(encryptedPEM);
    encryptedCMSMsg.decrypt(encryptedCMSMsg.recipients[0], this.keypair.privateKey);
    return encryptedCMSMsg;
}

RSAUtil.prototype.getCSR = function(userData) {
    this.csr = forge.pki.createCertificationRequest();
    this.csr.publicKey = this.keypair.publicKey;
    var csrSubject = [];
    if(userData.serialName)
        csrSubject.push({ name: 'serialName', value: userData.serialName});
    if(userData.surname)
        csrSubject.push({ name: 'surname', value: userData.surname});
    if(userData.givenName)
        csrSubject.push({ name: 'givenName', value: userData.givenName});

    this.csr.setSubject(csrSubject);
    this.csr.sign(this.keypair.privateKey);
    var verified = this.csr.verify();
    return forge.pki.certificationRequestToPem(this.csr);
}

RSAUtil.prototype.initCSR = function(x509CertificatePEM) {
    this.x509CertificatePEM = x509CertificatePEM
    this.x509Certificate = forge.pki.certificateFromPem(x509CertificatePEM);
}

RSAUtil.prototype.sign = function(contentToSign, callback) {
    if(typeof contentToSign !== 'string') contentToSign = JSON.stringify(contentToSign)
    vs.getTimeStampToken(contentToSign, function (timeStampTokenASN1) {
        console.log("timeStampTokenASN1:", timeStampTokenASN1, " - contentToSign: ", contentToSign)
        var cmsSignedMessage = forge.pkcs7.createSignedData();
        cmsSignedMessage.content = forge.util.createBuffer(contentToSign, 'utf8');
        cmsSignedMessage.addCertificate(this.x509Certificate);
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
        }, {
            type: forge.pki.oids.signatureTimeStampToken,
            // value can also be auto-populated at signing time
            value: timeStampTokenASN1
        }]
        cmsSignedMessage.addSigner({
            key: this.keypair.privateKey,
            certificate: this.x509Certificate,
            digestAlgorithm: forge.pki.oids.sha256,
            authenticatedAttributes: authenticatedAttributes 
        });
        cmsSignedMessage.sign()
        cmsSignedMessage = forge.pkcs7.messageToPem(cmsSignedMessage);
        callback(cmsSignedMessage);
    }.bind(this))
}

RSAUtil.prototype.signAndSend = function(contentToSign, url, callback) {
    this.sign(contentToSign, function (cmsSignedMessage) {
        vs.httpPost(url, callback , cmsSignedMessage, "application/pkcs7-signature");
    })
}

RSAUtil.prototype.decryptMessage = function(messageJSON) {
    var decryptedMsg = this.decryptCMS(messageJSON.encryptedMessage)
    console.log("decryptedMsg: ", decryptedMsg);

    if(decryptedMsg.content && decryptedMsg.content.data) {
        var encryptedContentJSON = toJSON(decryptedMsg.content.data);


        console.log("encryptedContentJSON: ", encryptedContentJSON);

        if(encryptedContentJSON.socketOperation != null)
            messageJSON.socketOperation = encryptedContentJSON.socketOperation;
        if(encryptedContentJSON.operation != null)
            messageJSON.operation = encryptedContentJSON.operation;
        if(encryptedContentJSON.step != null)
            messageJSON.step = encryptedContentJSON.step;
        if(encryptedContentJSON.statusCode != null)
            messageJSON.statusCode = encryptedContentJSON.statusCode;
        if(encryptedContentJSON.operationCode != null)
            messageJSON.operationCode = encryptedContentJSON.operationCode;
        if(encryptedContentJSON.deviceFromUUID != null)
            messageJSON.deviceFromUUID = encryptedContentJSON.deviceFromUUID;
        if(encryptedContentJSON.deviceToUUID != null)
            messageJSON.deviceToUUID = encryptedContentJSON.deviceToUUID;
        if(encryptedContentJSON.userFromName  != null)
            messageJSON.userFromName = encryptedContentJSON.userFromName;
        if(encryptedContentJSON.userToName != null)
            messageJSON.userToName = encryptedContentJSON.userToName;
        if(encryptedContentJSON.deviceFromName != null)
            messageJSON.deviceFromName = encryptedContentJSON.deviceFromName;
        if(encryptedContentJSON.deviceToName != null)
            messageJSON.deviceToName = encryptedContentJSON.deviceToName;
        if(encryptedContentJSON.message != null)
            messageJSON.message = encryptedContentJSON.message;
        if(encryptedContentJSON.base64Data != null)
            messageJSON.base64Data = encryptedContentJSON.base64Data;
        if(encryptedContentJSON.subject != null)
            messageJSON.subject = encryptedContentJSON.subject;
        if(encryptedContentJSON.publicKeyPEM != null)
            messageJSON.publicKeyPEM = encryptedContentJSON.publicKeyPEM;
        if(encryptedContentJSON.aesParams != null)
            messageJSON.aesParams = encryptedContentJSON.aesParams;
        if(encryptedContentJSON.certificatePEM != null)
            messageJSON.certificatePEM = encryptedContentJSON.certificatePEM;
        if(encryptedContentJSON.timeLimited != null)
            messageJSON.timeLimited = encryptedContentJSON.timeLimited;
        if(encryptedContentJSON.CurrencySet != null)
            messageJSON.CurrencySet = encryptedContentJSON.CurrencySet;
        if(encryptedContentJSON.date != null)
            messageJSON.date = encryptedContentJSON.date;
        if(encryptedContentJSON.device != null)
            messageJSON.device = encryptedContentJSON.device;
        if(encryptedContentJSON.UUID != null)
            messageJSON.UUID = encryptedContentJSON.UUID;
        if(encryptedContentJSON.locale != null)
            messageJSON.locale = encryptedContentJSON.locale;
        messageJSON.encryptedMessage = null;
    } else console.error("encrypted content not found")

    console.log("messageJSON: ", messageJSON);
    return messageJSON;
}

vs.getPublicKeyBase64 = function(publicKey) {
    var derBytes = forge.asn1.toDer(forge.pki.publicKeyToAsn1(publicKey)).getBytes()
    return  forge.util.encode64(derBytes);
}

vs.extractUserInfoFromCert = function(x509Certificate) {
    if(typeof x509Certificate === 'string') x509Certificate = forge.pki.certificateFromPem(x509Certificate)
    var result = {}
    var subjectAttrs = x509Certificate.subject.attributes
    for (var i = 0; i < subjectAttrs.length; ++i) {
        if (subjectAttrs[i].type === forge.pki.oids.serialName) result.serialName = subjectAttrs[i].value
        if (subjectAttrs[i].type === forge.pki.oids.givenName) result.givenName = subjectAttrs[i].value
        if (subjectAttrs[i].type === forge.pki.oids.surname) result.surname = subjectAttrs[i].value
    }
    result.fullName = result.givenName + " " + result.surname;
    return result
}

vs.operationCode = function() {
  return Math.random().toString(36).substring(2, 6).toUpperCase();
}

vs.encryptToCMS = function (receptorCertPEM, jsonToEncrypt) {
    var p7 = forge.pkcs7.createEnvelopedData();
    var cert = forge.pki.certificateFromPem(receptorCertPEM);
    p7.addRecipient(cert);
    jsonToEncrypt.publicKeyPEM = this.publicKeyPEM
    var contentToEncrypt = JSON.stringify(jsonToEncrypt)
    p7.content = forge.util.createBuffer(contentToEncrypt);
    p7.encrypt();
    return forge.pkcs7.messageToPem(p7);
}

vs.getTSTInfoFromTimeStampTokenBase64 = function (base64TimeStampToken) {
    var timeStampTokenDer =  forge.util.decode64(base64TimeStampToken)
    var timeStampTokenASN1 = forge.asn1.fromDer(timeStampTokenDer)
    return vs.getTSTInfoFromTimeStampTokenASN1(timeStampTokenASN1)
}

vs.getTSTInfoFromTimeStampTokenASN1 = function (timeStampTokenASN1) {
    var capture = {};
    var errors = [];
    if(!forge.asn1.validate(timeStampTokenASN1, forge.pkcs7asn1.timeStampValidator, capture, errors)) {
        console.log(errors)
        throw new Error('ASN.1 object is not an PKCS#7 TimeStampData.');
    }
    var TSTInfoASN1 = forge.asn1.fromDer(capture.tsInfoDer)
    return vs.getTSTInfoFromASN1(TSTInfoASN1)
}

vs.getTSTInfoFromASN1 = function (TSTInfoASN1) {
    capture = {};
    errors = [];
    if(!forge.asn1.validate(TSTInfoASN1, forge.pkcs7asn1.TSTInfoValidator, capture, errors)) {
        console.log(errors)
        var error = new Error('Cannot read TSTInfo. ASN.1 object is not an TSTInfo.');
        error.errors = errors;
        throw error;
    }
    var TSTInfo = capture
    TSTInfo.date = forge.asn1.generalizedTimeToDate(capture.generalizedTime)
    TSTInfo.digestOID = forge.asn1.derToOid(capture.digestOID)
    TSTInfo.digestHex = forge.util.bytesToHex(capture.digest)
    TSTInfo.digestBase64 = forge.util.encode64(capture.digest);
    return TSTInfo
}

vs.getTimeStampToken = function (contentToSign, callback) {
    var asn1 = forge.asn1
    var digest = forge.md["sha256"].create().start().update(contentToSign).digest();
    var digestASN1 = asn1.create(asn1.Class.UNIVERSAL, asn1.Type.OCTETSTRING, false, digest.bytes());
    var timeStampRequestASN1 =  asn1.create(asn1.Class.UNIVERSAL, asn1.Type.SEQUENCE, true, [
        asn1.create(asn1.Class.UNIVERSAL, asn1.Type.INTEGER, false, asn1.integerToDer("1").getBytes()),
        asn1.create(asn1.Class.UNIVERSAL, asn1.Type.SEQUENCE, true, [
            asn1.create(asn1.Class.UNIVERSAL, asn1.Type.SEQUENCE, true, [
                asn1.create(asn1.Class.UNIVERSAL, asn1.Type.OID, false, asn1.oidToDer(forge.pki.oids.sha256).getBytes()),
                asn1.create(asn1.Class.UNIVERSAL, asn1.Type.NULL, false, '')
            ]),
            digestASN1
        ]),
        asn1.create(asn1.Class.UNIVERSAL, asn1.Type.INTEGER, false, asn1.integerToDer(
            Math.floor(Math.random() * (2000000000 - 0))).getBytes())
    ]);
    var timeStampRequestDer = forge.asn1.toDer(timeStampRequestASN1);
    var timeStampRequestBase64 = forge.util.encode64(timeStampRequestDer.getBytes());

    vs.httpPost(vs.timeStampServiceURL, function(responseText, status) {
            if(status == 200) {
                var timeStampTokenDer = forge.util.decode64(responseText);
                var timeStampTokenASN1 = forge.asn1.fromDer(timeStampTokenDer);
                callback(timeStampTokenASN1);
            } else console.log("status: " + status + " - responseText: " + responseText);
        }, timeStampRequestBase64, "application/timestamp-query", {"Content-Encoding":"base64"});

}