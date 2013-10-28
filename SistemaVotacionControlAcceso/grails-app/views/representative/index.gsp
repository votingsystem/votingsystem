<% def representativeFullName = representative?.nombre + " " + representative?.primerApellido %>
<html>
<head>
	<meta name="layout" content="main" />
  <script>
	  	$(function() {
			var pendingOperation
		  	$( "#tabs" ).tabs({
			      beforeLoad: function( event, ui ) {
			    	  ui.panel.html("${render(template:'/template/tabProgres').replace("\n","")}");
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
				allFields.removeClass("ui-state-error");

				if(dateFrom.datepicker("getDate") > 
					dateTo.datepicker("getDate")) {
					showResultDialog("${message(code:'dataFormERRORLbl')}",
							'<g:message code="dateRangeERRORMsg"/>') 
					dateFrom.addClass("ui-state-error");
					dateTo.addClass("ui-state-error");
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
			pendingOperation = Operation.REPRESENTATIVE_SELECTION
	    	var webAppMessage = new WebAppMessage(StatusCode.SC_PROCESANDO, pendingOperation)
	    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
			webAppMessage.contenidoFirma = {operation:Operation.REPRESENTATIVE_SELECTION, representativeNif:"${representative.nif}",
    				representativeName:"${representativeFullName}"}
			webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
			webAppMessage.urlEnvioDocumento = "${createLink(controller:'representative', action:'userSelection', absolute:true)}"
			webAppMessage.asuntoMensajeFirmado = '<g:message code="requestRepresentativeAcreditationsLbl"/>'
			webAppMessage.respuestaConRecibo = true
			votingSystemClient.setMessageToSignatureClient(JSON.stringify(webAppMessage)); 
		}

		function requestVotingHistory() {
			console.log("requestVotingHistory")
			pendingOperation = Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST
	    	var webAppMessage = new WebAppMessage(StatusCode.SC_PROCESANDO, pendingOperation)
	    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
        	var dateFromStr = $("#dateFrom").datepicker('getDate').format()
        	var dateToStr = $("#dateTo").datepicker('getDate').format() 
        	console.log("requestVotingHistory - dateFromStr: " + dateFromStr + " - dateToStr: " + dateToStr)
			webAppMessage.contenidoFirma = {operation:Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST, representativeNif:"${representative.nif}",
    				representativeName:"${representativeFullName}", dateForm:dateFromStr, 
    				dateTo:dateToStr, email:$("#userEmailText").val()}
			webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
			webAppMessage.urlEnvioDocumento = "${createLink(controller:'representative', action:'history', absolute:true)}"
			webAppMessage.asuntoMensajeFirmado = '<g:message code="requestVotingHistoryLbl"/>'
			webAppMessage.emailSolicitante = $("#userEmailText").val()
			webAppMessage.respuestaConRecibo = true
			votingSystemClient.setMessageToSignatureClient(JSON.stringify(webAppMessage)); 
		}

		function requestAccreditations() {
			var accreditationDateSelectedStr = $("#accreditationDateSelected").val()
			console.log("requestAccreditations - accreditationDateSelectedStr: " + accreditationDateSelectedStr)
			pendingOperation = Operation.REPRESENTATIVE_ACCREDITATIONS_REQUEST
	    	var webAppMessage = new WebAppMessage(StatusCode.SC_PROCESANDO, pendingOperation)
	    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
			webAppMessage.contenidoFirma = {operation:Operation.REPRESENTATIVE_ACCREDITATIONS_REQUEST, 
    				representativeNif:"${representative.nif}", email:$("#accreditationReqUserEmailText").val(),
    				representativeName:"${representativeFullName}", selectedDate:accreditationDateSelectedStr}
			webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
			webAppMessage.urlEnvioDocumento = "${createLink(controller:'representative', action:'accreditations', absolute:true)}"
			webAppMessage.asuntoMensajeFirmado = '<g:message code="requestRepresentativeAcreditationsLbl"/>'
			webAppMessage.emailSolicitante = $("#accreditationReqUserEmailText").val()
			webAppMessage.respuestaConRecibo = true
			votingSystemClient.setMessageToSignatureClient(JSON.stringify(webAppMessage)); 
		}
	
		function setMessageFromSignatureClient(appMessage) {
			console.log("setMessageFromSignatureClient - message from native client: " + appMessage);
			$("#loadingVotingSystemAppletDialog").dialog("close");
			if(appMessage != null) {
				signatureClientToolLoaded = true;
				var appMessageJSON
				if( Object.prototype.toString.call(appMessage) == '[object String]' ) {
					appMessageJSON = JSON.parse(appMessage);
				} else {
					appMessageJSON = appMessage
				} 
				var statusCode = appMessageJSON.codigoEstado
				if(StatusCode.SC_PROCESANDO == statusCode){
					$("#loadingVotingSystemAppletDialog").dialog("close");
					$("#workingWithAppletDialog").dialog("open");
				} else {
					$("#workingWithAppletDialog" ).dialog("close");
					var caption = '<g:message code="operationERRORCaption"/>'
					var msg = appMessageJSON.mensaje
					var msgTemplate
					if(StatusCode.SC_OK == statusCode) { 
						caption = '<g:message code="operationOKCaption"/>'
						if(pendingOperation == Operation.REPRESENTATIVE_SELECTION)  {
							msg = "<g:message code='selectedRepresentativeMsg' args="${[representativeFullName]}"/>";
						}
					}
					showResultDialog(caption, msg)
				}
			}
		}
	  	
  </script>
</head>
<body>
<div id="contentDiv" style="display:block;position:relative; margin: 0px 0px 30px 0px;min-height: 700px;">	
	<div>
		<div style="margin:0px auto 15px 0px; position:relative;display:table;">
			<div style="display:table-cell;">
				<votingSystem:simpleButton id="selectRepresentativeButton"
					imgSrc="${resource(dir:'images',file:'accept_16x16.png')}" style="margin:0px 20px 0px 0px;">
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
	<div id="tabs" style="min-height: 700px;">
		    <ul>
			    <li><a href="#tabs-1"><g:message code='profileLbl'/></a></li>
			    <li><a href="#tabs-2"><g:message code='votingHistoryLbl'/></a></li>
		  	</ul>
			<div id="tabs-1">
				<div style="width: 90%;margin: auto;top: 0; left: 0;right: 0; position:relative;display:table;">
					<div style="display:table-cell; vertical-align: top; float:left;">
						<img id="representativeImg" src="${representative.imageURL}" style="text-align:center; width: 200px;"></img>
					</div>
					<div style="display:table-cell;margin:0px 15px 15px 20px; vertical-align: top;text-align:center;">
						${representative.info} 
					</div>
				</div>
			</div>
			<div id="tabs-2">
				<div style="width: 90%;margin: auto;top: 0; left: 0; right: 0; position:relative;display:table;">
					<div style="display:table-cell;">
						<votingSystem:simpleButton id="votingHistoryButton"
							imgSrc="${resource(dir:'images',file:'requestRepresentativeVotingHistory.png')}" style="margin:0px 20px 0px 0px;">
								<g:message code="requestVotingHistoryLbl"/>
						</votingSystem:simpleButton>
					</div>
					<div style="display:table-cell;">
						<votingSystem:simpleButton id="accreditationRequestButton"
							imgSrc="${resource(dir:'images',file:'requestRepresentativeAccreditations.png')}" style="margin:0px 20px 0px 0px;">
								<g:message code="requestRepresentativeAcreditationsLbl"/>
						</votingSystem:simpleButton>
					</div>
				</div>
			</div>
		</div>

</div>
<g:render template="/template/dialog/selectRepresentativeDialog" model="${[representativeName:representativeFullName]}" />	
<g:render template="/template/dialog/representativeImageDialog"/>	
<g:render template="/template/dialog/requestRepresentativeVotingHistoryDialog"/>	
<g:render template="/template/dialog/requestRepresentativeAccreditationsDialog"/>	


</body>
</html>