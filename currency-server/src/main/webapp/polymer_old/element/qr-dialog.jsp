<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="qr-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div style="width: 300px;">
                <div class="layout horizontal center center-justified">
                    <div hidden="{{!caption}}" flex style="font-size: 1.4em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">{{caption}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div hidden="[[processing]]">
                    <div id="messageDiv" style="font-size: 1.2em; color:#888; font-weight: bold; text-align: center;
                padding:10px 20px 10px 20px; display:block;word-wrap:break-word;">
                        ${msg.readQRMsg}
                    </div>
                    <div class="vertical layout center center-justified">
                        <div id="operationCodeDiv" style="font-size: 1.2em; color:#6c0404; font-weight: bold;"></div>
                        <img id="qrImg" src=""/>
                    </div>
                </div>
                <div hidden="[[!processing]]" style="text-align: center; color:#6c0404;font-size: 1.3em;">
                    <div>${msg.waitMsg}</div>
                    <i class="fa fa-cog fa-spin" style="font-size: 1.5em;"></i>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'qr-dialog',
            ready: function() { },
            show: function(operationCode, qrCodeURL, caption) {
                console.log(this.tagName + " - qrCodeURL: " + qrCodeURL)
                this.processing = false
                this.$.operationCodeDiv.innerHTML = operationCode
                this.$.qrImg.src = qrCodeURL
                this.caption = caption
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            showProcessing: function() {
                this.processing = true
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