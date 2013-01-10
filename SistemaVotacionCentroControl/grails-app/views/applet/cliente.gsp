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
			var attributes = {id: 'appletFirma', 
        	code:'org.sistemavotacion.AppletFirma', mayscript:'true',
            	width:600, height:170} ;
        	var parameters = {jnlp_href: 'jnlpCliente' } ;
        	deployJava.runApplet(attributes, parameters, '1.6');
    	</script>
    </body>
</html>