<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-fab" file="paper-fab.html"/>
<vs:webresource dir="core-media-query" file="core-media-query.html"/>
<vs:webcomponent path="/eventVSElection/eventvs-election-voteconfirm-dialog"/>
<vs:webcomponent path="/element/eventvs-admin-dialog"/>
<vs:webcomponent path="/element/votevs-result-dialog"/>
<vs:webcomponent path="/eventVSElection/eventvs-election-stats"/>
<vs:webresource dir="paper-tabs" file="paper-tabs.html"/>

<polymer-element name="eventvs-election" attributes="subpage">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .tabContent { margin:0px auto 0px auto; width:auto; }
            .representativeNameHeader { font-size: 1.3em; text-overflow: ellipsis; color:#6c0404; padding: 0 40px 0 40px;}
            .representativeNumRepHeader { text-overflow: ellipsis; color:#888;}
            paper-tabs.transparent-teal { padding: 0px; background-color: #ffeeee; color:#ba0011;
                box-shadow: none; cursor: pointer; height: 35px;
            }
            paper-tabs.transparent-teal::shadow #selectionBar {
                background-color: #ba0011;
            }
            paper-tabs.transparent-teal paper-tab::shadow #ink {
                color: #ba0011;
            }
        </style>
        <core-media-query query="max-width: 600px" queryMatches="{{smallScreen}}"></core-media-query>
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
                    <paper-fab mini icon="arrow-back" on-click="{{back}}" style="color: white;"></paper-fab>
                </template>
                <div flex id="pageTitle" eventvsId-data="{{eventvs.id}}" class="pageHeader">{{eventvs.subject}}</div>
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
                <div style="margin:0px 30px 0px 30px; color: #888;"><b><g:message code="electionDateLbl"/>: </b>
                    {{eventvs.dateBeginStr}}</div>
            </div>

            <div>
                <div class="eventContentDiv">
                    <vs-html-echo html="{{eventvs.content}}"></vs-html-echo>
                </div>

                <div id="eventAuthorDiv" class="text-right row" style="margin:0px 20px 20px 20px; color:#888; font-size: 0.85em;">
                    <b><g:message code="publishedByLbl"/>: </b>{{eventvs.userVS}}
                </div>

                <template if="{{smallScreen}}">
                    <paper-tabs style="margin:0px auto 0px auto; cursor: pointer;" class="transparent-teal center"
                                valueattr="name" selected="{{selectedTab}}"  on-core-select="{{tabSelected}}" noink>
                        <paper-tab name="optionsTab" style="width: 400px"><g:message code="pollFieldLegend"/></paper-tab>
                        <paper-tab name="statsTab"><g:message code="resultsLbl"/></paper-tab>
                    </paper-tabs>
                </template>


                <div horizontal layout class="fieldsBox">
                    <div style="width: 100%;display:{{smallScreen?(selectedTab == 'optionsTab'?'block':'none'):'block'}}">
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
                    <div style="display:{{smallScreen?(selectedTab == 'statsTab'?'block':'none'):'block'}}">
                        <eventvs-election-stats eventVSId="{{eventvs.id}}"></eventvs-election-stats>
                    </div>
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
                this.fire('core-signal', {name: "vs-innerpage", data: {caption:"<g:message code="pollLbl"/>"}});
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
                var webAppMessage = new WebAppMessage(Operation.SEND_SMIME_VOTE)
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