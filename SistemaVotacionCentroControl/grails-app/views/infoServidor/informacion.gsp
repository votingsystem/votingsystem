<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
        <g:include controller="app" action="jsUtils" />
        <style type="text/css" media="screen">
        	#content a{margin: 0px 100px 0px 0px;}
        </style>  
		  <script>
		  $(function() {
			  $( "#tabs" ).tabs({
			      beforeLoad: function( event, ui ) {
			    	  ui.panel.html("${votingSystem.tabProgresTemplate()}");
			      }
			    })

	    		$("#validationToolLink").click(function () { 
			    	var webAppMessage = new WebAppMessage(StatusCode.SC_PROCESANDO, 
					    	Operation.MENSAJE_HERRAMIENTA_VALIDACION)
	    			votingSystemApplet.setMessateToValidationTool(JSON.stringify(webAppMessage))
	    		});
		  });

			function setMessageFromValidationTool(appMessage) {
				console.log("setMessageFromValidationTool: " + appMessage);
				$("#loadingVotingSystemAppletDialog").dialog("close");
				if(appMessage != null) {
					validationToolAppletLoaded = true;
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
		  </script>
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
				<div class="mailLink" style="width:100px;margin:0 auto 0 auto;">
					<a href="mailto:${grailsApplication.config.SistemaVotacion.emailAdmin}">${message(code: 'emailLabel', null)}</a>
				</div>
		   	</div>	
		<div>
		<iframe id="validationToolAppletFrame" src="" style="visibility:hidden;width:0px; height:0px;"></iframe>
	</body>
</html>
