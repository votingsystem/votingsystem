<!DOCTYPE html>
<html>
    <head>
        <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
   	    <script type="text/javascript">

		function setClienteFirmaMessage(mensaje) {
			return parent.setClienteFirmaMessage(mensaje)
		}

		function obtenerOperacion() {
			return parent.obtenerOperacion()
		}
		
   	    </script>
    </head>
    <body>
        <script src="${resource(dir:'js',file:'deployJava.js')}"></script>
		<script>
			var attributes = {id: 'appletHerramienta', 
        	code:'org.sistemavotacion.herramientavalidacion.AppletHerramienta', mayscript:'true',
            	width:600, height:170} ;
		    <g:if test="${params.gwt}">
        		var parameters = {jnlp_href: 'jnlpHerramienta?gwt=true' } ;
        	</g:if>
        	<g:else>
        		var parameters = {jnlp_href: 'jnlpHerramienta' } ;
        	</g:else>
        	deployJava.runApplet(attributes, parameters, '1.6');
    	</script>
    </body>
</html>