<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-html-echo', file: 'votingsystem-html-echo.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">



<polymer-element name="votingsystem-message-dialog" attributes="opened">
    <template>
        <votingsystem-dialog id="xDialog" class="dialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
            <style no-shim>
            .dialog {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 13px;
                -webkit-user-select: none;
                -moz-user-select: none;
                overflow: auto;
                background: white;
                padding:10px 30px 30px 30px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 400px;
            }
            paper-button.button {
                background-color: #f9f9f9;
                color: #6c0404;
                border: 1px solid #ccc;
                margin:10px;
                vertical-align: middle;
                line-height: 24px;
                height: 35px;
            }
            paper-button:hover {
                background: #;
                color: #f9f9f9;
            }

            paper-button::shadow #ripple {
                color: green;
            }
            </style>
            <div>
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:0px 0px 0px 30px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;display:{{caption? 'block':'none'}}">{{caption}}</div>
                    </div>
                    <div>
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <div style="font-size: 1.2em; color:#888; font-weight: bold; text-align: center; padding:10px 20px 10px 20px; display:block;word-wrap:break-word;">
                    <votingsystem-html-echo html="{{message}}"></votingsystem-html-echo>
                </div>
                <div layout horizontal style="margin:0px 20px 0px 0px;">
                    <div flex></div>
                    <div style="margin:10px 0px 10px 0px;display:{{isConfirmMessage?'block':'none'}};">
                        <votingsystem-button on-click="{{accept}}" style="margin: 0px 0px 0px 5px;">
                            <g:message code="acceptLbl"/> <i class="fa fa-check"></i>
                        </votingsystem-button>
                    </div>
                </div>
            </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('votingsystem-message-dialog', {
            ready: function() {
                this.isConfirmMessage = this.isConfirmMessage || false
            },
            onCoreOverlayOpen:function(e) {
                this.opened = this.$.xDialog.opened
            },
            openedChanged:function() {
                this.$.xDialog.opened = this.opened
                if(this.opened == false) this.close()
            },
            setMessage: function(message, caption, callerId, isConfirmMessage) {
                this.message = message
                this.caption = caption
                this.callerId = callerId
                this.isConfirmMessage = isConfirmMessage
                this.opened = true
            },

            accept: function() {
                var callerId = this.callerId
                this.close()
                this.fire('core-signal', {name: "messagedialog-accept", data: callerId});
            },

            close: function() {
                this.opened = false
                this.fire('core-signal', {name: "messagedialog-closed", data: this.callerId});
                this.message = null
                this.callerId = null
                this.caption = null
                this.isConfirmMessage = false
            }
        });
    </script>
</polymer-element>
