<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-dialog', file: 'vs-dialog.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/uservs-selector']"/>">

<polymer-element name="uservs-selector-dialog" attributes="transactionvsURL opened">
    <template>
        <vs-dialog id="xDialog" class="uservsSearchDialog" title="<g:message code="userSearchLbl"/>"
                             on-core-overlay-open="{{onCoreOverlayOpen}}" style="overflow: auto;">
        <style no-shim>
            .uservsSearchDialog {
                width: 600px;
                min-height: 300px;
                padding: 10px 20px;
            }
        </style>
        <g:include view="/include/styles.gsp"/>
            <div id="main">
                <div style="">
                    <div>
                        <div class="center" style="padding: 10px;">{{selectReceptorMsg}}</div>
                        <vs-user-box flex id="receptorBox" boxCaption="<g:message code="receptorLbl"/>"></vs-user-box>
                    </div>

                    <div>
                        <div layout horizontal center center-justified id="searchPanel" style="margin:5px auto 0px auto;width: 100%;">
                            <input id="userSearchInput" type="text" style="width:200px;" class="form-control"
                                   placeholder="<g:message code="enterReceptorDataMsg"/>">
                            <paper-button raised on-click="{{searchUser}}" style="margin: 0px 0px 0px 5px;">
                                <i class="fa fa-search"></i> <g:message code="userSearchLbl"/>
                            </paper-button>
                        </div>
                        <uservs-selector id="userVSSelector" isSelector="true"></uservs-selector>
                    </div>
                </div>
            </div>
        </vs-dialog>
    </template>
    <script>
        Polymer('uservs-selector-dialog', {
            ready: function() {
                console.log(this.tagName + " - " + this.id + " - ready")
                this.$.userSearchInput.onkeypress = function(event){
                    if (event.keyCode == 13)  this.searchUser()
                }.bind(this)

                if(document.querySelector("#coreSignals")) {
                    document.querySelector("#coreSignals").addEventListener('core-signal-user-clicked', function(e) {
                        console.log(this.tagName + " - user-clicked - closing dialog")
                        this.$.xDialog.opened = false
                    }.bind(this));
                }
            },
            reset: function() {
                console.log(this.id + " - reset")
                this.removeErrorStyle(this.$.formDataDiv)
                this.isTransactionVSFromGroupToAllMembers = false
                this.$.userSearchInput.value = ""
                this.$.amount.value = ""
                this.$.transactionvsSubject.value = ""
                this.$.userVSSelector.url = ""
                this.setMessage(200, null)
                this.$.receptorBox.removeUsers()
                this.$.userVSSelector.reset()
                this.$.tagDialog.reset()
            },
            show: function() {
                console.log(this.tagName + " - show")
                this.$.xDialog.opened = true
            },
            searchUser: function() {
                var textToSearch = this.$.userSearchInput.value
                if(textToSearch.trim() == "") return
                var targetURL
                if(this.groupId != null) targetURL = "${createLink(controller: 'userVS', action: 'searchGroup')}?searchText=" + textToSearch + "&groupId=" + this.groupId
                else targetURL = "${createLink(controller: 'userVS', action: 'search')}?searchText=" + textToSearch
                this.$.userVSSelector.url = targetURL
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</polymer-element>