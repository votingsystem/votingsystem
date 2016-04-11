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
                if(vs.deviceId) {
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
                                vs.deviceId = messageJSON.deviceToId
                                if(this.pendingOperation) {
                                    this.showOperationQRCode(this.pendingOperation.system, this.pendingOperation.operation,
                                            this.pendingOperation.operationCode)
                                    this.pendingOperation = null;
                                } else if(this.pendingAccessRequest) {
                                    this.showAccessQRCode()
                                    this.pendingAccessRequest = null
                                }
                                break;
                            case 'INIT_REMOTE_SIGNED_SESSION':
                                if(ResponseVS.SC_WS_CONNECTION_INIT_OK == messageJSON.statusCode) {
                                    var aesParams = {key:forge.util.decode64(messageJSON.connectedDevice.aesParams.key),
                                        iv:forge.util.decode64(messageJSON.connectedDevice.aesParams.iv)}
                                    vs.rsaUtil.initCSR(messageJSON.connectedDevice.x509CertificatePEM, aesParams);
                                    this.$.qrDialog.close();
                                    vs.setConnected(messageJSON.connectedDevice, toJSON(messageJSON.message));
                                }
                                break;
                            default:
                                if(messageJSON.statusCode === ResponseVS.SC_WS_CONNECTION_NOT_FOUND) {
                                    alert(messageJSON.message, "${msg.errorLbl}")
                                }
                        }

                    } else {
                        vs.rsaUtil.decryptSocketMsg(messageJSON)
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
                            case 'INIT_REMOTE_SIGNED_SESSION':
                                //we receive the user data from the mobile and prepare a CSR that is sended to the mobile.
                                //The mobile signs that CSR and sends it to the server.
                                var qrOperation = this.qrOperationsMap[messageJSON.operationCode]
                                if(qrOperation.operation !== 'INIT_REMOTE_SIGNED_SESSION') {
                                    throw new Error('INIT_REMOTE_SIGNED_SESSION - qrOperation ERROR');
                                }
                                var socketMessageDto = {"operation":"MSG_TO_DEVICE",
                                    statusCode:700,
                                    deviceToId:messageJSON.deviceFromId,
                                    deviceFromId: vs.deviceId}
                                var userDataFromCert = vs.extractUserInfoFromCert(messageJSON.x509CertificatePEM)
                                var csr = vs.rsaUtil.getCSR(userDataFromCert)
                                var remoteSignedSessionDto = {csr:csr, deviceId:vs.deviceId}
                                var encryptedDto = {operation:'INIT_REMOTE_SIGNED_SESSION',
                                    operationCode:messageJSON.operationCode, deviceFromId: vs.wsId,
                                    message:JSON.stringify(remoteSignedSessionDto),
                                    uuid:messageJSON.uuid}
                                socketMessageDto.encryptedMessage = vs.encryptToCMS(
                                        messageJSON.x509CertificatePEM, encryptedDto)
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
            showAccessQRCode:function () {
                var operationCode = this.getOperationCode()
                if(!vs.deviceId) {
                    this.pendingAccessRequest = true
                    this.async(function () {
                        this.$.qrDialog.showProcessing(operationCode, null)
                    }.bind(this))
                    return;
                }
                if(!vs.rsaUtil) vs.rsaUtil = new RSAUtil(1024);
                this.qrOperationsMap[operationCode] = {operation:'INIT_REMOTE_SIGNED_SESSION'}
                this.$.qrDialog.show(operationCode, vs.getQRCodeURL(vs.QROperationCode.INIT_REMOTE_SIGNED_SESSION, operationCode,
                        vs.deviceId, vs.rsaUtil.publicKeyBase64, "200x200"))
            },
            showOperationQRCode:function (socketSystem, operationVS, operationCode) {
                if(!operationCode) {
                    operationCode = this.getOperationCode()
                    this.qrOperationsMap[operationCode] = operationVS
                }
                if(!vs.deviceId) {
                    this.pendingOperation = {system:socketSystem, operation:operationVS, operationCode:operationCode}
                    return operationCode;
                }
                this.$.qrDialog.show(operationCode, vs.getQRCodeURL(vs.QROperationCode.OPERATION_PROCESS, operationCode,
                        vs.deviceId, null, "200x200", socketSystem))
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
                    deviceFromId:vs.deviceId, deviceToId:socketMessage.deviceFromId,
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
