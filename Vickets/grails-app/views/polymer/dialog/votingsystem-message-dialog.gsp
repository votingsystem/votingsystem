<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-html-echo', file: 'votingsystem-html-echo.html')}">
<polymer-element name="votingsystem-message-dialog">
    <template>
        <style no-shim>
        .view { :host {position: relative;} }
        .card {
            position: relative;
            display: inline-block;
            vertical-align: top;
            background-color: #f9f9f9;
            box-shadow: 0 12px 15px 0 rgba(0, 0, 0, 0.24);
            border: 1px solid #6c0404;
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
            background: #ba0011;
            color: #f9f9f9;
        }

        paper-button::shadow #ripple {
            color: green;
        }
        </style>
        <div class="card" style="width:400px;z-index: 3000; display: {{isVisible?'block':'none'}}">
            <div layout horizontal center center-justified style="background: #ba0011;">
                <div flex style="font-size: 1.3em; margin:0px 0px 0px 30px;font-weight: bold; color:#f9f9f9;">
                    <div style="display:{{caption? 'block':'none'}}">{{caption}}</div>
                </div>
                <div>
                    <core-icon-button on-click="{{close}}" icon="close" style="fill:#f9f9f9; color:#f9f9f9;"></core-icon-button>
                </div>
            </div>
            <div style="font-size: 1.3em; color:#6c0404; font-weight: bold; text-align: center; padding:30px 20px 30px 20px;">
                <votingsystem-html-echo html="{{message}}"></votingsystem-html-echo>
            </div>
            <div layout horizontal style="margin:0px 20px 0px 0px;">
                <div flex></div>
                <div style="margin:10px 0px 10px 0px;display:{{isConfirmMessage?'block':'none'}};">
                    <paper-button raisedButton class="button" label="<g:message code="acceptLbl"/>"
                                  on-click="{{accept}}" style=""></paper-button>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer('votingsystem-message-dialog', {
            isVisible:false,
            ready: function() {
                this.isVisible = false
                this.isConfirmMessage = this.isConfirmMessage || false
            },

            setMessage: function(message, caption, callerId, isConfirmMessage) {
                this.message = message
                this.caption = caption
                this.callerId = callerId
                this.isConfirmMessage = isConfirmMessage
                this.isVisible = true
            },

            accept: function() {
                var callerId = this.callerId
                this.close()
                this.fire('core-signal', {name: "messagedialog-accept", data: callerId});
            },

            close: function() {
                this.isVisible = false
                this.fire('core-signal', {name: "messagedialog-closed", data: this.callerId});
                this.message = null
                this.callerId = null
                this.caption = null
                this.isConfirmMessage = false
            }
        });
    </script>
</polymer-element>
