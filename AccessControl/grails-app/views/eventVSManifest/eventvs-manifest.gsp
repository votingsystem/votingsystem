<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/eventvs-admin-dialog.gsp']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/eventvs-backup-request-dialog.gsp']"/>">

<polymer-element name="eventvs-manifest">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>

        </style>
        <div class="pageContentDiv" style="width:100%;max-width: 1000px; padding:0px 20px 0px 20px;">
            <template if="{{'admin' == menuType}}">
                <div class="text-center" style="">
                    <template if="{{'ACTIVE' == eventvs.state || 'PENDING' == eventvs.state}}">
                        <button type="submit" class="btn btn-warning" on-click="{{showAdminDialog}}"
                                style="margin:15px 20px 15px 0px;">
                            <g:message code="adminDocumentLinkLbl"/> <i class="fa fa fa-check"></i>
                        </button>
                    </template>
                </div>
            </template>

            <h3><div class="pageHeader text-center">{{eventvs.subject}}</div></h3>

            <div>
                <div class="" style="margin:0px 0px 0px 30px;><b><g:message code="dateLimitLbl"/>: </b>
                {{eventvs.dateFinishStr}}</div>
            </div>

            <div>
                <div class="eventContentDiv" style="width:100%;">
                    <votingsystem-html-echo html="{{eventvs.content}}"></votingsystem-html-echo>
                </div>

                <div>
                    <template if="{{eventvs.numSignatures > 0}}">
                        <div style="float:left;margin:10px 0px 0px 40px;">
                            <template if="{{eventvs.backupAvailable}}">
                                <button id="requestBackupButton" type="button" class="btn btn-default btn-lg"
                                        on-click="{{requestBackup}}" style="margin:0px 20px 0px 0;">
                                    {{numSignaturesMsg}}
                                </button>
                            </template>
                            <template if="{{!eventvs.backupAvailable}}">{{numSignaturesMsg}}</template>
                        </div>
                    </template>
                    <div flex></div>
                    <div id="eventAuthorDiv" style="margin:0px 20px 20px 0px;">
                        <b><g:message code="publishedByLbl"/>: </b>{{eventvs.userVS}}
                    </div>
                </div>
            </div>

            <div layout horizontal center center-justified style="margin: 15px auto 30px auto;padding:0px 10px 0px 10px;">
                <div flex></div>
                <votingsystem-button on-click="{{submitManifest}}" style="margin: 0px 0px 0px 5px;">
                    <i class="fa fa-check" style="margin:0 5px 0 2px;"></i> <g:message code="signLbl"/>
                </votingsystem-button>
            </div>
        </div>
        <eventvs-admin-dialog id="eventVSAdminDialog"></eventvs-admin-dialog>
        <eventvs-backup-request-dialog id="requestBackupDialog"></eventvs-backup-request-dialog>
    </template>
    <script>
        Polymer('eventvs-manifest', {
            menuType:menuType,
            numSignaturesMsg:null,
            publish: { eventvs: {value: {}} },
            eventvsChanged:function() {
                this.numSignaturesMsg = '<g:message code="numSignaturesForEvent"/>'.format(this.eventvs.numSignatures)
            },
            ready: function() {
                console.log(this.tagName + "- menuType:  " + this.menuType)
                this.$.requestBackupDialog.addEventListener('backup-request-ok', function (e) {
                    showMessageVS("<g:message code='operationOKCaption'/>", e.detail)
                }.bind(this))
            },
            showAdminDialog:function() {
                this.$.eventVSAdminDialog.opened = true
            },
            requestBackup:function() {
                this.$.requestBackupDialog.show(this.eventvs)
            },
            submitManifest:function() {
                console.log("submitManifest")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.MANIFEST_SIGN)
                webAppMessage.serviceURL = "${createLink( controller:'eventVSManifestCollector', absolute:true)}/" + this.eventvs.id
                webAppMessage.signedMessageSubject = this.eventvs.subject
                webAppMessage.contentType = 'application/x-pkcs7-signature'
                webAppMessage.eventVS = this.eventvs
                webAppMessage.documentURL = this.eventvs.URL
                webAppMessage.setCallback(function(appMessage) {
                    console.log(this.tagName + " - vote callback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    var caption = '<g:message code="operationERRORCaption"/>'
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='operationOKCaption'/>"
                    }
                    showMessageVS(appMessageJSON.message, caption)
                }.bind(this))
                console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>