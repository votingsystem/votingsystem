<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/eventvs-vote-confirm-dialog.gsp']"/>">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/eventvs-admin-dialog.gsp']"/>">


<polymer-element name="eventvs-election">
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

            <div style="display:inline;">
                <div class="" style="margin:0px 0px 0px 30px; display: inline;"><b><g:message code="dateLimitLbl"/>: </b>
                    {{eventvs.dateFinishStr}}</div>
                <div id="pendingTimeDiv" class="text-right" style="margin:0px 40px 0px 0px; color: #388746; display: inline; white-space:nowrap;"></div>
            </div>

            <div>
                <div class="eventContentDiv" style="width:100%;">
                    <votingsystem-html-echo html="{{eventvs.content}}"></votingsystem-html-echo>
                </div>

                <div id="eventAuthorDiv" class="text-right row" style="margin:0px 20px 20px 0px;">
                    <b><g:message code="publishedByLbl"/>: </b>{{eventvs.userVS}}
                </div>

                <div class="fieldsBox" style="">
                    <fieldset id="fieldsBox">
                        <legend id="fieldsLegend"><g:message code="pollFieldLegend"/></legend>
                        <div id="fields" class="" style="width:100%;">
                            <template if="{{'ACTIVE' == eventvs.state}}">
                                <template repeat="{{optionvs in eventvs.fieldsEventVS}}">
                                    <div class="btn btn-default btn-lg voteOptionButton"
                                         style="width: 90%;margin: 10px auto 30px auto;" on-click="{{showConfirmDialog}}">
                                        {{optionvs.content}}
                                    </div>
                                </template>
                            </template>
                            <template if="{{'ACTIVE' != eventvs.state}}">
                                <template repeat="{{optionvs in eventvs.fieldsEventVS}}">
                                    <div class="voteOption" style="width: 90%;margin: 10px auto 0px auto;">
                                        - {{optionvs.content}}
                                    </div>
                                </template>
                            </template>
                        </div>
                    </fieldset>
                </div>
            </div>
        </div>
        <eventvs-vote-confirm-dialog id="confirmOptionDialog"></eventvs-vote-confirm-dialog>
        <eventvs-admin-dialog id="eventVSAdminDialog"></eventvs-admin-dialog>
    </template>
    <script>
        Polymer('eventvs-election', {
            menuType:menuType,
            publish: {
                eventvs: {value: {}}
            },
            optionVSSelected:null,
            eventvsChanged:function() {
                this.optionVSSelected = null
            },
            ready: function() {
                console.log(this.tagName + "- menuType:  " + this.menuType)
                this.$.confirmOptionDialog.addEventListener('optionconfirmed', function (e) {
                    this.submitVote()
                }.bind(this))
            },
            showAdminDialog:function() {
                this.$.eventVSAdminDialog.opened = true
            },
            showConfirmDialog: function(e) {
                this.optionVSSelected = e.target.templateInstance.model.optionvs
                this.$.confirmOptionDialog.show(this.optionVSSelected.content)
            },
            submitVote:function() {
                console.log("submitVote")
                var voteVS = {optionSelected:this.optionVSSelected, eventId:this.eventvs.id, eventURL:this.eventvs.URL}
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SEND_SMIME_VOTE)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                votingEvent.voteVS = voteVS
                webAppMessage.eventVS = this.eventvs
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                webAppMessage.signedMessageSubject = '<g:message code="sendVoteMsgSubject"/>'

                var objectId = Math.random().toString(36).substring(7)
                webAppMessage.callerCallback = objectId

                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log(this.tagName + " - vote callback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    if(appMessageJSON != null) {
                        var caption = '<g:message code="voteERRORCaption"/>'
                        var msgTemplate = "<g:message code='voteResultMsg'/>"
                        var msg
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = "<g:message code='voteOKCaption'/>"
                            msg = msgTemplate.format('<g:message code="voteResultOKMsg"/>',appMessageJSON.message);
                        } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == appMessageJSON.statusCode) {
                            msgTemplate =  "<g:message code='accessRequestRepeatedMsg'/>"
                            msg = msgTemplate.format(votingEvent.subject, appMessageJSON.message);
                        } else msg = appMessageJSON.message
                        showMessageVS(caption, msg)
                    }}.bind(this)}

                console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>