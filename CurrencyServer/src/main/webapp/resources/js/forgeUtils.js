var RSAUtil = function() {
    this.rsa = forge.pki.rsa;
    // generate an RSA key pair synchronously
    this.keypair = this.rsa.generateKeyPair({bits: 1024, e: 0x10001});
    this.pemPublicKey = forge.pki.publicKeyToPem(this.keypair.publicKey),
    this.pemPrivateKey = forge.pki.privateKeyToPem(this.keypair.privateKey);
    this.publicKeyBase64 =  forge.util.encode64(this.pemPublicKey);
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

RSAUtil.prototype.decryptSocketMsg = function(messageJSON) {
    var decryptedMsg = this.decryptCMS(messageJSON.encryptedMessage)
    console.log("decryptedMsg: ")
    console.log(decryptedMsg.content.data)
    if(decryptedMsg.content && decryptedMsg.content.data) {
        var msgContentJSON = toJSON(decryptedMsg.content.data)
        messageJSON.messageType = messageJSON.operation
        messageJSON.operation = msgContentJSON.operation

        if(msgContentJSON.statusCode != null) messageJSON.statusCode = msgContentJSON.statusCode;
        if(msgContentJSON.pemCert != null) messageJSON.pemCert = msgContentJSON.pemCert;
        if(msgContentJSON.deviceFromName != null) messageJSON.deviceFromName = msgContentJSON.deviceFromName;
        if(msgContentJSON.from != null) messageJSON.from = msgContentJSON.from;
        if(msgContentJSON.deviceFromId != null) messageJSON.deviceFromId = msgContentJSON.deviceFromId;
        if(msgContentJSON.cmsMessage != null) messageJSON.cms = msgContentJSON.cmsMessage;
        if(msgContentJSON.subject != null) messageJSON.subject = msgContentJSON.subject;
        if(msgContentJSON.message != null) messageJSON.message = msgContentJSON.message;
        if(msgContentJSON.toUser != null) messageJSON.toUser = msgContentJSON.toUser;
        if(msgContentJSON.deviceToName != null) messageJSON.deviceToName = msgContentJSON.deviceToName;
        if(msgContentJSON.URL != null) messageJSON.URL = msgContentJSON.URL;
        if(msgContentJSON.locale != null) messageJSON.locale = msgContentJSON.locale;
        messageJSON.encryptedMessage = null;
    } else console.error("encrypted content not found")
}

vs.encryptToCMS = function (receptorCertPEM, jsonToEncrypt) {
    var p7 = forge.pkcs7.createEnvelopedData();
    var cert = forge.pki.certificateFromPem(receptorCertPEM);
    p7.addRecipient(cert);
    jsonToEncrypt.pemPublicKeyKey = this.pemPublicKey
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
    xhttp.open("POST",  vs.timeStampServerURL + "/timestamp", true);
    xhttp.setRequestHeader("Content-type", "application/timestamp-query");
    xhttp.setRequestHeader("Content-Encoding", "base64");
    xhttp.send(timeStampRequestBase64);
}
