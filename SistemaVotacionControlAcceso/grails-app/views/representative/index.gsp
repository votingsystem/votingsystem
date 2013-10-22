<% def representativeFullName = representative.nombre + " " + representative.primerApellido %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
	<meta name="layout" content="main" />
  <script>
	  	$(function() {
			var pendingOperation

		    $("#dateFrom").datepicker(pickerOpts);
		    $("#dateTo").datepicker(pickerOpts);
		    $("#accreditationDateSelected").datepicker(pickerOpts);
		    
		  	$( "#tabs" ).tabs({
			      beforeLoad: function( event, ui ) {
			    	  ui.panel.html("${votingSystem.tabProgresTemplate()}");
			      }
		    })

		    $("#representativeImg").click(function() {
		    	$("#dialogRepresentativeImg").attr('src',"${representative.imageURL}");
		    	$("#imageDialog").dialog("open");
			})
		    
   		    $("#votingHistoryButton").click(function() {
		    	$("#reqVotingHistoryDialog").dialog("open");
			})
		    
   		    $("#accreditationRequestButton").click(function() {
		    	$("#reqAccreditationDialog").dialog("open");
			})
		    
   		    $("#selectRepresentativeButton").click(function() {
		    	$("#selectRepresentativeDialog").dialog("open");
			})
		    
		    
		   $('#reqVotingHistoryForm').submit(function(event){
		 		event.preventDefault();
				var dateFrom = $("#dateFrom"),
					dateTo = $("#dateTo"); 
	        	allFields = $( [] ).add(dateFrom).add(dateTo);
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

		   
		   
   		   $("#selectRepresentativeDialog").dialog({
   			   	  width: 500, autoOpen: false, modal: true,
   			      buttons: [{id: "acceptButton",
   			        		text:"<g:message code="acceptLbl"/>",
   			               	icons: { primary: "ui-icon-check"},
   			             	click:function() {
   			             		selectRepresentative(); 
   			             		$(this).dialog( "close" );  	   			   				
   	   			        	}}, {	
   	  	   			        	id: "cancelButton",
   	   			        		text:"<g:message code="cancelLbl"/>",
   	   			               	icons: { primary: "ui-icon-closethick"},
   	   			             	click:function() {
   	  	   			   					$(this).dialog( "close" );
   	  	   			       	 		}}],
   			      show: {effect:"fade", duration: 300},
   			      hide: {effect: "fade",duration: 300}
		    });
		    
		    
   		   $("#imageDialog").dialog({
   			   	  width: 500, autoOpen: false, modal: true,
   			      buttons: [{id: "acceptButton",
   			        		text:"<g:message code="acceptLbl"/>",
   			               	icons: { primary: "ui-icon-check"},
   			             	click:function() {
   			             		$(this).dialog( "close" );   	   			   				
   	   			        	}}],
   			      show: {effect:"fade", duration: 300},
   			      hide: {effect: "fade",duration: 300}
		    });

   		   $("#reqAccreditationDialog").dialog({
   			   	  width: 500, autoOpen: false, modal: true,
   			      buttons: [{id: "acceptButton",
   			        		text:"<g:message code="acceptLbl"/>",
   			               	icons: { primary: "ui-icon-check"},
   			             	click:function() {
   			             		$("#submitAccreditationRequest").click()  	   			   				
   	   			        	}}, {id: "cancelButton",
   	   			        		text:"<g:message code="cancelLbl"/>",
   	   			               	icons: { primary: "ui-icon-closethick"},
   	   			             	click:function() {
   	  	   			   					$(this).dialog( "close" );
   	  	   			       	 		}}],
   			      show: {effect:"fade", duration: 300},
   			      hide: {effect: "fade",duration: 300}
		    });
		    
   		   $("#reqVotingHistoryDialog").dialog({
			   	  width: 550, autoOpen: false, modal: true,
			      buttons: [{id: "acceptButton",
			        		text:"<g:message code="acceptLbl"/>",
			               	icons: { primary: "ui-icon-check"},
			             	click:function() {
			             		$("#submitVotingHistoryRequest").click() 	   			   				
	   			        	}}, {id: "cancelButton",
   	   			        		text:"<g:message code="cancelLbl"/>",
   	   			               	icons: { primary: "ui-icon-closethick"},
   	   			             	click:function() {
   	  	   			   					$(this).dialog( "close" );
   	  	   			       	 		}}],
			      show: {effect:"fade", duration: 300},
			      hide: {effect: "fade",duration: 300}
		    });
	    
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
			votingSystemApplet.setMessageToSignatureClient(JSON.stringify(webAppMessage)); 
		}

		function requestVotingHistory() {
			console.log("requestVotingHistory")
			pendingOperation = Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST
	    	var webAppMessage = new WebAppMessage(StatusCode.SC_PROCESANDO, pendingOperation)
	    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
        	var dateFromStr = $("#dateForm").val() + " 00:00:00"
        	var dateToStr = $("#dateTo").val() + " 00:00:00"
        	console.log("requestVotingHistory - dateFromStr: " + dateFromStr + " - dateToStr: " + dateToStr)
			webAppMessage.contenidoFirma = {operation:Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST, representativeNif:"${representative.nif}",
    				representativeName:"${representativeFullName}", dateForm:dateFromStr, 
    				dateTo:dateToStr, email:$("#userEmailText").val()}
			webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
			webAppMessage.urlEnvioDocumento = "${createLink(controller:'representative', action:'history', absolute:true)}"
			webAppMessage.asuntoMensajeFirmado = '<g:message code="requestVotingHistoryLbl"/>'
			webAppMessage.emailSolicitante = $("#userEmailText").val()
			webAppMessage.respuestaConRecibo = true
			votingSystemApplet.setMessageToSignatureClient(JSON.stringify(webAppMessage)); 
		}

		function requestAccreditations() {
			var accreditationDateSelectedStr = $("#accreditationDateSelected").val() + " 00:00:00"
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
			votingSystemApplet.setMessageToSignatureClient(JSON.stringify(webAppMessage)); 
		}
	
		function setMessageFromSignatureClient(appMessage) {
			console.log("setMessageFromSignatureClient - message from native client: " + appMessage);
			$("#loadingVotingSystemAppletDialog").dialog("close");
			if(appMessage != null) {
				votingSystemAppletLoaded = true;
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
							msgTemplate = "<g:message code='representativeFullName'/>";
							msg = msgTemplate.format(representativeFullName);
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
		</div>
	
	
     <div id="selectRepresentativeDialog" title="<g:message code="selectRepresentativeLbl"/>" style="padding:20px 20px 20px 20px;">
		<% def msgParams = [representativeFullName]%>
		<g:message code="selectRepresentativeMsg" args='${msgParams}'/>
		<p style="text-align:center;"><g:message code="clickAcceptToContinueLbl" args='${msgParams}'/></p>
    </div> 
		
    <div id="imageDialog" title="" style="padding:20px 20px 20px 20px">
		<img id="dialogRepresentativeImg" style="width:100%; height: 100%;"></img>
    </div> 
    
     <div id="reqVotingHistoryDialog" title="<g:message code="requestVotingHistoryLbl"/>" style="padding:20px 20px 20px 20px">
		<g:message code="representativeHistoryRequestMsg"/>
		
		<label><g:message code="selectDateRangeMsg"/></label>
		
		<form id="reqVotingHistoryForm">
			<div style="display:table;margin:20px 0px 0px 0px;">
				<div style="display:table-cell;margin:0px 0px 0px 20px;">
					<label for="dateFrom" style="margin:0px 0px 0px 3px;width:250px;display:block;"><g:message code="firstDaterangeLbl"/></label>
					<input type="text" id="dateFrom" style="width:200px;" readonly required
					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
					onchange="this.setCustomValidity('')"/>
				</div>
				<div style="display:table-cell;margin:0px 0px 0px 20px;">
					<label for="dateTo" style="margin:0px 0px 0px 3px;width:250px;display:block;"><g:message code="dateToLbl"/></label>
					<input type="text" id="dateTo" style="width:200px;" readonly required
					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
					onchange="this.setCustomValidity('')"/>
				</div>
			</div>
			<div style="margin:15px 0px 20px 0px">
		    	<label for="userEmailText"><g:message code="enterEmailLbl"/></label>
				<input type="email" id="userEmailText" style="width:350px; margin:0px auto 0px auto;" required
						oninvalid="this.setCustomValidity('<g:message code="emailERRORMsg"/>')"
			   			onchange="this.setCustomValidity('')"/>
			</div>
			<input id="submitVotingHistoryRequest" type="submit" style="display:none;">
		</form> 	   
    </div> 
		
     <div id="reqAccreditationDialog" title="<g:message code="requestRepresentativeAcreditationsLbl"/>" style="padding:20px 20px 20px 20px">
		<g:message code="accreditationRequestMsg"/>
		<form id="accreditationRequestForm">
			<div style="display:table-cell;margin:0px 0px 0px 20px;">
				<label for="accreditationDateSelected" style="margin:0px 0px 0px 3px;width:250px;display:block;"><g:message code="dateRequestLbl"/></label>
				<input type="text" id="accreditationDateSelected" style="width:200px;" readonly required 
				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
				onchange="this.setCustomValidity('')"/>
			</div>
			<div style="margin:15px 0px 20px 0px">
		    	<label for="accreditationReqUserEmailText"><g:message code="enterEmailLbl"/></label>
				<input type="email" id="accreditationReqUserEmailText" style="width:350px; margin:0px auto 0px auto;" required
						oninvalid="this.setCustomValidity('<g:message code="emailERRORMsg"/>')"
			   			onchange="this.setCustomValidity('')"/>
			</div>			
			<input id="submitAccreditationRequest" type="submit" style="display:none;">
		</form>
    </div> 	  
	
	
	

	
	</div>
</body>
</html>