<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-overlay', file: 'core-overlay.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">


<polymer-element name="get-reason-dialog" attributes="caption opened isForAdmins messageToUser">
    <template>
        <style></style>
        <core-overlay flex vertical id="coreOverlay" vertical opened="{{opened}}" layered="true" sizingTarget="{{$.container}}">
            <!-- place all overlay styles inside the overlay target -->
            <style no-shim>
                .card {
                    position: relative;
                    display: inline-block;
                    vertical-align: top;
                    background-color: #f9f9f9;
                    box-shadow: 0 12px 15px 0 rgba(0, 0, 0, 0.24);
                    border: 1px solid #ccc;
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
            </style>
            <div id="container" layout vertical class="card" style="overflow-y: auto; width:450px; padding:10px;">
                <h3 style="color: #6c0404;">{{caption}}</h3>

                <div class="center" style="color: #6c0404;display:{{isForAdmins?'block':'none'}}">
                    <core-icon icon="warning" style="fill:#6c0404;"></core-icon>
                    <g:message code="systemAdminReservedOperationMsg"/>
                </div>
                <div class="center" style="color: #6c0404;display:{{messageToUser?'block':'none'}}">
                    {{messageToUser}}
                </div>
                <div style="margin:20px 0px 10px 0px;">
                    <paper-input id="reason" multiline floatingLabel rows="3" label="<g:message code="cancelSubscriptionFormMsg"/>"></paper-input>
                </div>

                <div layout horizontal style="margin:10px 20px 0px 0px; margin:10px;">
                    <div flex></div>
                    <paper-button raisedButton class="button" label="<g:message code="acceptLbl"/>"
                                  on-click="{{submitForm}}" style=""></paper-button>
                </div>
               <content></content>
            </div>
        </core-overlay>
    </template>
<script>
    Polymer('get-reason-dialog', {
        opened: false,
        isForAdmins: false,
        ready: function() { },
        openedChanged: function() {},
        submitForm: function() {
            this.fire('on-submit', this.$.reason.value);
        },
        toggle: function() {
            this.$.coreOverlay.toggle();
        },
        show: function(message) {
            this.messageToUser = message;
        }
    });
</script>

</polymer-element>
