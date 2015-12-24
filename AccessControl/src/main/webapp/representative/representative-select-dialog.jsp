<%@ page contentType="text/html; charset=UTF-8" %>

<link href="representative-select-anonymous-dialog.vsp" rel="import"/>
<link href="representative-select-public-dialog.vsp" rel="import"/>
<link href="../resources/bower_components/paper-dialog-scrollable/paper-dialog-scrollable.html" rel="import"/>


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
                <p>${msg.delegationIntroLbl}</p>
                <div style="font-size:1.2em;font-weight: bold;">${msg.anonymousDelegationLbl}</div>
                <div>${msg.anonymousDelegationMsg}</div>
                <div >
                    <div hidden="{{showingDetails}}" class="linkVS" on-click="toggleDetails">
                        <div>${msg.showAnoymousDelegationDetailsMsg}</div>
                    </div>
                    <div hidden="{{!showingDetails}}">
                        <div class="linkVS" on-click="toggleDetails" style="margin: 0 0 0 40px;" >${msg.hideDetailsMsg}</div>
                        <div>${msg.anonymousDelegationMoreMsg}</div>
                    </div>
                </div>

                <div style="margin:20px 0 0 0;font-size:1.2em;font-weight: bold;">${msg.publicDelegationLbl}</div>
                <div>${msg.publicDelegationMsg}</div>

                <div class="horizontal layout center center-justified" style="margin:15px 0 0 40px;">
                    <div style="margin:0 20px 0 0;">
                        <button on-click="anonymousDelegation">${msg.anonymousDelegationLbl}</button>
                    </div>
                    <div>
                        <button on-click="publicDelegation">${msg.publicDelegationLbl}</button>
                    </div>
                </div>
            </div>
        </div>
        <representative-select-anonymous-dialog id="anonymousDialog"></representative-select-anonymous-dialog>
        <representative-select-public-dialog  id="publicDialog"></representative-select-public-dialog>
    </template>
    <script>
        Polymer({
            is:'representative-select-dialog',
            properties: {
                representative:{type:Object}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            toggleDetails:function() {
                this.showingDetails = !this.showingDetails
            },
            show: function(representative) {
                this.representative = representative
                this.showingDetails = false
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            publicDelegation:function() {
                this.$.publicDialog.show(this.representative)
            },
            anonymousDelegation:function() {
                this.$.anonymousDialog.show(this.representative)
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            }
        });
    </script>
</dom-module>
