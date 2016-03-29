<%@ page contentType="text/html; charset=UTF-8" %>

<link href="qr-dialog.vsp" rel="import"/>

<dom-module name="vs-socket">
    <template>
        <qr-dialog id="qrDialog"></qr-dialog>
    </template>
    <script>
        Polymer({
            is:'vs-socket',
            properties:{
                qrOperationsMap:{type:Object, value:{}},
                devId:{type:String},
                DEVICE_ID_KEY:{type:String, value:"did"},
                OPERATION_KEY:{type:String, value:"op"},
                OPERATION_CODE_KEY:{type:String, value:"opid"},
                SERVER_KEY:{type:String, value:"srv"},
                PUBLIC_KEY_KEY:{type:String, value:"pk"},
                OPERATION_PROCESS:{type:Number, value:5},
                CURRENCY_SYSTEM:{type:Number, value:0},
                VOTING_SYSTEM:{type:Number, value:1},
            },
            ready: function() {},
            connect: function() {
                if(this.websocket) {
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
                    console.log(this.tagName + '- WebSocket connection opened.');
                };

                this.websocket.onmessage = function (event) {
                    console.log(this.tagName + ' - Received: ' + event.data);
                    var messageJSON = toJSON(event.data)
                    if("MESSAGEVS_FROM_VS" ===  messageJSON.operation) {
                        messageJSON.operation = messageJSON.messageType
                        switch(messageJSON.operation) {
                            case "INIT_SESSION":
                                this.devId = messageJSON.deviceToId
                                if(this.pendingOperation) {
                                    this.showOperationQRCode(this.pendingOperation.system, this.pendingOperation.operation)
                                    this.pendingOperation = null;
                                }
                                break;
                            default:
                                if(messageJSON.statusCode === ResponseVS.SC_WS_CONNECTION_NOT_FOUND) {
                                    alert(messageJSON.message, "${msg.errorLbl}")
                                }
                        }

                    } else {
                        switch(messageJSON.messageType) {
                            case "OPERATION_PROCESS":
                                this.sendOperation(messageJSON, this.qrOperationsMap[messageJSON.operationCode])
                                break;
                            case "OPERATION_RESULT":
                                this.fire('socket-message', messageJSON);
                                break;
                        }
                    }
                }.bind(this);
                this.websocket.onclose = function (event) {
                    console.log('Info: WebSocket connection closed, Code: ' + event.code +
                            (event.reason == "" ? "" : ", Reason: " + event.reason));
                };
                vs.socket = this
            },
            showOperationQRCode:function (socketSystem, operationVS) {
                if(!this.devId) {
                    this.pendingOperation = {system:socketSystem, operation:operationVS}
                    return;
                }
                var operationCode = this.getOperationCode()
                this.qrOperationsMap[operationCode] = operationVS
                this.$.qrDialog.show(operationCode, this.getQRCodeURL(vs.socket.OPERATION_PROCESS, operationCode,
                        this.devId, null, "200x200", socketSystem))
                return operationCode
            },
            sendOperation: function(socketMessage, operation) {
                var socketMessage = {operation:"MSG_TO_DEVICE", messageType:"OPERATION_PROCESS",
                    deviceFromId:this.devId, deviceToId:socketMessage.deviceFromId,
                    operationCode:socketMessage.operationCode,
                    message:JSON.stringify(operation), uuid:socketMessage.uuid}
                console.log(this.tagName + " - socketMessage: ", socketMessage)
                this.websocket.send(JSON.stringify(socketMessage))
            },
            getOperationCode: function() {
                return Math.random().toString(36).substring(2, 6).toUpperCase();
            },
            getUUID: function() {
                //http://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript?rq=1
                return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                    var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
                    return v.toString(16);
                });
            },
            getQRCodeURL:function(operation, operationCode, deviceId, key, size, server) {
                if(!size) size = "100x100"
                if(!server) server = this.CURRENCY_SYSTEM
                var result = vs.contextURL + "/qr?cht=qr&chs=" + size + "&chl=" + this.SERVER_KEY + "=" + server + ";"
                if(operation != null) result = result + this.OPERATION_KEY + "=" + operation + ";"
                if(operationCode != null) result = result + this.OPERATION_CODE_KEY + "=" + operationCode + ";"
                if(deviceId != null) result = result + this.DEVICE_ID_KEY + "=" + deviceId + ";"
                if(key != null) result = result + this.PUBLIC_KEY_KEY + "=" + key + ";"
                return result;
            }
        });
    </script>
</dom-module>
