<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
                <meta name="layout" content="main" />
        <style type="text/css" media="screen">
        	#content a{margin: 0px 100px 0px 0px;}
        	#contentText {margin: 40px 0px 0px 0px;}
        </style>
    </head>
    <body>

	    <div class="container">
	        
		    <div class="navbar">   
				<div class="navbar-inner">
				    <div class="container">
				 		<ul class="nav">
							<li class="active"><a href="informacion">${message(code: 'infoLabel', null)}</a></li>
							<li><a href="listaServicios">${message(code: 'servicesListLabel', null)}</a></li>
							<li><a href="datosAplicacion">${message(code: 'appDataLabel', null)}</a></li>
						</ul>
					</div>
				</div>
			</div>

					
			<div id="content" class="content" role="main">
		           <div class="mainLinkContainer">
		           <div class="mainLink"><a href="${grailsApplication.config.grails.serverURL}/app/index">${message(code: 'mainPageLabel', null)}</a></div>
		           <div class="mainLink"><a href="https://github.com/jgzornoza/SistemaVotacion/tree/master/SistemaVotacionControlAcceso">${message(code: 'sourceCodeLabel', null)}</a></div>
		           <div class="mainLink"><a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Control-de-Acceso">${message(code: 'wikiLabel', null)}</a></div>
		           </div>
		           <p id="contentText">${message(code: 'urlMatch', null)}: <b>${grailsApplication.config.grails.serverURL}</b></p>
			</div>
				
		</div>
		
	</body>
</html>
