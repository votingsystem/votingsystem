<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-fab', file: 'paper-fab.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/eventvs-election-voteconfirm-dialog']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/eventvs-admin-dialog']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/votevs-result-dialog']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/eventvs-election-stats']"/>">



<polymer-element name="eventvs-election" attributes="subpage">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style></style>
        <div vertical layout>
            <div style="text-align: center;">
                <template if="{{'admin' == menuType}}">
                    <template if="{{'ACTIVE' == eventvs.state || 'PENDING' == eventvs.state}}">
                        <paper-button raised type="submit" on-click="{{showAdminDialog}}"
                                style="margin:15px 20px 15px 0px;">
                            <g:message code="adminDocumentLinkLbl"/> <i class="fa fa fa-check"></i>
                        </paper-button>
                    </template>
                </template>
            </div>

            <div style="margin: 0px 30px;">
            <div layout horizontal center center-justified style="width:100%;">
                <template if="{{subpage}}">
                    <paper-fab icon="arrow-back" on-click="{{back}}" style="color: white;"></paper-fab>
                </template>
                <div flex id="pageTitle" eventvsId-data="{{eventvs.id}}" class="pageHeader text-center">{{eventvs.subject}}</div>
            </div>

            <div layout horizontal style="width: 100%;">
                <div flex>
                    <template if="{{'PENDING' == eventvs.state}}">
                        <div style="font-size: 1.3em; font-weight:bold;color:#fba131;"><g:message code="eventVSPendingMsg"/></div>
                    </template>
                    <template if="{{'TERMINATED' == eventvs.state}}">
                        <div style="font-size: 1.3em; font-weight:bold;color:#cc1606;"><g:message code="eventVSFinishedLbl"/></div>
                    </template>
                    <template if="{{'CANCELLED' == eventvs.state}}">
                        <div style="font-size: 1.3em; font-weight:bold;color:#cc1606;"><g:message code="eventVSCancelledLbl"/></div>
                    </template>
                </div>
                <template if="{{'PENDING' == eventvs.state}}">
                    <div><b><g:message code="dateBeginLbl"/>: </b>
                        {{eventvs.dateBeginStr}}</div>
                </template>
                <div style="margin:0px 30px 0px 30px; color: #888;"><b><g:message code="dateLimitLbl"/>: </b>
                    {{eventvs.dateFinishStr}}</div>
            </div>

            <div>
                <div class="eventContentDiv">
                    <vs-html-echo html="{{eventvs.content}}"></vs-html-echo>
                </div>

                <div id="eventAuthorDiv" class="text-right row" style="margin:0px 20px 20px 0px; color:#888;">
                    <b><g:message code="publishedByLbl"/>: </b>{{eventvs.userVS}}
                </div>

                <div horizontal layout class="fieldsBox" style="">
                    <div style="width: 100%;">
                        <fieldset>
                            <legend><g:message code="pollFieldLegend"/></legend>
                            <div>
                                <template if="{{'ACTIVE' == eventvs.state}}">
                                    <div vertical layout>
                                        <template repeat="{{optionvs in eventvs.fieldsEventVS}}">
                                            <paper-button raised on-click="{{showConfirmDialog}}"
                                                          style="margin: 30px 0px 0px 5px;font-size: 1.2em; border: 1px solid #6c0404;">
                                                {{optionvs.content}}
                                            </paper-button>
                                        </template>
                                    </div>
                                </template>
                                <template if="{{'ACTIVE' != eventvs.state}}">
                                    <template repeat="{{optionvs in eventvs.fieldsEventVS}}">
                                        <div class="voteOption" style="width: 90%;margin: 15px auto 0px auto;
                                        font-size: 1.3em; font-weight: bold;">
                                            - {{optionvs.content}}
                                        </div>
                                    </template>
                                </template>
                            </div>
                        </fieldset>
                    </div>

                    <eventvs-election-stats eventVSId="{{eventvs.id}}"></eventvs-election-stats>
                </div>
            </div>
            </div>
        </div>
        <eventvs-vote-confirm-dialog id="confirmOptionDialog"></eventvs-vote-confirm-dialog>
        <eventvs-admin-dialog id="eventVSAdminDialog" eventvs="{{eventvs}}"></eventvs-admin-dialog>
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
            fireSignal:function() {
                this.fire('core-signal', {name: "vs-innerpage", data: {title:"<g:message code="pollLbl"/>"}});
            },
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
                this.eventvs.voteVS = voteVS
                webAppMessage.eventVS = this.eventvs
                webAppMessage.signedMessageSubject = '<g:message code="sendVoteMsgSubject"/>'
                webAppMessage.setCallback(function(appMessage) {
                    console.log(this.tagName + " - vote callback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    appMessageJSON.eventVS = this.eventvs
                    appMessageJSON.optionSelected = this.optionVSSelected.content
                    this.$.votevsResultDialog.show(appMessageJSON)
                }.bind(this))
                console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>