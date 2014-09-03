<link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/add-control-center-dialog.gsp']"/>">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/add-voting-option-dialog.gsp']"/>">

<polymer-element name="vote-editor" attributes="opened">
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
        <div class="pageHeader"><h3><g:message code="publishVoteLbl"/></h3></div>

        <div style="display:{{messageToUser? 'block':'none'}}">
            <div class="messageToUser">
                <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                    <div id="messageToUser">{{messageToUser}}</div>
                </div>
                <paper-shadow z="1"></paper-shadow>
            </div>
        </div>

        <form id="mainForm" on-submit="{{submitForm}}">

            <div style="margin:0px 0px 20px 0px">
                <div style="display: block;">
                    <input type="text" name="subject" id="subject" style="width:600px" required
                           title="<g:message code="subjectLbl"/>" class="form-control"
                           placeholder="<g:message code="subjectLbl"/>"
                           oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                           onchange="this.setCustomValidity('')" />
                </div>
                <div layout horizontal center id="dateRangeDiv" style="margin:10px 0px 0px 0px;">
                    <label>${message(code:'dateBeginLbl')}</label>
                    <div class="datePicker">
                        <g:datePicker id="dateBegin" name="dateBegin" value="${new Date().plus(2)}" precision="day" relativeYears="[0..1]"/>
                    </div>


                    <label style="margin:0px 0px 0px 30px;">${message(code:'dateFinishLbl')}</label>
                    <g:datePicker id="dateFinish" name="dateFinish" value="${new Date().plus(2)}" precision="day" relativeYears="[0..1]"/>

                </div>
            </div>

            <div style="position:relative; width:100%;">
                <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
            </div>

            <div>
                <div layout horizontal center center-justified style="margin: 15px auto 30px auto;">
                    <div id="controlCenterLink" on-click="{{showControlCenterDialog}}" style="font-size:1.1em; color:#02227a;
                        cursor: pointer; cursor: hand;"> <g:message code="controlCenterLbl"/>  <i class="fa fa-info-circle"></i>
                    </div>
                    <div style="margin: 0px 0px 0px 30px;">
                        <select id="controlCenterSelect" style="" on-change="{{selectControlCenter}}" class="form-control" required>
                            <option value=""> --- <g:message code="selectControlCenterLbl"/> --- </option>
                            <template repeat="{{controlCenter in controlCenters}}">
                                <option value="{{controlCenter}}">{{controlCenterVS.name}}</option>
                            </template>
                        </select>
                    </div>
                    <div flex></div>
                    <div>
                        <button id="addOptionButton" type="button" class="btn btn-default" style=""
                                onclick="document.querySelector('#addVotingOptionDialog').show()"><g:message code="addOptionLbl"/> <i class="fa fa-plus"></i>
                        </button>
                    </div>
                </div>
            </div>


            <div id="fieldsDiv" class="fieldsBox" style="display:none;">
                <fieldset id="fieldsBox">
                    <legend id="fieldsLegend" style="border: none;"><g:message code="pollFieldLegend"/></legend>
                    <div id="fields" style="">
                        <template repeat="{{pollOption in pollOptionList}}">
                            {{pollOption}}
                        </template>
                    </div>
                </fieldset>
            </div>

            <div style="position:relative; margin:0px 10px 30px 30px;">
                <button id="buttonAccept" type="submit" class="btn btn-default" style="position:absolute; right:10px; top:0px;">
                    <g:message code="publishDocumentLbl"/> <i class="fa fa fa-check"></i>
                </button>
            </div>

        </form>

    </div>

        <add-control-center-dialog id="addControlCenterDialog"></add-control-center-dialog>
        <add-voting-option-dialog id="addVotingOptionDialog"></add-voting-option-dialog>
    </template>
    <script>
        Polymer('vote-editor', {
            opened: false,
            pollOptionList : [],
            controlCenter:null,
            ready: function() {
                console.log(this.tagName + " - ready")
                this.$.addVotingOptionDialog.addEventListener('on-submit', function (e) {
                    console.log("========== e.detail: " + e.detail)
                    this.pollOptionList.push(e.detail)
                })
            },
            publish: {
                controlCenters: {value: {}}
            },
            showControlCenterDialog: function() {
                this.$.addControlCenterDialog.show()
            },
            submitForm: function() {
                this.messageToUser = null
                this.$.subject.removeClass("formFieldError");
                this.$.controlCenterSelect.removeClass( "formFieldError" )

                if(!this.$.subject.validity.valid) {
                    this.$.subject.addClass("formFieldError");
                    this.messageToUser = '<g:message code="emptyFieldMsg"/>'
                    return
                }

                if(dateBegin == null) {
                    this.messageToUser = '<g:message code="emptyFieldMsg"/>'
                    return
                }

                if(dateFinish == null) {
                    this.messageToUser = '<g:message code="emptyFieldMsg"/>'
                    return
                }

                if(dateFinish < new Date() ) {
                    this.messageToUser = '<g:message code="dateInitERRORMsg"/>'
                    return
                }

                if(dateBegin > dateFinish) {
                    this.messageToUser = '<g:message code="dateRangeERRORMsg"/>'
                    return
                }

                if(textEditor.getData() == 0) {
                    this.$.textEditor.classList.add("formFieldError");
                    this.messageToUser = '<g:message code="emptyDocumentERRORMsg"/>'
                    return
                }

                if(!this.$.controlCenterSelect.validity.valid) {
                    this.$.controlCenterSelect.addClass( "formFieldError" );
                    this.messageToUser = '<g:message code="selectControlCenterLbl"/>'
                    return
                }


                if(this.pollOptionList.length < 2) { //two options at least
                    this.messageToUser = '<g:message code="optionsMissingERRORMsg"/>'
                    this.$.addOptionButton.addClass( "formFieldError" );
                    return
                }

                var eventVS = new EventVS();
                eventVS.subject = this.$.subject.value;
                eventVS.content = textEditor.getData();
                eventVS.dateBegin = getDatePickerValue('dateBegin').format();
                eventVS.dateFinish = getDatePickerValue('dateFinish').format();
                eventVS.controlCenter = this.controlCenter
                eventVS.fieldsEventVS = this.pollOptionList
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.VOTING_PUBLISHING)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.signedContent = eventVS
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                webAppMessage.serviceURL = "${createLink(controller:'eventVSElection', absolute:true)}"
                webAppMessage.signedMessageSubject = "<g:message code="publishVoteSubject"/>"



                var objectId = Math.random().toString(36).substring(7)
                webAppMessage.callerCallback = objectId

                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("publishDocumentCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    electionDocumentURL = null
                    if(appMessageJSON != null) {
                        var caption = '<g:message code="publishERRORCaption"/>'
                        var msg = appMessageJSON.message
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = '<g:message code="publishOKCaption"/>'
                            var msgTemplate = "<g:message code='documentLinkMsg'/>";
                            msg = "<p><g:message code='publishOKMsg'/>.</p>" +  msgTemplate.format(appMessageJSON.message);
                            window.location.href = appMessageJSON.message
                        }
                        showMessageVS(msg, caption)
                    }
                    }.bind(this)}




                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage)
            },
            selectControlCenter: function () {
                this.controlCenter = this.$.controlCenterSelect.value
                console.log("selectControlCenter: " + this.controlCenter)
                if("" != optionSelected) {

                }
            }
        });
    </script>

</polymer-element>