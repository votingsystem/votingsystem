//based on https://github.com/mpetersen/aes-example
var AesUtil = function() {
  this.keySize = 256 / 32;
  this.iterationCount = 1000;
  this.passphrase = Math.random().toString(36).substring(2, 10);
  this.salt = CryptoJS.lib.WordArray.random(128/8).toString(CryptoJS.enc.Base64);
  this.iv = CryptoJS.lib.WordArray.random(128/8).toString(CryptoJS.enc.Base64);
  this.key = this.generateKey(this.salt, this.passphrase)
  this.keyBase64 = this.key.toString(CryptoJS.enc.Base64)
};


AesUtil.prototype.generateKey = function(salt, passPhrase) {
  var key = CryptoJS.PBKDF2(passPhrase, CryptoJS.enc.Base64.parse(salt),
      { keySize: this.keySize, iterations: this.iterationCount });
  return key;
}

AesUtil.prototype.encrypt = function(plainText) {
  var encrypted = CryptoJS.AES.encrypt(plainText, this.key, { iv: CryptoJS.enc.Base64.parse(this.iv) });
  return encrypted.ciphertext.toString(CryptoJS.enc.Base64);
}

AesUtil.prototype.decrypt = function(cipherText) {
  var cipherParams = CryptoJS.lib.CipherParams.create({ ciphertext: CryptoJS.enc.Base64.parse(cipherText) });
  var decrypted = CryptoJS.AES.decrypt( cipherParams, this.key, { iv: CryptoJS.enc.Base64.parse(this.iv) });
  return decrypted.toString(CryptoJS.enc.Utf8);
}

AesUtil.prototype.decryptSocketMsg = function(messageJSON) {
    var decryptedMsg = this.decrypt(messageJSON.encryptedMessage)
    var msgContentJSON = toJSON(decryptedMsg)
    messageJSON.messageType = messageJSON.operation
    messageJSON.operation = msgContentJSON.operation
  
    if(msgContentJSON.statusCode != null) messageJSON.statusCode = msgContentJSON.statusCode;
    if(msgContentJSON.deviceFromName != null) deviceFromName = msgContentJSON.deviceFromName;
    if(msgContentJSON.from != null) from = msgContentJSON.from;
    if(msgContentJSON.deviceFromId != null) deviceFromId = msgContentJSON.deviceFromId;
    if(msgContentJSON.smimeMessage != null) smime = msgContentJSON.smimeMessage;
    if(msgContentJSON.subject != null) subject = msgContentJSON.subject;
    if(msgContentJSON.message != null) message = msgContentJSON.message;
    if(msgContentJSON.toUser != null) toUser = msgContentJSON.toUser;
    if(msgContentJSON.deviceToName != null) deviceToName = msgContentJSON.deviceToName;
    if(msgContentJSON.URL != null) URL = msgContentJSON.URL;
    if(msgContentJSON.locale != null) locale = msgContentJSON.locale;
    this.encryptedMessage = null;
}