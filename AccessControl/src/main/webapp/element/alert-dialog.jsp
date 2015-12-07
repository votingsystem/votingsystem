<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="alert-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div>
                <div class="layout horizontal center center-justified">
                    <div hidden="{{!caption}}" flex style="font-size: 1.4em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">{{caption}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div style="font-size: 1.2em; color:#888; font-weight: bold; text-align: center; padding:10px 20px 10px 20px; display:block;word-wrap:break-word;">
                    <vs-html-echo html="{{message}}"></vs-html-echo>
                </div>
                <div class="layout horizontal"  style="margin:0px 20px 0px 0px;">
                    <div class="flex"></div>
                    <div hidden="{{!isConfirmMessage}}">
                        <button on-click="accept">
                            <i class="fa fa-check"></i> ${msg.acceptLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'alert-dialog',
            ready: function() {
                this.isConfirmMessage = this.isConfirmMessage || false
            },
            setMessage: function(message, caption, callerId, isConfirmMessage) {
                this.reset()
                this.message = message
                this.caption = caption
                this.callerId = callerId
                this.isConfirmMessage = isConfirmMessage
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            reset: function() {
                this.message = null
                this.callerId = null
                this.caption = null
                this.isConfirmMessage = false
            },
            accept: function() {
                this.close()
                this.fire('iron-signal', {name: "messagedialog-accept", data: {callerId:this.callerId}});
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
                this.fire('iron-signal', {name: "messagedialog-closed", data: {callerId:this.callerId}});
            }
        });
    </script>
</dom-module>
