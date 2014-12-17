<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog-transition.html"/>
<vs:webcomponent path="/userVS/uservs-selector"/>


<polymer-element name="uservs-selector-dialog" attributes="groupVSId groupVSState">
    <template>
        <paper-dialog id="xDialog" layered backdrop class="uservsSearchDialog" title="<g:message code="userSearchLbl"/>"
                             on-core-overlay-open="{{onCoreOverlayOpen}}" style="overflow: auto;">
        <style no-shim>
            .uservsSearchDialog {  width: 700px;  min-height: 300px; padding: 10px 20px; }
        </style>
        <g:include view="/include/styles.gsp"/>
            <div id="main">
                <div>
                    <vs-user-box flex id="receptorBox" boxCaption="<g:message code="receptorLbl"/>"></vs-user-box>
                </div>
                <div>
                    <uservs-selector id="userVSSelector"></uservs-selector>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer('uservs-selector-dialog', {
            ready: function() {
                console.log(this.tagName + " - " + this.id + " - ready")
                if(document.querySelector("#coreSignals")) {
                    document.querySelector("#coreSignals").addEventListener('core-signal-user-clicked', function(e) {
                        if(!this.$.xDialog.opened) return;
                        console.log(this.tagName + " - user-clicked - closing dialog")
                        this.$.xDialog.opened = false
                    }.bind(this));
                }
            },
            show: function() {
                console.log(this.tagName + " - show")
                this.$.receptorBox.removeUsers()
                this.$.xDialog.opened = true
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</polymer-element>