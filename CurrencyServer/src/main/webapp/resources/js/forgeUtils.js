forge.oids['2.5.4.42'] = "givenName"
forge.oids["givenName"] = '2.5.4.42'
forge.oids['2.5.4.4'] = "surname"
forge.oids["surname"] = '2.5.4.4'

var RSAUtil = function() {
    var keyLength = 1024
    this.rsa = forge.pki.rsa;
    // generate an RSA key pair synchronously
    console.log("RSAUtil - keyLength: " + keyLength)
    this.keypair = this.rsa.generateKeyPair(keyLength);
    this.publicKeyPEM = forge.pki.publicKeyToPem(this.keypair.publicKey),
    this.privateKeyPEM = forge.pki.privateKeyToPem(this.keypair.privateKey);
    var derBytes = forge.asn1.toDer(forge.pki.publicKeyToAsn1(this.keypair.publicKey)).getBytes()
    this.publicKeyBase64 =  forge.util.encode64(derBytes);
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
    this.csr.setSubject([
        { name: 'serialName', value: userData.serialName},
        { name: 'surname', value: userData.surname },
        { name: 'givenName', value: userData.givenName }]);
    this.csr.sign(this.keypair.privateKey);
    var verified = this.csr.verify();
    return forge.pki.certificationRequestToPem(this.csr);
}

RSAUtil.prototype.initCSR = function(issuedX509CertificatePEM) {
    this.x509CertificatePEM = issuedX509CertificatePEM
    this.x509Certificate = forge.pki.certificateFromPem(issuedX509CertificatePEM);
}

RSAUtil.prototype.sign = function(contentToSign, callback) {
    vs.getTimeStampToken(contentToSign, function (timeStampTokenASN1) {
        console.log("timeStampTokenASN1:")
        console.log(timeStampTokenASN1)
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
        callback(cmsSignedMessage);
    }.bind(this))
}

RSAUtil.prototype.decryptSocketMsg = function(messageJSON) {
    var decryptedMsg = this.decryptCMS(messageJSON.encryptedMessage)
    console.log("decryptedMsg: ")
    console.log(decryptedMsg.content.data)
    if(decryptedMsg.content && decryptedMsg.content.data) {
        var encryptedContentJSON = toJSON(decryptedMsg.content.data)
        messageJSON.messageType = messageJSON.operation
        messageJSON.operation = encryptedContentJSON.operation

        if(encryptedContentJSON.operationCode != null) messageJSON.operationCode = encryptedContentJSON.operationCode;
        if(encryptedContentJSON.statusCode != null) messageJSON.statusCode = encryptedContentJSON.statusCode;
        if(encryptedContentJSON.step != null) messageJSON.step = encryptedContentJSON.step;
        if(encryptedContentJSON.x509CertificatePEM != null) messageJSON.x509CertificatePEM = encryptedContentJSON.x509CertificatePEM;
        if(encryptedContentJSON.publicKeyPEM != null) messageJSON.publicKeyPEM = encryptedContentJSON.publicKeyPEM;
        if(encryptedContentJSON.deviceFromName != null) messageJSON.deviceFromName = encryptedContentJSON.deviceFromName;
        if(encryptedContentJSON.from != null) messageJSON.from = encryptedContentJSON.from;
        if(encryptedContentJSON.deviceFromId != null) messageJSON.deviceFromId = encryptedContentJSON.deviceFromId;
        if(encryptedContentJSON.cmsMessage != null) messageJSON.cms = encryptedContentJSON.cmsMessage;
        if(encryptedContentJSON.subject != null) messageJSON.subject = encryptedContentJSON.subject;
        if(encryptedContentJSON.message != null) messageJSON.message = encryptedContentJSON.message;
        if(encryptedContentJSON.toUser != null) messageJSON.toUser = encryptedContentJSON.toUser;
        if(encryptedContentJSON.deviceToName != null) messageJSON.deviceToName = encryptedContentJSON.deviceToName;
        if(encryptedContentJSON.URL != null) messageJSON.URL = encryptedContentJSON.URL;
        if(encryptedContentJSON.uuid != null) messageJSON.uuid = encryptedContentJSON.uuid;
        if(encryptedContentJSON.locale != null) messageJSON.locale = encryptedContentJSON.locale;
        messageJSON.encryptedMessage = null;
    } else console.error("encrypted content not found")
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
            Math.floor(Math.random() * (2000000000  - 0))).getBytes())
    ]);
    var timeStampRequestDer = forge.asn1.toDer(timeStampRequestASN1);
    var timeStampRequestBase64 = forge.util.encode64(timeStampRequestDer.getBytes());
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (xhttp.readyState == 4 && xhttp.status == 200) {
            var timeStampTokenDer = forge.util.decode64(xhttp.responseText);
            var timeStampTokenASN1 = forge.asn1.fromDer(timeStampTokenDer)
            callback(timeStampTokenASN1)
        }
    };
    xhttp.open("POST",  vs.timeStampServiceURL , true);
    xhttp.setRequestHeader("Content-type", "application/timestamp-query");
    xhttp.setRequestHeader("Content-Encoding", "base64");
    xhttp.send(timeStampRequestBase64);
}

//http://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript?rq=1
vs.getUUID = function() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
        return v.toString(16);
    });
}

vs.getQRCodeURL = function(operation, operationCode, deviceId, sessionId, key, size) {
    if(!size) size = "100x100"
    var result = vs.contextURL + "/qr?cht=qr&chs=" + size + "&chl="
    if(operation != null) result = result + OPERATION_KEY + "=" + operation + ";"
    if(operationCode != null) result = result + OPERATION_CODE_KEY + "=" + operationCode + ";"
    if(deviceId != null) result = result + DEVICE_ID_KEY + "=" + deviceId + ";"
    if(sessionId != null) result = result + WEB_SOCKET_SESSION_KEY + "=" + sessionId + ";"
    if(key != null) result = result + PUBLIC_KEY_KEY + "=" + key + ";"
    return result;
}