<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" href="${resource(dir:'css',file:'main.css')}" />
        <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
        <g:javascript library="application" />
    </head>
    <body>
        <h3>${flash.tipoCliente}</h3>
        <script src="http://java.com/js/deployJava.js"></script>
        <script>
            var attributes = {
                code:       "org.sistemavotacion.Applet",
                archive:    "SistemaVotacionClienteVoto.jar",
                width:      300,
                height:     300
            };
            <!-- Applet Parameters -->
            var parameters = {urlPDF:"${flash.urlPDF}",
            		urlRecepcionPDFIRMADO:"${flash.urlRecepcionPDFIRMADO}"}; 
            var version = "1.6"; <!-- Required Java Version -->
            deployJava.runApplet(attributes, parameters, version);            
        </script>
    </body>
</html>