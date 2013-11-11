<html>
    <head>
   		<r:require modules="application"/>
	    <r:layoutResources/>
        <style type="text/css" media="screen">
        	#content a{margin: 0px 100px 0px 0px;}
        </style>  
    </head>
    <body>
		<div class="container">
        	<a class="headerTitle" href="${grailsApplication.config.grails.serverURL}">${message(code: 'nombreServidorLabel', null)}</a>
			<div id="tabs" style="min-height: 700px;">
			  <ul>
			    <li><a href="#tabs-1"><span><g:message code="infoLabel"/></span></a></li>
			    <li><a href="listaServicios"><span><g:message code="serviceURLSMsg"/></span></a></li>
			    <li><a href="datosAplicacion"><span><g:message code="appDataLabel"/></span></a></li>
			  </ul>
			  <div id="tabs-1">
			  	<div class="container"  style="height:100%;">
			  		<div id="content" class="content">
			           <div class="mainLinkContainer">
				           <div class="mainLink"><a href="${grailsApplication.config.grails.serverURL}/">${message(code: 'mainPageLabel', null)}</a></div>
				           <div class="mainLink"><a href="https://github.com/jgzornoza/SistemaVotacion/tree/master/SistemaVotacionControlAcceso">${message(code: 'sourceCodeLabel', null)}</a></div>
				           <div class="mainLink"><a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Control-de-Acceso">${message(code: 'wikiLabel', null)}</a></div>
			           </div>
			           <p id="contentText" style="margin: 40px 0px 0px 0px;">${message(code: 'urlMatch', null)}: <b>${grailsApplication.config.grails.serverURL}</b></p>
			           <p>
       						<img src="${resource(dir:'images',file:'password_22x22.png')}"></img>
           					<a id="validationToolLink" class="appLink" style="color: #09287e;">
								<g:message code="validationToolLinkText"/>
							</a>
			           </p>
			           
					</div>
			  	</div>
			  </div>
			</div>
			<div class="infoFooter" style="margin: 0px auto 20px 0;width:100%;font-size: 0.7em;">
				<p style="text-align: center;">
					<a  class="appLink" href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}">
						${message(code: 'emailLabel', null)}</a>
				</p>	
		   	</div>	
		<div>
		<iframe id="validationToolAppletFrame" src="" style="visibility:hidden;width:0px; height:0px;"></iframe>
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

	    		$("#validationToolLink").click(function () { 
			    	var webAppMessage = new WebAppMessage(StatusCode.SC_PROCESANDO, 
					    	Operation.MENSAJE_HERRAMIENTA_VALIDACION)
			    	votingSystemClient.setMessageToValidationTool(JSON.stringify(webAppMessage))
	    		});
		  });

			function setMessageFromValidationTool(appMessage) {
				console.log("setMessageFromValidationTool: " + appMessage);
				$("#loadingVotingSystemAppletDialog").dialog("close");
				if(appMessage != null) {
					validationToolLoaded = true;
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
					} else if(StatusCode.SC_CANCELADO == statusCode) {
						$("#workingWithAppletDialog" ).dialog("close");
					}
				}
				
				
			}
</r:script>
<r:layoutResources/>