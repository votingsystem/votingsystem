<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/eventvs-vote-confirm-dialog.gsp']"/>">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/eventvs-admin-dialog.gsp']"/>">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/votevs-result-dialog.gsp']"/>">



<polymer-element name="eventvs-election" attributes="subpage">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style></style>
        <div class="pageContentDiv">
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

            <div layout horizontal center center-justified style="width:100%;">
                <template if="{{subpage}}">
                    <votingsystem-button isFab on-click="{{back}}" style="font-size: 1.5em; margin:5px 0px 0px 0px;">
                        <i class="fa fa-arrow-left"></i></votingsystem-button>
                </template>
                <div flex id="pageTitle" class="pageHeader text-center"><h3>{{eventvs.subject}}</h3></div>
            </div>

            <div layout horizontal style="width: 100%;">
                <div flex>
                    <template if="{{'PENDING' == eventvs.state}}">
                        <div style="font-size: 1.2em; font-weight:bold;color:#fba131;"><g:message code="eventVSPendingMsg"/></div>
                    </template>
                    <template if="{{'TERMINATED' == eventvs.state || 'CANCELLED' == eventvs.state}}">
                        <div style="font-size: 1.2em; font-weight:bold;color:#cc1606;"><g:message code="eventVSFinishedLbl"/></div>
                    </template>
                </div>
                <template if="{{'PENDING' == eventvs.state}}">
                    <div><b><g:message code="dateBeginLbl"/>: </b>
                        {{eventvs.dateBeginStr}}</div>
                </template>
                <div style="margin:0px 30px 0px 30px;"><b><g:message code="dateLimitLbl"/>: </b>
                    {{eventvs.dateFinishStr}}</div>
            </div>

            <div>
                <div class="eventContentDiv" style="">
                    <votingsystem-html-echo html="{{eventvs.content}}"></votingsystem-html-echo>
                </div>

                <div id="eventAuthorDiv" class="text-right row" style="margin:0px 20px 20px 0px;">
                    <b><g:message code="publishedByLbl"/>: </b>{{eventvs.userVS}}
                </div>

                <div class="fieldsBox" style="">
                    <fieldset>
                        <legend><g:message code="pollFieldLegend"/></legend>
                        <div>
                            <template if="{{'ACTIVE' == eventvs.state}}">
                                <template repeat="{{optionvs in eventvs.fieldsEventVS}}">
                                    <div class="btn btn-default btn-lg" on-click="{{showConfirmDialog}}"
                                         style="width: 90%;margin: 10px auto 30px auto; border: 2px solid #6c0404; padding: 10px; font-size: 1.2em;" >
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
        <votevs-result-dialog id="votevsResultDialog"></votevs-result-dialog>
    </template>
    <script>
        Polymer('eventvs-election', {
            menuType:menuType,
            publish: {
                eventvs: {value: {}}
            },
            subpage:false,
            optionVSSelected:null,
            eventvsChanged:function() {
                this.optionVSSelected = null
            },
            ready: function() {
                console.log(this.tagName + "- subpage:  " + this.subpage)
                this.$.confirmOptionDialog.addEventListener('optionconfirmed', function (e) {
                    this.submitVote()
                }.bind(this))
            },
            showAdminDialog:function() {
                this.$.eventVSAdminDialog.opened = true
            },
            back:function() {
                this.fire('core-signal', {name: "eventvs-election-closed", data: null});
            },
            showConfirmDialog: function(e) {
                console.log(this.tagName + " showConfirmDialog")
                this.optionVSSelected = e.target.templateInstance.model.optionvs
                this.$.confirmOptionDialog.show(this.optionVSSelected.content)
            },
            submitVote:function() {
                console.log("submitVote")
                var voteVS = {optionSelected:this.optionVSSelected, eventId:this.eventvs.id, eventURL:this.eventvs.URL}
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SEND_SMIME_VOTE)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                this.eventvs.voteVS = voteVS
                webAppMessage.eventVS = this.eventvs
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                webAppMessage.signedMessageSubject = '<g:message code="sendVoteMsgSubject"/>'

                var objectId = Math.random().toString(36).substring(7)
                webAppMessage.callerCallback = objectId

                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log(this.tagName + " - vote callback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    appMessageJSON.eventVS = this.eventvs
                    appMessageJSON.optionSelected = this.optionVSSelected.content
                    this.$.votevsResultDialog.show(appMessageJSON)
                    }.bind(this)}

                console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>