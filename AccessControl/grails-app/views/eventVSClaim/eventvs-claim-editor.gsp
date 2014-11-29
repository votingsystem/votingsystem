<vs:webresource dir="vs-texteditor" file="vs-texteditor.html"/>
<vs:webcomponent path="/element/eventvs-option-dialog"/>

<polymer-element name="eventvs-claim-editor">
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
        <core-signals on-core-signal-messagedialog-closed="{{messagedialog}}"></core-signals>
        <div class="pageHeader"  layout horizontal center center-justified>
            <h3><g:message code="publishClaimLbl"/></h3>
        </div>

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
                <div>
                    <input type="text" id="subject" class="form-control" style="width:600px;"
                           placeholder="<g:message code="subjectLbl"/>" error="<g:message code="requiredLbl"/>" required/>
                </div>
                <div layout horizontal center id="dateRangeDiv" style="margin:10px 0px 0px 0px;">
                    <label style="margin:0px 0px 0px 30px;">${message(code:'dateFinishLbl')}</label>
                    <div id="dateFinish">
                        <g:datePicker name="dateFinish" value="${new Date().plus(2)}" precision="day" relativeYears="[0..1]"/>
                    </div>
                </div>
            </div>

            <div style="position:relative; width:100%;">
                <vs-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></vs-texteditor>
            </div>

            <div style="margin:0px 0px 30px 0px;">
                <div style="font-size: 0.9em; margin:10px 0 0 20px; width:60%;display: inline-block;">
                    <input type="checkbox" id="multipleSignaturesCheckbox"><g:message code="multipleClaimsLbl"/><br>
                    <input type="checkbox" id="allowBackupRequestCheckbox"><g:message code="allowBackupRequestLbl"/>
                </div>
            </div>

            <div id="fieldsDiv" style="display:{{claimOptionList.length == 0? 'none':'block'}}">
                <div class="fieldsBox">
                    <fieldset>
                        <legend><g:message code="claimFieldLegend"/></legend>
                        <div layout vertical>
                            <template repeat="{{claimOption in claimOptionList}}">
                                <div>
                                    <a class="btn btn-default" on-click="{{removePollOption}}" style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                                        <g:message code="deleteLbl"/> <i class="fa fa-minus"></i></a>
                                    {{claimOption}}
                                </div>

                            </template>
                        </div>
                    </fieldset>
                </div>
            </div>

            <div layout horizontal center center-justified style="margin: 15px auto 30px auto;padding:0px 10px 0px 10px;">
                <div>
                    <button id="addOptionButton" type="button" class="btn btn-default" style=""
                            on-click={{showVotingOptionDialog}}"><g:message code="addClaimFieldLbl"/> <i class="fa fa-plus"></i>
                    </button>
                </div>
                <div flex></div>
                <paper-button raised on-click="{{submitForm}}" style="margin: 0px 0px 0px 5px;">
                    <i class="fa fa-check"></i> <g:message code="publishLbl"/>
                </paper-button>
            </div>
        </form>

    </div>

        <eventvs-option-dialog id="addVotingOptionDialog"></eventvs-option-dialog>
    </template>
    <script>
        Polymer('eventvs-claim-editor', {
            appMessageJSON:null,
            claimOptionList : [],
            ready: function() {
                console.log(this.tagName + " - ready")
                //alert( CKEDITOR.basePath );
                this.$.addVotingOptionDialog.addEventListener('on-submit', function (e) {
                    this.claimOptionList.push(e.detail)
                }.bind(this))
            },
            showVotingOptionDialog: function() {
                this.$.addVotingOptionDialog.show()
            },
            removePollOption: function(e) {
                var claimOption = e.target.templateInstance.model.claimOption
                console.log("removePollOption")
                for(optionIdx in this.claimOptionList) {

                    console.log("option: " +  this.claimOptionList[optionIdx] + " - claimOption: " + claimOption)
                    if(claimOption == this.claimOptionList[optionIdx]) {
                        this.claimOptionList.splice(optionIdx, 1)
                    }
                }
            },
            submitForm: function() {
                this.messageToUser = null
                this.$.subject.classList.remove("formFieldError");
                var dateFinish = getDatePickerValue('dateFinish', this.$.dateFinish)

                if(!this.$.subject.validity.valid) {
                    this.$.subject.classList.add("formFieldError");
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

                if(this.$.textEditor.getData().length == 0) {
                    this.$.textEditor.classList.add("formFieldError");
                    this.messageToUser = '<g:message code="emptyDocumentERRORMsg"/>'
                    return
                }

                var eventVS = {};
                eventVS.subject = this.$.subject.value;
                eventVS.content = this.$.textEditor.getData();
                eventVS.dateFinish = dateFinish.formatWithTime();
                eventVS.fieldsEventVS = this.claimOptionList

                if(this.$.multipleSignaturesCheckbox.checked) eventVS.cardinality = "MULTIPLE"
                else eventVS.cardinality = "EXCLUSIVE"
                eventVS.backupAvailable = this.$.allowBackupRequestCheckbox.checked

                var webAppMessage = new WebAppMessage(Operation.CLAIM_PUBLISHING)
                webAppMessage.signedContent = eventVS
                webAppMessage.serviceURL = "${createLink( controller:'eventVSClaim', absolute:true)}"
                webAppMessage.signedMessageSubject = "<g:message code="publishClaimSubject"/>"
                this.appMessageJSON = null
                webAppMessage.setCallback(function(appMessage) {
                    console.log("publishDocumentCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    electionDocumentURL = null
                    var caption = '<g:message code="publishERRORCaption"/>'
                    var msg = appMessageJSON.message
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = '<g:message code="publishOKCaption"/>'
                        var msgTemplate = "<g:message code='documentLinkMsg'/>";
                        msg = "<p><g:message code='publishOKMsg'/>.</p>" +  msgTemplate.format(appMessageJSON.message);
                    }
                    showMessageVS(msg, caption)
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage)
            },
            messagedialog:function() {
                if(this.appMessageJSON != null && this.appMessageJSON.message != null)
                    window.location.href = this.appMessageJSON.message
            }
        });
    </script>

</polymer-element>