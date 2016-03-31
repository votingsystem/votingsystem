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
                //websocket system
                CURRENCY_SYSTEM:{type:Number, value:0},
                VOTING_SYSTEM:{type:Number, value:1},
                //param keys
                DEVICE_ID_KEY:{type:String, value:"did"},
                ITEM_ID_KEY:{type:String, value:"iid"},
                OPERATION_KEY:{type:String, value:"op"},
                OPERATION_CODE_KEY:{type:String, value:"opid"},
                PUBLIC_KEY_KEY:{type:String, value:"pk"},
                SERVER_KEY:{type:String, value:"srv"},
                //operations
                INIT_REMOTE_SIGNED_SESSION:{type:Number, value:0},
                MESSAGE_INFO:{type:Number, value:1},
                CURRENCY_SEND:{type:Number, value:2},
                USER_INFO:{type:Number, value:3},
                VOTE:{type:Number, value:4},
                OPERATION_PROCESS:{type:Number, value:5},
                ANONYMOUS_REPRESENTATIVE_SELECTION:{type:Number, value:6},
            },
            ready: function() {},
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
                    console.log(this.tagName + '- WebSocket connection opened.');
                };
                this.websocket.onmessage = function (event) {
                    console.log(this.tagName + ' - Received: ' + event.data);
                    var messageJSON = toJSON(event.data)
                    if("MESSAGEVS_FROM_VS" ===  messageJSON.operation) {
                        messageJSON.operation = messageJSON.messageType
                        switch(messageJSON.operation) {
                            case "INIT_SESSION":
                                vs.deviceId = messageJSON.deviceToId
                                if(this.pendingOperation) {
                                    this.showOperationQRCode(this.pendingOperation.system, this.pendingOperation.operation,
                                            this.pendingOperation.operationCode)
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
                                var operation = this.qrOperationsMap[messageJSON.operationCode]
                                if(operation.callback) operation.callback(messageJSON)
                                break;
                        }
                    }
                }.bind(this);
                this.websocket.onclose = function (event) {
                    vs.deviceId = null
                    console.log('Info: WebSocket connection closed, Code: ' + event.code +
                            (event.reason == "" ? "" : ", Reason: " + event.reason));
                };
            },
            closeQRDialog:function (socketSystem, operationVS, operationCode) {
                this.$.qrDialog.close()
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
                this.$.qrDialog.show(operationCode, this.getQRCodeURL(this.OPERATION_PROCESS, operationCode,
                        vs.deviceId, null, "200x200", socketSystem))
                return operationCode
            },
            sendOperation: function(socketMessage, operation) {
                operation.uuid = vs.getUUID()
                if(operation.jsonStr) {
                    var jsonData = toJSON(operation.jsonStr)
                    jsonData.UUID =  vs.getUUID()
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
