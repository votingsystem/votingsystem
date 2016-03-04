var RSAUtil = function() {
    this.rsa = forge.pki.rsa;
    // generate an RSA key pair synchronously
    this.keypair = this.rsa.generateKeyPair({bits: 1024, e: 0x10001});
    this.pemPublic = forge.pki.publicKeyToPem(this.keypair.publicKey),
    this.pemPrivate = forge.pki.privateKeyToPem(this.keypair.privateKey);
    this.publicKeyBase64 =  forge.util.encode64(this.pemPublic);
}

RSAUtil.prototype.encrypt = function(plainText) {
    var encrypted = this.keypair.publicKey.encrypt(plainText, 'RSAES-PKCS1-V1_5');
    return forge.util.encode64(encrypted);
}

RSAUtil.prototype.decrypt = function(encryptedBase64) {
    var encrypted = forge.util.decode64(encryptedBase64)
    return this.keypair.privateKey.decrypt(encrypted, 'RSAES-PKCS1-V1_5');
}


var AESUtil = function() { }

AESUtil.prototype.init = function() {
    // Note: a key size of 16 bytes will use AES-128, 24 => AES-192, 32 => AES-256
    this.key = forge.random.getBytesSync(32);
    this.iv = forge.random.getBytesSync(16);
    this.keyBase64 = forge.util.encode64(this.key)
    this.ivBase64 = forge.util.encode64(this.iv)
    return this;
}

AESUtil.prototype.loadData = function(keyBase64, ivBase64) {
    this.key = forge.util.decode64(keyBase64)
    this.iv = forge.util.decode64(ivBase64)
    this.keyBase64 = keyBase64
    this.ivBase64 = ivBase64
    return this;
}

AESUtil.prototype.encrypt = function(plainText) {
    var cipher = forge.cipher.createCipher('AES-CBC', this.key);
    cipher.start({iv: this.iv});
    cipher.update(forge.util.createBuffer(plainText));
    cipher.finish();
    return forge.util.encode64(cipher.output.data)
}

AESUtil.prototype.decrypt = function(encryptedBase64) {
    var textToDecrypt = forge.util.decode64(encryptedBase64)
    var decipher = forge.cipher.createDecipher('AES-CBC', this.key);
    decipher.start({iv: this.iv});
    decipher.update(forge.util.createBuffer(textToDecrypt));
    decipher.finish();
    return decipher.output.data;
}

AESUtil.prototype.decryptSocketMsg = function(messageJSON) {
    var decryptedMsg = this.decrypt(messageJSON.encryptedMessage)
    var msgContentJSON = toJSON(decryptedMsg)
    messageJSON.messageType = messageJSON.operation
    messageJSON.operation = msgContentJSON.operation

    if(msgContentJSON.statusCode != null) messageJSON.statusCode = msgContentJSON.statusCode;
    if(msgContentJSON.deviceFromName != null) messageJSON.deviceFromName = msgContentJSON.deviceFromName;
    if(msgContentJSON.from != null) messageJSON.from = msgContentJSON.from;
    if(msgContentJSON.deviceFromId != null) messageJSON.deviceFromId = msgContentJSON.deviceFromId;
    if(msgContentJSON.smimeMessage != null) messageJSON.smime = msgContentJSON.smimeMessage;
    if(msgContentJSON.subject != null) messageJSON.subject = msgContentJSON.subject;
    if(msgContentJSON.message != null) messageJSON.message = msgContentJSON.message;
    if(msgContentJSON.toUser != null) messageJSON.toUser = msgContentJSON.toUser;
    if(msgContentJSON.deviceToName != null) messageJSON.deviceToName = msgContentJSON.deviceToName;
    if(msgContentJSON.URL != null) messageJSON.URL = msgContentJSON.URL;
    if(msgContentJSON.locale != null) messageJSON.locale = msgContentJSON.locale;
    messageJSON.encryptedMessage = null;
}