<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="currency-request-result-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                        <div hidden="{{!caption}}" style="text-align: center;">{{caption}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div id="messageDiv" style="font-size: 1.3em; color:#888; font-weight: bold; text-align: center;
                        padding:20px 20px 10px 20px; display:block;word-wrap:break-word;">
                </div>
                <div hidden="{{isStoredInWallet}}" class="horizontal layout flex" style="margin:10px 20px 0px 0px;">
                    <button on-click="saveToSecureWallet">
                        <i class="fa fa-money"></i> ${msg.saveToSecureWalletMsg}
                    </button>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'currency-request-result-dialog',
            ready: function() { },
            saveToSecureWallet: function() {
                var operationVS = new OperationVS(Operation.WALLET_SAVE)
                operationVS.setCallback(function(appMessage) { this.saveResponse(appMessage)}.bind(this))
                vs.client.processOperation(operationVS);
            },
            saveResponse:function(appMessageJSON) {
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    this.isStoredInWallet = true;
                } else {
                    alert(appMessageJSON.message, '${msg.errorLbl}')
                }
            },
            showMessage:function(caption, message) {
                this.caption = caption;
                this.$.messageDiv.innerHTML = message
                this.isStoredInWallet = false
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            }
        });
    </script>
</dom-module>