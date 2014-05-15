<% def representativeFullName = representative?.firstName + " " + representative?.lastName %>
<html>
<head>
	<meta name="layout" content="main" />
</head>
<body>
<div id="contentDiv" style="max-width: 1200px; ; margin: 0 auto 0 auto;">
    <div style="margin: 0 30px 0 30px;">
        <div class="text-center row" style="margin:20px auto 15px 15px;">
            <div class="representativeNameHeader col-md-6">
                <div>${representativeFullName}</div>
            </div>
            <div  class="representativeNumRepHeader col-md-2" style="">
                ${representative.numRepresentations} <g:message code="numDelegationsPartMsg"/>
            </div>
            <g:if test="${"user".equals(params.menu)}">
                <div class="col-md-1" style="">
                    <button type="button" onclick="showSelectRepresentativeDialog(representativeOperationCallback, '${representativeFullName}')"
                            class="btn btn-default">
                        <g:message code="saveAsRepresentativeLbl"/> <i class="fa fa-hand-o-right"></i>
                    </button>
                </div>
            </g:if>
        </div>
        <div id="tabs" style="margin: 0 auto 0 auto;">
            <ul class="nav nav-tabs">
                <li class="active">
                    <a href="#tabs-1" data-toggle="tab" style="font-size: 1.2em;"><g:message code='profileLbl'/></a>
                </li>
                <li>
                    <a href="#tabs-2" data-toggle="tab" style="font-size: 1.2em;"><g:message code='votingHistoryLbl'/></a>
                </li>
            </ul>
        </div>

        <div class="tab-content" style="margin:20px 0 0 0;">
            <div class="tab-pane active" id="tabs-1">
                <div style="width: 90%;margin: auto;top: 0; left: 0;right: 0; position:relative;display:table;">
                    <div style="display:table-cell; vertical-align: top; float:left;">
                        <img id="representativeImg" src="${representative.imageURL}"
                             style="text-align:center; width: 100px;margin-right: 20px;"></img>
                    </div>
                    <div style="display: table;  margin:0px auto 15px auto; vertical-align: top;">
                        ${raw(representative.description)}
                    </div>
                </div>
            </div>
            <div class="tab-pane" id="tabs-2">
                <div id="tabs-2">
                    <g:if test="${"admin".equals(params.menu)}">
                        <div style="margin: auto;top: 0; left: 0; right: 0; position:relative;display:table;">
                            <div style="display:table-cell;">
                                <button id="votingHistoryButton" type="button" class="btn btn-default"
                                        style="margin:0px 20px 0px 0px; width:300px;">
                                    <g:message code="requestVotingHistoryLbl"/>
                                </button>
                            </div>
                            <div style="display:table-cell;">
                                <button type="button" id="accreditationRequestButton" style="margin:0px 20px 0px 0px; width:300px;"
                                        class="btn btn-default">
                                    <g:message code="requestRepresentativeAcreditationsLbl"/>
                                </button>
                            </div>
                        </div>
                    </g:if>
                </div>
            </div>
        </div>
    </div>
</div>

<g:include view="/include/dialog/anonymousReceiptDialog.gsp"/>
<g:include view="/include/dialog/clientToolAdvertDialog.gsp"/>
<g:include view="/include/dialog/selectRepresentativeDialog.gsp" model="${[representativeName:representativeFullName]}" />
<g:include view="/include/dialog/anonymousRepresentativeDateRangeDialog.gsp"/>
<g:include view="/include/dialog/representativeImageDialog.gsp"/>
<g:include view="/include/dialog/requestRepresentativeVotingHistoryDialog.gsp"/>	
<g:include view="/include/dialog/requestRepresentativeAccreditationsDialog.gsp"/>

</body>
</html>
<r:script>
    $(function() {

        $("#representativeImg").click(function() {
            $("#dialogRepresentativeImg").attr('src',"${representative.imageURL}");
            $("#imageDialog").dialog("open");
        })

        $("#votingHistoryButton").click(function() {
            $("#requestRepresentativeVotingHistoryDialog").dialog("open");
        })

        $("#accreditationRequestButton").click(function() {
            $("#requestRepresentativeAccreditationsDialog").dialog("open");
        })


       $('#reqVotingHistoryForm').submit(function(event){
            event.preventDefault();
            var dateFrom = document.getElementById("dateFrom").getValidatedDate(),
                dateTo = document.getElementById("dateTo").getValidatedDate();
            allFields = $([]).add(dateFrom).add(dateTo);
            allFields.removeClass("formFieldError");
            if(!checkDateRange()) return false
            requestVotingHistory();
       })

       $('#accreditationRequestForm').submit(function(event){
           event.preventDefault();
           requestAccreditations();
        })

    });

    function requestVotingHistory() {
        console.log("requestVotingHistory")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        var dateFromStr =  document.getElementById("dateFrom").getValidatedDate().format()
        var dateToStr = document.getElementById("dateTo").getValidatedDate().format()
        console.log("requestVotingHistory - dateFromStr: " + dateFromStr + " - dateToStr: " + dateToStr)
        webAppMessage.signedContent = {operation:Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST, representativeNif:"${representative.nif}",
                representativeName:"${representativeFullName}", dateFrom:dateFromStr,
                dateTo:dateToStr, email:$("#userEmailText").val()}
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        webAppMessage.serviceURL = "${createLink(controller:'representative', action:'history', absolute:true)}"
        webAppMessage.signedMessageSubject = '<g:message code="requestVotingHistoryLbl"/>'
        webAppMessage.email = $("#userEmailText").val()
        webAppMessage.callerCallback = 'representativeOperationCallback'
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function requestAccreditations() {
        var accreditationDateSelectedStr = document.getElementById("accreditationDateSelected").getValidatedDate().format()
        console.log("requestAccreditations - accreditationDateSelectedStr: " + accreditationDateSelectedStr)
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.REPRESENTATIVE_ACCREDITATIONS_REQUEST)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.signedContent = {operation:Operation.REPRESENTATIVE_ACCREDITATIONS_REQUEST,
                representativeNif:"${representative.nif}", email:$("#accreditationReqUserEmailText").val(),
                representativeName:"${representativeFullName}", selectedDate:accreditationDateSelectedStr}
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        webAppMessage.serviceURL = "${createLink(controller:'representative', action:'accreditations', absolute:true)}"
        webAppMessage.signedMessageSubject = '<g:message code="requestRepresentativeAcreditationsLbl"/>'
        webAppMessage.email = $("#accreditationReqUserEmailText").val()
        webAppMessage.callerCallback = 'representativeOperationCallback'
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function selectRepresentativeCallback(appMessage) {
        console.log("selectRepresentativeCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var caption = '<g:message code="operationERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                if(isAnonymuosDelegation) {
                    showAnonymousReceiptDialog("<g:message code='selectedRepresentativeMsg' args="${[representativeFullName]}"/>",
                        msg)
                    return
                } else {
                    caption = "<g:message code='operationOKCaption'/>"
                    msg = "<g:message code='selectedRepresentativeMsg' args="${[representativeFullName]}"/>";
                }
            } else if (ResponseVS.SC_CANCELLED== appMessageJSON.statusCode) {
                caption = "<g:message code='operationCANCELLEDLbl'/>"
            }
            showResultDialog(caption, msg)
        }
    }

    function representativeOperationCallback(appMessage) {
        console.log("requestAccreditationsCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var caption = '<g:message code="operationERRORCaption"/>'
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "<g:message code='operationOKCaption'/>"
            } else if (ResponseVS.SC_CANCELLED== appMessageJSON.statusCode) {
                caption = "<g:message code='operationCANCELLEDLbl'/>"
            }
            var msg = appMessageJSON.message
            showResultDialog(caption, msg)
        }
    }

</r:script>