<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <meta name="layout" content="main"/>
    </head>
    <body>
    <div class="container">
	    <div class="navbar">   
			<div class="navbar-inner">
			    <div class="container">
			 		<ul class="nav">
						<li><a href="informacion">${message(code: 'infoLabel', null)}</a></li>
						<li class="active"><a href="listaServicios">${message(code: 'servicesListLabel', null)}</a></li>
						<li><a href="datosAplicacion">${message(code: 'appDataLabel', null)}</a></li>
					</ul>
				</div>
			</div>
		</div>
		<div id="content" role="main">
			<g:include controller="infoServidor" />
			<g:include controller="anuladorVoto" />
			<g:include controller="buscador" />
			<g:include controller="certificado" />
			<g:include controller="error400" />
			<g:include controller="error500" />
			<g:include controller="eventoVotacion" />
			<g:include controller="subscripcion" />
			<g:include controller="voto" />
			<g:include controller="mensajeSMIME" />
		</div>
	</div>		
	</body>
</html>
