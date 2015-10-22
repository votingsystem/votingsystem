<%@ page contentType="text/html; charset=UTF-8" %>

<link href="representative-cancel-dialog.vsp" rel="import"/>

<dom-module name="representative-edit">
    <template>
        <div>
            <div class="layout horizontal center center-justified">
                <button type="button" on-click="representativeRevoke"
                        style="margin:15px 20px 15px 0px;">
                    <i class="fa fa-times"></i> ${msg.removeRepresentativeLbl}
                </button>
                <button type="button" on-click="representativeEdit"
                        style="margin:15px 20px 15px 0px;">
                    <i class="fa fa-hand-o-right"></i> ${msg.editRepresentativeLbl}
                </button>
            </div>
        </div>
    </div>

        <representative-cancel-dialog id="representativeRevokeDialog"></representative-cancel-dialog>
        <paper-dialog id="editDialog" with-backdrop no-cancel-on-outside-click>
            <div class="layout horizontal center center-justified">
                <div hidden="{{!caption}}" flex style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;">${msg.editRepresentativeLbl}</div>
                </div>
                <div style="position: absolute; top: 0px; right: 0px;">
                    <i class="fa fa-times closeIcon" on-click="closeEdit"></i>
                </div>
            </div>
            <div class="layout vertical center center-justified" style="margin:15px auto 0px auto; width: 250px;">
                <label style="margin:0px 0px 20px 0px">${msg.representativeNIFLbl}</label>
                <input type="text" id="representativeNif" style="width:200px; margin:0px auto 0px auto;" class="form-control"/>
                <div style="width: 100%;">
                    <div class="layout horizontal" style="margin: 15px auto 30px auto;padding:0px 10px 0px 10px;">
                        <div class="flex"></div>
                        <button on-click="checkRepresentativeNIF" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-check"></i> ${msg.acceptLbl}
                        </button>
                    </div>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer({
            is:'representative-edit',
            ready: function() {
                console.log(this.tagName + " - ready")
                this.$.representativeNif.onkeypress = function(event){
                    if (event.keyCode == 13) this.checkRepresentativeNIF()
                }.bind(this)
            },
            closeEdit: function() {
                this.$.editDialog.opened = false
            },
            representativeEdit: function() {
                this.$.editDialog.opened = true
            },
            representativeRevoke: function() {
                this.$.representativeRevokeDialog.show()
            },
            checkRepresentativeNIF: function() {
                console.log(this.tagName + " - ready")
                var validatedNif = validateNIF(this.$.representativeNif.value)
                if(validatedNif == null) showMessageVS('${msg.nifERRORMsg}','${msg.errorLbl}')
                else {
                    var operationVS = new OperationVS(Operation.EDIT_REPRESENTATIVE)
                    operationVS.nif = validatedNif
                    VotingSystemClient.setMessage(operationVS);
                    this.$.editDialog.opened = false
                }
            }
        });
    </script>
</dom-module>