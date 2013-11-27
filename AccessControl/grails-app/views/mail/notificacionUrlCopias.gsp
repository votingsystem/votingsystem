<%@ page contentType="text/html" %>
<%@page pageEncoding="UTF-8"%>
<html>
    <head>
        <title>Solicitud de copia de seguridad</title>
        <style type="text/css" media="screen">
        </style>
    </head>
    <body>
		${solicitante} ha solicitado mediante <a href="${urlSolicitud}">este PDFDocumentVS firmado electrónicamente</a> una copia de seguridad de los archivos
		relacionados con el subject '<b>${eventVSManifestSubject}</b>'.<br/>
		<br/>
		Pulse <a href="${urlDescarga}">aquí</a> para proceder con la descarga del archivo con la copia de seguridad.
		<br/><br/>
		Saludos.
    </body>
</html>
