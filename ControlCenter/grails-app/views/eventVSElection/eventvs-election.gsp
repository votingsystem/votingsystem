<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-fab" file="paper-fab.html"/>
<vs:webresource dir="paper-fab" file="paper-fab.html"/>
<vs:webcomponent path="/eventVSElection/eventvs-election-stats"/>
<vs:webcomponent path="/element/time-elements"/>

<polymer-element name="eventvs-election" attributes="subpage">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style></style>
        <div vertical layout>

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
                        <div style="font-size: 1.2em; font-weight:bold;color:#fba131;"><g:message code="eventVSPendingMsg"/></div>
                    </template>
                    <template if="{{'TERMINATED' == eventvs.state || 'CANCELLED' == eventvs.state}}">
                        <div style="font-size: 2em; font-weight:bold;color:#cc1606;"><g:message code="eventVSFinishedLbl"/></div>
                    </template>
                </div>
                <div style="margin:0px 30px 0px 30px;"><b><g:message code="beginLbl"/>: </b>
                    <time is="local-time" datetime="{{eventvs.dateBeginStr}}" hour="numeric" minute="numeric"
                          day="numeric" month="short" year="numeric"/></div>
            </div>

            <div>
                <div class="eventContentDiv">
                    <vs-html-echo html="{{eventvs.content}}"></vs-html-echo>
                </div>

                <div id="eventAuthorDiv" class="text-right row" style="margin:0px 20px 20px 0px;font-size: 0.85em;">
                    <b><g:message code="publishedByLbl"/>: </b>{{eventvs.userVS}}
                </div>

                <div horizontal layout class="fieldsBox" style="">
                    <div style="width: 100%;">
                        <fieldset>
                            <legend><g:message code="pollFieldLegend"/></legend>
                            <div>
                                <template repeat="{{optionvs in eventvs.fieldsEventVS}}">
                                    <div class="voteOption" style="width: 90%;margin: 15px auto 0px auto;
                                        font-size: 1.3em; font-weight: bold;">
                                        - {{optionvs.content}}
                                    </div>
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
        <eventvs-admin-dialog id="eventVSAdminDialog"></eventvs-admin-dialog>
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
            back:function() {
                this.fire('core-signal', {name: "eventvs-election-closed", data: null});
            }
        });
    </script>
</polymer-element>