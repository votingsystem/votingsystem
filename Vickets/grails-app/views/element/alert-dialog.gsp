<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-html-echo', file: 'vs-html-echo.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-dialog', file: 'vs-dialog.html')}">


<polymer-element name="alert-dialog">
    <template>
        <g:include view="/include/styles.gsp"/>
        <vs-dialog id="xDialog" class="votingsystemMessageDialog" style="max-width: 600px;"
                 on-core-overlay-open="{{onCoreOverlayOpen}}" on-closed="{{close}}">
            <style no-shim></style>
            <div>
                <div style="font-size: 1.2em; color:#888; font-weight: bold; text-align: center; padding:10px 20px 10px 20px; display:block;word-wrap:break-word;">
                    <vs-html-echo html="{{message}}"></vs-html-echo>
                </div>
                <div layout horizontal style="margin:0px 20px 0px 0px;">
                    <div flex>
                        <input type="text" id="keyEnterListener" style="width: 0px;background-color:white; border: none;" autofocus/>
                    </div>
                    <div style="margin:10px 0px 10px 0px;display:{{isConfirmMessage?'block':'none'}};">
                        <paper-button raised on-click="{{accept}}" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-check"></i> <g:message code="acceptLbl"/>
                        </paper-button>
                    </div>
                </div>
            </div>
        </vs-dialog>
    </template>
    <script>
        Polymer('alert-dialog', {
            uriData:null,
            ready: function() {
                this.isConfirmMessage = this.isConfirmMessage || false
                console.log(this.tagName + " - ready")
                this.$.keyEnterListener.onkeypress = function(event){
                    if (event.keyCode == 13) this.close()
                }.bind(this)
            },
            onCoreOverlayOpen:function(e) { },
            setMessage: function(message, caption, callerId, isConfirmMessage) {
                this.reset()
                this.message = message
                this.$.xDialog.title = caption
                this.callerId = callerId
                this.isConfirmMessage = isConfirmMessage
                this.$.xDialog.opened = true
            },
            reset: function() {
                this.message = null
                this.callerId = null
                this.caption = null
                this.isConfirmMessage = false
            },
            sendAndroidURIMessage:function(encodedData) {
                this.uriData = "${createLink(controller:'app', action:'androidClient', absolute:'true')}?operationvs="+ encodedData
                this.isConfirmMessage = true
                this.message = "<g:message code='selectAndroidAppMsg'/>"
                this.$.xDialog.title = "<g:message code="messageLbl"/>"
                this.$.xDialog.opened = true
            },
            accept: function() {
                if(this.uriData != null) {
                    window.location.href = this.uriData.replace("\n","")
                }
                this.close()
                this.fire('core-signal', {name: "messagedialog-accept", data: {callerId:this.callerId}});
            },
            close: function() {
                this.$.xDialog.opened = false
                this.fire('core-signal', {name: "messagedialog-closed", data: {callerId:this.callerId}});
            }
        });
    </script>
</polymer-element>
