<%@ page contentType="text/html; charset=UTF-8" %>

<link href="qr-dialog.vsp" rel="import"/>
<link href="../resources/forgePKCS7.html" rel="import"/>

<dom-module name="vs-socket">
    <template>
        <qr-dialog id="qrDialog"></qr-dialog>
    </template>
    <script>
        Polymer({
            is:'vs-socket',
            properties:{
                qrOperationsMap:{type:Object, value:{}}
            },
            ready: function() {
                console.log("ready")
            },
            connect: function() {
                if(vs.connectedDevice) {
                    console.log("socket already connected")
                    return;
                }
                if ('WebSocket' in window) {
                    this.websocket = new WebSocket("${webSocketURL}");
                } else {
                    alert('WebSocket is not supported by this browser.');
                    return;
                }
                this.websocket.onopen = function () {
                    console.log(this.tagName + '- websocket.onopen');
                }.bind(this);
                this.websocket.onmessage = function (event) {
                    var messageJSON = toJSON(event.data)
                    console.log(this.tagName + ' - websocket.onmessage: ', messageJSON);
                    if("MESSAGEVS_FROM_VS" ===  messageJSON.operation) {
                        messageJSON.operation = messageJSON.messageType
                        switch(messageJSON.operation) {
                            case "INIT_SESSION":
                                vs.connectedDevice = messageJSON.connectedDevice
                                if(this.pendingOperation) {
                                    this.showOperationQRCode(this.pendingOperation.system, this.pendingOperation.operation,
                                            this.pendingOperation.operationCode)
                                    this.pendingOperation = null;
                                }
                                break;
                            case 'INIT_REMOTE_SIGNED_SESSION':
                                if(ResponseVS.SC_WS_CONNECTION_INIT_OK == messageJSON.statusCode) {
                                    vs.rsaUtil.initCSR(messageJSON.connectedDevice.x509CertificatePEM);
                                    var sessionData = vs.encryptAES(JSON.stringify({connectedDevice: messageJSON.connectedDevice,
                                        privateKeyPEM:vs.rsaUtil.privateKeyPEM,
                                        x509CertificatePEM:vs.rsaUtil.x509CertificatePEM}), vs.aesParams)
                                    localStorage.setItem('sessionData', sessionData);
                                    this.$.qrDialog.close();
                                    vs.setConnected(messageJSON.connectedDevice, toJSON(messageJSON.message));
                                }
                                break;
                            case 'RENEW_SESSION':

                                break;
                            default:
                                if(messageJSON.statusCode === ResponseVS.SC_WS_CONNECTION_NOT_FOUND) {
                                    alert(messageJSON.message, "${msg.errorLbl}")
                                }
                        }
                    } else {
                        if(vs.rsaUtil) vs.rsaUtil.decryptSocketMsg(messageJSON)
                        else this.rsaUtil.decryptSocketMsg(messageJSON)
                        console.log("decryptedSocketMsg: ", messageJSON)
                        switch(messageJSON.messageType) {
                            case "OPERATION_PROCESS":
                                this.sendOperation(messageJSON, this.qrOperationsMap[messageJSON.operationCode])
                                break;
                            case "OPERATION_RESULT":
                                document.querySelector("#voting_system_page").dispatchEvent(
                                        new CustomEvent('socket-message', {detail:messageJSON}))
                                var operation = this.qrOperationsMap[messageJSON.operationCode]
                                if(operation.callback) operation.callback(messageJSON)
                                break;
                            case "SEND_AES_PARAMS":
                                vs.aesParams = {key:forge.util.decode64(messageJSON.aesParams.key),
                                    iv:forge.util.decode64(messageJSON.aesParams.iv),
                                    iv_id:messageJSON.aesParams.iv.substring(0, 4)}
                                document.querySelector("#voting_system_page").dispatchEvent(
                                        new CustomEvent('SEND_AES_PARAMS'))
                                break;
                            case 'INIT_REMOTE_SIGNED_SESSION':
                                //we receive the user data from the mobile and prepare a CSR that is sended to the mobile.
                                //The mobile signs a request with the CSR and sends it to the server.
                                vs.aesParams = {key:forge.util.decode64(messageJSON.aesParams.key),
                                        iv:forge.util.decode64(messageJSON.aesParams.iv),
                                        iv_id:messageJSON.aesParams.iv.substring(0, 4)}
                                var qrOperation = this.qrOperationsMap[messageJSON.operationCode]
                                if(qrOperation.operation !== 'INIT_REMOTE_SIGNED_SESSION') {
                                    throw new Error('INIT_REMOTE_SIGNED_SESSION - qrOperation ERROR');
                                }
                                var socketMessageDto = {"operation":"MSG_TO_DEVICE",
                                    statusCode:700,
                                    deviceToId:messageJSON.deviceFromId,
                                    deviceFromId: vs.connectedDevice.id}
                                var userDataFromCert = vs.extractUserInfoFromCert(messageJSON.x509CertificatePEM)
                                var csr = vs.rsaUtil.getCSR(userDataFromCert)
                                var remoteSignedSessionDto = {csr:csr, deviceId:vs.connectedDevice.id}
                                var encryptedDto = {operation:'INIT_REMOTE_SIGNED_SESSION',
                                    operationCode:messageJSON.operationCode, deviceFromId: vs.wsId,
                                    message:JSON.stringify(remoteSignedSessionDto),
                                    uuid:messageJSON.uuid}
                                socketMessageDto.encryptedMessage = vs.encryptToCMS(
                                        messageJSON.x509CertificatePEM, encryptedDto)
                                socketMessageDto.uuid = messageJSON.uuid
                                console.log("INIT_REMOTE_SIGNED_SESSION response: ", socketMessageDto)
                                this.websocket.send(JSON.stringify(socketMessageDto))
                                //delete this.qrOperationsMap[messageJSON.operationCode];
                                break;
                        }
                    }
                }.bind(this);
                this.websocket.onclose = function (event) {
                    console.log('Info: WebSocket connection closed, Code: ' + event.code +
                            (event.reason == "" ? "" : ", Reason: " + event.reason));
                    vs.setDisConnected()
                }.bind(this);
            },
            disconnect:function () {
                var socketMessageDto = {operation:"CLOSE_SESSION", uuid:vs.getUUID()}
                function signCallback(cmsSignedMessage) {
                    socketMessageDto.cmsMessagePEM = forge.pkcs7.messageToPem(cmsSignedMessage);
                    this.websocket.send(JSON.stringify(socketMessageDto))
                }
                vs.rsaUtil.sign(JSON.stringify(socketMessageDto), signCallback.bind(this))
            },
            closeQRDialog:function (socketSystem, operationVS, operationCode) {
                this.$.qrDialog.close()
            },
            showOperationQRCode:function (socketSystem, operationVS, operationCode) {
                var publicKeyBase64;
                if(!operationCode) {
                    operationCode = this.getOperationCode()
                    this.qrOperationsMap[operationCode] = operationVS
                }
                if(!vs.connectedDevice) {
                    this.pendingOperation = {system:socketSystem, operation:operationVS, operationCode:operationCode}
                    this.async(function () {
                        this.$.qrDialog.showProcessing(operationCode, null)
                    }.bind(this))
                    return operationCode;
                }
                if(vs.QROperationCode.GET_AES_PARAMS === operationVS.qrOperationCode) {
                    this.rsaUtil = new RSAUtil(1024);
                    publicKeyBase64 = this.rsaUtil.publicKeyBase64
                }
                if(vs.QROperationCode.INIT_REMOTE_SIGNED_SESSION === operationVS.qrOperationCode) {
                    if(!vs.rsaUtil) vs.rsaUtil = new RSAUtil(1024);
                    publicKeyBase64 = vs.rsaUtil.publicKeyBase64
                }
                var qrOperationCode = operationVS.qrOperationCode != null?
                        operationVS.qrOperationCode : vs.QROperationCode.OPERATION_PROCESS;
                this.$.qrDialog.show(operationCode, vs.getQRCodeURL(qrOperationCode, operationCode, vs.connectedDevice.id,
                        publicKeyBase64, "200x200", socketSystem, operationVS.msg), operationVS.caption)
                return operationCode
            },
            sendOperation: function(socketMessage, operation) {
                operation.uuid = vs.getUUID()
                if(operation.jsonStr) {
                    var jsonData = toJSON(operation.jsonStr)
                    jsonData.uuid =  vs.getUUID()
                    operation.jsonStr = JSON.stringify(jsonData)
                }
                var socketMessage = {operation:"MSG_TO_DEVICE", messageType:"OPERATION_PROCESS",
                    deviceFromId:vs.connectedDevice.id, deviceToId:socketMessage.deviceFromId,
                    operationCode:socketMessage.operationCode,
                    message:JSON.stringify(operation), uuid:socketMessage.uuid}
                console.log(this.tagName + " - sendOperation: ", socketMessage)
                this.websocket.send(JSON.stringify(socketMessage))
            },
            getOperationCode: function() {
                return Math.random().toString(36).substring(2, 6).toUpperCase();
            }
        });
    </script>
</dom-module>
