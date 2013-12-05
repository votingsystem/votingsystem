<% def representativeFullName = representative?.name + " " + representative?.firstName %>
<html>
<head>
	<meta name="layout" content="main" />
</head>
<body>
<div id="contentDiv" style="display:block;position:relative; margin: 0px 0px 30px 0px;min-height: 700px;">	
	<div>
		<div style="margin:0px auto 15px 0px; position:relative;display:table;">
			<div style="display:table-cell;">
				<votingSystem:simpleButton id="selectRepresentativeButton"
					imgSrc="${resource(dir:'images/fatcow_16',file:'accept.png')}" style="margin:0px 20px 0px 30px;">
						<g:message code="selectRepresentativeLbl"/>
				</votingSystem:simpleButton>
			</div>
			<div class="representativeNameHeader">
				<div>${representativeFullName}</div>
			</div>
			<div  class="representativeNumRepHeader">
				<div>  ${representative.numRepresentations} <g:message code="numDelegationsPartMsg"/></div>
			</div>
		</div>
	</div>
	<div id="tabs" style="min-height: 700px; margin: 0 30px 0 30px;">
		    <ul>
			    <li><a href="#tabs-1" style="font-size: 0.8em;"><g:message code='profileLbl'/></a></li>
			    <li><a href="#tabs-2" style="font-size: 0.8em;"><g:message code='votingHistoryLbl'/></a></li>
		  	</ul>
			<div id="tabs-1">
				<div style="width: 90%;margin: auto;top: 0; left: 0;right: 0; position:relative;display:table;">
					<div style="display:table-cell; vertical-align: top; float:left;">
						<img id="representativeImg" src="${representative.imageURL}" style="text-align:center; width: 200px;"></img>
					</div>
					<div style="display: table;  margin:0px auto 15px auto; vertical-align: top;">
                        ${raw(representative.description)}
					</div>
				</div>
			</div>
			<div id="tabs-2">
				<div style="margin: auto;top: 0; left: 0; right: 0; position:relative;display:table;">
					<div style="display:table-cell;">
						<votingSystem:simpleButton id="votingHistoryButton" style="margin:0px 20px 0px 0px; width:300px;">
								<g:message code="requestVotingHistoryLbl"/>
						</votingSystem:simpleButton>
					</div>
					<div style="display:table-cell;">
						<votingSystem:simpleButton id="accreditationRequestButton" style="margin:0px 20px 0px 0px; width:300px;">
								<g:message code="requestRepresentativeAcreditationsLbl"/>
						</votingSystem:simpleButton>
					</div>
				</div>
			</div>
		</div>

</div>

<g:include view="/include/dialog/selectRepresentativeDialog.gsp" model="${[representativeName:representativeFullName]}" />	
<g:include view="/include/dialog/representativeImageDialog.gsp"/>	
<g:include view="/include/dialog/requestRepresentativeVotingHistoryDialog.gsp"/>	
<g:include view="/include/dialog/requestRepresentativeAccreditationsDialog.gsp"/>	

	<div id="tabProgressTemplate" style="display:none;">
		<g:include view="/include/tabProgress.gsp"/>
	</div> 
</body>
</html>
<r:script>
	  	$(function() {
		  	$( "#tabs" ).tabs({
			      beforeLoad: function( event, ui ) {
			    	  ui.panel.html($('#tabProgressTemplate').html());
			      }
		    })

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
		    
   		    $("#selectRepresentativeButton").click(function() {
		    	$("#selectRepresentativeDialog").dialog("open");
			})
		    
		    
		   $('#reqVotingHistoryForm').submit(function(event){
		 		event.preventDefault();
				var dateFrom = $("#dateFrom"),
					dateTo = $("#dateTo"); 
	        	allFields = $([]).add(dateFrom).add(dateTo);
				allFields.removeClass("formFieldError");

				if(dateFrom.datepicker("getDate") > 
					dateTo.datepicker("getDate")) {
					showResultDialog("${message(code:'dataFormERRORLbl')}",'<g:message code="dateRangeERRORMsg"/>')
					dateFrom.addClass("formFieldError");
					dateTo.addClass("formFieldError");
					return false
				}
				requestVotingHistory();
		   })
		   
		   $('#accreditationRequestForm').submit(function(event){
			   event.preventDefault();
			   requestAccreditations();
			})
	  	}); 


		function selectRepresentative() {
			console.log("selectRepresentative")
	    	var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.REPRESENTATIVE_SELECTION)
	    	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
    		webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
			webAppMessage.signedContent = {operation:Operation.REPRESENTATIVE_SELECTION, representativeNif:"${representative.nif}",
    				representativeName:"${representativeFullName}"}
			webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStampVS', absolute:true)}"
			webAppMessage.receiverSignServiceURL = "${createLink(controller:'representative', action:'userSelection', absolute:true)}"
			webAppMessage.signedMessageSubject = '<g:message code="requestRepresentativeAcreditationsLbl"/>'
			webAppMessage.isResponseWithReceipt = true
			votingSystemClient.setMessageToSignatureClient(webAppMessage, selectRepresentativeCallback); 
		}

		function requestVotingHistory() {
			console.log("requestVotingHistory")
	    	var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST)
	    	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
    		webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        	var dateFromStr = $("#dateFrom").datepicker('getDate').format()
        	var dateToStr = $("#dateTo").datepicker('getDate').format() 
        	console.log("requestVotingHistory - dateFromStr: " + dateFromStr + " - dateToStr: " + dateToStr)
			webAppMessage.signedContent = {operation:Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST, representativeNif:"${representative.nif}",
    				representativeName:"${representativeFullName}", dateFrom:dateFromStr,
    				dateTo:dateToStr, email:$("#userEmailText").val()}
			webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStampVS', absolute:true)}"
			webAppMessage.receiverSignServiceURL = "${createLink(controller:'representative', action:'history', absolute:true)}"
			webAppMessage.signedMessageSubject = '<g:message code="requestVotingHistoryLbl"/>'
			webAppMessage.email = $("#userEmailText").val()
			votingSystemClient.setMessageToSignatureClient(webAppMessage, representativeOperationCallback); 
		}

		function requestAccreditations() {
			var accreditationDateSelectedStr = $("#accreditationDateSelected").datepicker('getDate').format()
			console.log("requestAccreditations - accreditationDateSelectedStr: " + accreditationDateSelectedStr)
	    	var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.REPRESENTATIVE_ACCREDITATIONS_REQUEST)
	    	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
    		webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
			webAppMessage.signedContent = {operation:Operation.REPRESENTATIVE_ACCREDITATIONS_REQUEST,
    				representativeNif:"${representative.nif}", email:$("#accreditationReqUserEmailText").val(),
    				representativeName:"${representativeFullName}", selectedDate:accreditationDateSelectedStr}
			webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStampVS', absolute:true)}"
			webAppMessage.receiverSignServiceURL = "${createLink(controller:'representative', action:'accreditations', absolute:true)}"
			webAppMessage.signedMessageSubject = '<g:message code="requestRepresentativeAcreditationsLbl"/>'
			webAppMessage.email = $("#accreditationReqUserEmailText").val()
			votingSystemClient.setMessageToSignatureClient(webAppMessage, representativeOperationCallback); 
		}

		function selectRepresentativeCallback(appMessage) {
			console.log("selectRepresentativeCallback - message from native client: " + appMessage);
			var appMessageJSON = toJSON(appMessage)
			if(appMessageJSON != null) {
				$("#workingWithAppletDialog" ).dialog("close");
				var caption = '<g:message code="operationERRORCaption"/>'
				var msg = appMessageJSON.message
				if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
					caption = "<g:message code='operationOKCaption'/>"
					msg = "<g:message code='selectedRepresentativeMsg' args="${[representativeFullName]}"/>";
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
				$("#workingWithAppletDialog" ).dialog("close");
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