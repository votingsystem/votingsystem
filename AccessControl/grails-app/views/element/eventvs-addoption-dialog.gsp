<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-dialog', file: 'vs-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">

<polymer-element name="add-voting-option-dialog" attributes="opened">
    <template>
        <style></style>
        <vs-dialog flex vertical id="xDialog" vertical opened="{{opened}}" layered="true" sizingTarget="{{$.container}}">
            <g:include view="/include/styles.gsp"/>
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
                .messageToUser {
                    font-weight: bold;
                    margin:10px auto 10px auto;
                    background: #f9f9f9;
                    padding:10px 20px 10px 20px;
                }
            </style>
            <div id="container" layout vertical class="card" style="overflow-y: auto; width:450px; padding:10px;">
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:0px 0px 0px 30px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;"><g:message code="addOptionLbl"/></div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>

                <div style="display:{{messageToUser? 'block':'none'}}">
                    <div class="messageToUser">
                        <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                            <div id="messageToUser">{{messageToUser}}</div>
                        </div>
                        <paper-shadow z="1"></paper-shadow>
                    </div>
                </div>

                <div  layout horizontal center center-justified  style="margin:10px 0px 10px 0px;">
                    <input type="text" class="form-control" id="optionContent" style="width:420px;"
                           placeholder="<g:message code="pollOptionContentMsg"/>" required/>
                </div>

                <div layout horizontal style="margin:10px 20px 0px 0px; margin:10px;">
                    <div flex></div>
                    <paper-button raised on-click="{{submitForm}}" style="margin: 0px 0px 0px 5px;">
                        <i class="fa fa-check" style="margin:0 5px 0 2px;"></i> <g:message code="acceptLbl"/>
                    </paper-button>
                </div>
               <content></content>
            </div>
        </vs-dialog>
    </template>
<script>
    Polymer('add-voting-option-dialog', {
        opened: false,
        ready: function() {
            this.$.optionContent.onkeypress = function(event){
                if (event.keyCode == 13) this.submitForm()
            }.bind(this)
        },
        openedChanged: function() {
            if(!this.opened) {
                this.messageToUser = null
                this.$.optionContent.value = ""
            }
        },
        close: function() {
            this.opened = false;
        },
        submitForm: function() {
            this.messageToUser = null
            if(!this.$.optionContent.validity.valid) {
                this.messageToUser = "<g:message code="enterOptionContentErrorMsg"/>"
                return
            }
            this.fire('on-submit', this.$.optionContent.value);
            this.opened = false
        },
        toggle: function() {
            this.$.xDialog.toggle();
        },
        show: function() {
            this.$.xDialog.opened = true
        }
    });
</script>

</polymer-element>