<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="representative-select-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div>
                <div class="layout horizontal center center-justified">
                    <div flex style="font-size: 1.4em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">${msg.saveAsRepresentativeLbl}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div style="font-size:1.2em;font-weight: bold;margin: 15px 0 0 0;">${msg.anonymousDelegationLbl}</div>
                <div>${msg.anonymousDelegationMsg}</div>
                <div>${msg.anonymousDelegationMoreMsg}</div>
                <div style="margin: 10px auto; text-align: center;">
                    <img id="qrImg"  src="" style="border: 1px solid #ccc;"/>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'representative-select-dialog',
            properties: {
                representative:{type:Object}
            },
            ready: function() { },
            show: function(representative) {
                this.representative = representative
                this.showingDetails = false
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
                var anonymousRepresentativeSelectionOperationCode = 6;
                var qrCodeURL = vs.contextURL + "/qr?cht=qr&chs=150x150&chl=op=" +
                        anonymousRepresentativeSelectionOperationCode + ";iid=" + representative.id
                this.$.qrImg.setAttribute("src", qrCodeURL)
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
