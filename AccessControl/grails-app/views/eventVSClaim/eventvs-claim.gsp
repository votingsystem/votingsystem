<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/eventvs-admin-dialog.gsp']"/>">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/eventvs-backup-request-dialog.gsp']"/>">

<polymer-element name="eventvs-claim">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .messageToUser {
                font-weight: bold;
                margin:10px auto 10px auto;
                background: #f9f9f9;
                padding:10px 20px 10px 20px;
            }
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

            <div style="display:{{messageToUser? 'block':'none'}}">
                <div class="messageToUser">
                    <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                        <div id="messageToUser">{{messageToUser}}</div>
                    </div>
                    <paper-shadow z="1"></paper-shadow>
                </div>
            </div>

            <div style="display:inline;">
                <div class="" style="margin:0px 0px 0px 30px; display: inline;"><b><g:message code="dateLimitLbl"/>: </b>
                    {{eventvs.dateFinishStr}}</div>
                <div id="pendingTimeDiv" class="text-right" style="margin:0px 40px 0px 0px; color: #388746; display: inline; white-space:nowrap;"></div>
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

            <template if="{{eventvs.fieldsEventVS.size() > 0}}">
                <div id="claimFields">
                    <fieldset>
                        <legend><g:message code="claimsFieldLegend"/></legend>
                        <div id="fields" style="width:100%;">
                            <template if="{{'ACTIVE' == eventvs.state}}">
                                <template repeat="{{claimField in eventvs.fieldsEventVS}}">
                                    <input type='text' id='claimField{{claimField.id}}' required class='form-control'
                                           placeholder='{{claimField.content}}'/>
                                </template>
                            </template>
                            <template if="{{'ACTIVE' != eventvs.state}}">
                                <template repeat="{{claimField in eventvs.fieldsEventVS}}">
                                    <div class="voteOption" style="width: 90%;margin: 10px auto 0px auto;">
                                        - {{claimField.content}}
                                    </div>
                                </template>
                            </template>
                        </div>
                    </fieldset>
                </div>
            </template>

            <div layout horizontal center center-justified style="margin: 15px auto 30px auto;padding:0px 10px 0px 10px;">
                <div flex></div>
                <votingsystem-button on-click="{{submitClaim}}" style="margin: 0px 0px 0px 5px;">
                    <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="signLbl"/>
                </votingsystem-button>
            </div>
        </div>
        <eventvs-admin-dialog id="eventVSAdminDialog"></eventvs-admin-dialog>
        <eventvs-backup-request-dialog id="requestBackupDialog"></eventvs-backup-request-dialog>
    </template>
    <script>
        Polymer('eventvs-claim', {
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
            submitClaim:function() {
                console.log("submitClaim")
                var fieldsArray = new Array();
                this.messageToUser = null
                if(this.eventvs.fieldsEventVS.size() > 0) {
                    for(optionIdx in this.eventvs.fieldsEventVS) {
                        var selectedClaimField =  this.eventvs.fieldsEventVS[optionIdx]
                        var claimOptionInputFieldId = "claimField" + selectedClaimField.id
                        console.log("claimOptionInputFieldId: " +  claimOptionInputFieldId)
                        if(this.$.claimFields.querySelector('#' + claimOptionInputFieldId).validity.valid) {
                            selectedClaimField.value = this.$.claimFields.querySelector('#' + claimOptionInputFieldId).value
                            fieldsArray.push(selectedClaimField)
                        } else {
                            this.messageToUser = '<g:message code="fieldsMissingErrorMsg"/>'
                            return
                        }
                    }
                }
                this.eventvs.fieldsEventVS = fieldsArray


                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.SMIME_CLAIM_SIGNATURE)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.serviceURL = "${createLink( controller:'eventVSClaimCollector', absolute:true)}"
                webAppMessage.signedMessageSubject = this.eventvs.subject
                //signed and encrypted
                webAppMessage.contentType = 'application/x-pkcs7-signature'
                webAppMessage.eventVS = this.eventvs
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                webAppMessage.documentURL = this.eventvs.URL



                this.eventvs.operation = Operation.SMIME_CLAIM_SIGNATURE
                webAppMessage.signedContent = this.eventvs


                var objectId = Math.random().toString(36).substring(7)
                webAppMessage.callerCallback = objectId

                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log(this.tagName + "eventvs-claim callback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    if(appMessageJSON != null) {
                        var caption = '<g:message code="operationERRORCaption"/>'
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = "<g:message code='operationOKCaption'/>"
                        } else if (ResponseVS.SC_CANCELLED== appMessageJSON.statusCode) {
                            caption = "<g:message code='operationCANCELLEDLbl'/>"
                        }
                        showMessageVS(appMessageJSON.message, caption)
                    }}.bind(this)}

                console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>