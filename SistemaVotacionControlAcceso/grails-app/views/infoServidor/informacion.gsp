<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
        <link rel="stylesheet" href="${resource(dir:'css',file:'votingSystem.css')}" />
        <style type="text/css" media="screen">
        	#content a{margin: 0px 100px 0px 0px;}
        	#contentText {margin: 40px 0px 0px 0px;}
        	.infoFooter {
			    margin: 0px auto 20px 0;
			    width:100%;
				font-size: 0.7em;
				
			}
			.mailLink {
				width:100px;
				margin:0 auto 0 auto;
			}

        </style>
		  <link rel="stylesheet" href="http://code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css" />
		  <script src="http://code.jquery.com/jquery-1.9.1.js"></script>
		  <script src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script>
        <link rel="stylesheet" href="${resource(dir:'css',file:'jquery-ui-1.10.3.custom.min.css')}">    
		  <script>
		  $(function() {
			  $( "#tabs" ).tabs({
			      beforeLoad: function( event, ui ) {
			    	  ui.panel.html("${votingSystem.tabProgresTemplate()}");
			      }
			    })
		  });
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
			           <p id="contentText">${message(code: 'urlMatch', null)}: <b>${grailsApplication.config.grails.serverURL}</b></p>
					</div>
			  	</div>
			  </div>
			</div>
			<div class="infoFooter">
				<div class="mailLink">
					<a href="mailto:${grailsApplication.config.SistemaVotacion.emailAdmin}">${message(code: 'emailLabel', null)}</a>
				</div>
		   	</div>	
		<div>
	</body>
</html>
