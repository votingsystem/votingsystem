<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">

<polymer-element name="eventvs-election" attributes="subpage">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style></style>
        <div class="pageContentDiv" style="min-width: 800px;">

            <div style="margin: 0px 30px;">
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
                <div class="eventContentDiv">
                    <votingsystem-html-echo html="{{eventvs.content}}"></votingsystem-html-echo>
                </div>

                <div id="eventAuthorDiv" class="text-right row" style="margin:0px 20px 20px 0px;">
                    <b><g:message code="publishedByLbl"/>: </b>{{eventvs.userVS}}
                </div>

                <div class="fieldsBox" style="">
                    <fieldset>
                        <legend><g:message code="pollFieldLegend"/></legend>
                        <div>
                            <template repeat="{{optionvs in eventvs.fieldsEventVS}}">
                                <div class="voteOption" style="width: 90%;margin: 10px auto 0px auto;">
                                    - {{optionvs.content}}
                                </div>
                            </template>
                        </div>
                    </fieldset>
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