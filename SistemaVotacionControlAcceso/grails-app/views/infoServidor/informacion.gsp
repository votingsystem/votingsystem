<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <meta name="layout" content="main" />
        <style type="text/css" media="screen">
        	#content a{margin: 0px 100px 0px 0px;}
        	#contentText {margin: 40px 0px 0px 0px;}
        </style>
        <link rel="stylesheet" href="${resource(dir:'css',file:'tabmenu.css')}" />
    </head>
    <body>
 		<ul id="tabmenu">
			<li><a class="active" href="informacion">${message(code: 'infoLabel', null)}</a></li>
			<li><a href="listaServicios">${message(code: 'servicesListLabel', null)}</a></li>
			<li><a href="datosAplicacion">${message(code: 'appDataLabel', null)}</a></li>
		</ul>
		
		<div id="content" role="main">
            <h1><a href="${grailsApplication.config.grails.serverURL}/app/index">${message(code: 'mainPageLabel', null)}</a>
            <a href="https://github.com/jgzornoza/SistemaVotacionControlAcceso">${message(code: 'sourceCodeLabel', null)}</a>
            <a href="https://github.com/jgzornoza/SistemaVotacionControlAcceso/wiki/Control-de-Acceso">${message(code: 'wikiLabel', null)}</a></h1>
            <p id="contentText">${message(code: 'urlMatch', null)}: <b>${grailsApplication.config.grails.serverURL}</b></p>
		</div>
		
	</body>
</html>
