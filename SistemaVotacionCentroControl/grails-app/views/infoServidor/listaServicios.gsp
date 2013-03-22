<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <meta name="layout" content="main"/>
    </head>
    <body>
    	<div class="container">
   	    
	   	<ul class="nav nav-tabs">
			<li><a href="informacion"><g:message code="infoLabel"/></a></li>
			<li class="active"><a href="listaServicios"><g:message code="serviceURLSMsg"/></a></li>
			<li><a href="datosAplicacion"><g:message code="appDataLabel"/></a></li>
		</ul>

		<div id="content" role="main">
			<g:include controller="restDoc"/>
		</div>
	</div>		
	</body>
</html>
