<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/uservs-search']"/>">

<polymer-element name="uservs-search-dialog" attributes="transactionvsURL opened">
    <template>
        <votingsystem-dialog id="xDialog" class="uservsSearchDialog" title="<g:message code="userSearchLbl"/>"
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
                        <votingsystem-user-box flex id="receptorBox" boxCaption="<g:message code="receptorLbl"/>"></votingsystem-user-box>
                    </div>

                    <div>
                        <div layout horizontal center center-justified id="searchPanel" style="margin:5px auto 0px auto;width: 100%;">
                            <input id="userSearchInput" type="text" style="width:200px;" class="form-control"
                                   placeholder="<g:message code="enterReceptorDataMsg"/>">
                            <votingsystem-button on-click="{{searchUser}}" style="margin: 0px 0px 0px 5px;">
                                <i class="fa fa-search" style="margin:0 7px 0 3px;"></i> <g:message code="userSearchLbl"/>
                            </votingsystem-button>
                        </div>
                        <uservs-search id="userSearchList" isSelector="true"></uservs-search>
                    </div>
                </div>
            </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('uservs-search-dialog', {
            ready: function() {
                console.log(this.tagName + " - " + this.id + " - ready")
                this.$.userSearchInput.onkeypress = function(event){
                    if (event.keyCode == 13)  this.searchUser()
                }.bind(this)

                document.querySelector("#coreSignals").addEventListener('core-signal-user-clicked', function(e) {
                    console.log(this.tagName + " - user-clicked - closing dialog")
                    this.$.xDialog.opened = false
                }.bind(this));
            },
            reset: function() {
                console.log(this.id + " - reset")
                this.removeErrorStyle(this.$.formDataDiv)
                this.isTransactionVSFromGroupToAllMembers = false
                this.$.userSearchInput.value = ""
                this.$.amount.value = ""
                this.$.transactionvsSubject.value = ""
                this.$.userSearchList.url = ""
                this.setMessage(200, null)
                this.$.receptorBox.removeUsers()
                this.$.userSearchList.reset()
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
                this.$.userSearchList.url = targetURL
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</polymer-element>