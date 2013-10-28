<html>
<head>
        <meta name="layout" content="main" />
        <script type="text/javascript">
        	
		 	$(function() {
		 		document.getElementById('userNifText').addEventListener('change', nifValidation, false);

			 	$("#editRepresentativeButton").click(function() {
		 			$("#userNifText").val("");
			 		$("#editRepresentativeDialogFormDiv").show()
					$("#editRepresentativeDialogProgressDiv").hide()
			 		$("#acceptButton").button("enable");
			 		$("#cancelButton").button("enable");			 	
	 			  	$("#editRepresentativeDialog").dialog("open");
				 })

	    		  $("#removeRepresentativeDialog").dialog({
	   			   	  width: 450, autoOpen: false, modal: true,
	   			      buttons: [{
	   			        		text:"<g:message code="acceptLbl"/>",
	   			               	icons: { primary: "ui-icon-check"},
	   			             	click:function() {
	   			             		$("#removeRepresentativeDialog").dialog("close");
	       			             	removeRepresentative() 
	       			             }},{
	   			        		text:"<g:message code="cancelLbl"/>",
	   			               	icons: { primary: "ui-icon-closethick"},
	   			             	click:function() {
	  	   			   				$(this).dialog( "close" );
	  	   			       	 	}}],
	   			      show: { effect: "fade", duration: 100 },
	   			      hide: { effect: "fade", duration: 100 }
	   			    });

	      		   $("#editRepresentativeDialog").dialog({
	    			   	  width: 450, autoOpen: false, modal: true,
	    			      buttons: [{id: "acceptButton",
	    			        		text:"<g:message code="acceptLbl"/>",
	    			               	icons: { primary: "ui-icon-check"},
	    			             	click:function() {
	    			             		$("#submitNifCheck").click() 
	    			             		//$(this).dialog( "close" );   	   			   				
	    	   			        	}}, {id: "cancelButton",
	    			        		text:"<g:message code="cancelLbl"/>",
	    			               	icons: { primary: "ui-icon-closethick"},
	    			             	click:function() {
	   	   			   					$(this).dialog( "close" );
	   	   			       	 		}	
	    			           }],
	    			      show: {effect:"fade", duration: 300},
	    			      hide: {effect: "fade",duration: 300}
	    			    });

		   		    $('#editRepresentativeForm').submit(function(event){	
				 		console.log("editRepresentativeForm")
				 		event.preventDefault();
				 		$("#acceptButton").button("disable");
				 		$("#cancelButton").button("disable");
				 		$("#editRepresentativeDialogFormDiv").hide()
				 		$("#editRepresentativeDialogProgressDiv").fadeIn(500)
				 		var urlRequest = "${createLink(controller:'representative')}/edit/" + $("#userNifText").val()
				 		console.log(" - editRepresentative - urlRequest: " + urlRequest)
						$.ajax({///user/$nif/representative
							contentType:'application/json',
							url: urlRequest
						}).done(function(resultMsg) {
							window.location.href = urlRequest
						}).error(function(resultMsg) {
							showResultDialog('<g:message code="errorLbl"/>',resultMsg.responseText) 							
						}).always(function(resultMsg) {
							$("#editRepresentativeDialog").dialog("close");
						});

					 })
	    		  
		    		$("#removeRepresentativeButton").click(function () { 
		    			$("#removeRepresentativeDialog").dialog("open");
		    		});
			 });

		 	var nifValidation = function () {
				var nifInput = document.getElementById('userNifText')
				var validationResult = validateNIF(nifInput.value)
				console.log("validateNIF result: " + validationResult)
				if (!validationResult) {
					document.getElementById('userNifText').setCustomValidity("<g:message code='nifERRORMsg'/>");
				}
			}

	 		function removeRepresentative() {
		 		console.log("removeRepresentative")
		    	var webAppMessage = new WebAppMessage(
				    	StatusCode.SC_PROCESANDO, 
				    	Operation.REPRESENTATIVE_REVOKE)
		    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
	    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
				webAppMessage.contenidoFirma = {operation:Operation.REPRESENTATIVE_REVOKE}
				webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
				webAppMessage.urlEnvioDocumento = "${createLink(controller:'representative', action:'revoke', absolute:true)}"
				webAppMessage.asuntoMensajeFirmado = '<g:message code="removeRepresentativeMsgSubject"/>'
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
						if(StatusCode.SC_OK == statusCode) { 
							caption = '<g:message code="operationOKCaption"/>'
								msg = "<g:message code='removeRepresentativeOKMsg'/>";
						}
						showResultDialog(caption, msg)
					}
				}
			}
        </script>

</head>
<body>
	<div id="contentDiv" style="position:relative; height:700px;">
	
		<div style="width: 50%;height: 50%;overflow: auto;margin: auto;top: 0; left: 0; bottom: 0; right: 0;">
			<votingSystem:simpleButton href="${createLink(controller:'representative', action:'newRepresentative')}"
				imgSrc="${resource(dir:'images',file:'newRepresentative.png')}" style="margin:10px 20px 0px 0px; width:400px;">
					<g:message code="newRepresentativeLbl"/>
			</votingSystem:simpleButton>
			<votingSystem:simpleButton id="removeRepresentativeButton" style="margin:10px 20px 0px 0px; width:400px;"
				imgSrc="${resource(dir:'images',file:'removeRepresentative.png')}">
				<g:message code="removeRepresentativeLbl"/>
			</votingSystem:simpleButton>
			<votingSystem:simpleButton  id="editRepresentativeButton" style="margin:10px 20px 0px 0px; width:400px;"
					imgSrc="${resource(dir:'images',file:'editRepresentative.png')}">
					<g:message code="editRepresentativeLbl"/>
			</votingSystem:simpleButton>
		</div>
	</div>
    
    <div id="removeRepresentativeDialog" title="<g:message code="removeRepresentativeLbl"/>"  style="padding:20px 20px 20px 20px">
		<p style="text-align: center;"><g:message code="removeRepresentativeMsg"/></p>
		<p style="text-align: center;"><g:message code="clickAcceptToContinueLbl"/></p>
    </div> 
    
    
    <div id="editRepresentativeDialog" title="<g:message code="editRepresentativeLbl"/>" style="margin:20px auto 20px auto;">	
		<div id="editRepresentativeDialogFormDiv" style="margin:0px auto 0px 20px;">
			<form id="editRepresentativeForm">
		    	<label for="userNifText" style="margin:0px 0px 20px 0px"><g:message code="nifForEditRepresentativeLbl"/></label>
				<input type="text" id="userNifText" style="width:350px; margin:0px auto 0px auto;" required
					oninvalid="this.setCustomValidity('<g:message code="nifERRORMsg"/>')"
		   			onchange="this.setCustomValidity('')"/>
				<input id="submitNifCheck" type="submit" style="display:none;">
			</form>
		</div>
		<div id="editRepresentativeDialogProgressDiv" style="display:none;">
			<p style='text-align: center;'><g:message code="checkingDataLbl"/></p>
			<progress style='display:block;margin:0px auto 10px auto;'></progress>
		</div>
	</div>
    
</body>
</html>