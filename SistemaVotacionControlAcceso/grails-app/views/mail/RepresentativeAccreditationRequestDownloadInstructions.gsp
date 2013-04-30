<%@ page contentType="text/html" %>
<%@page pageEncoding="UTF-8"%>
<html>
    <head>
        <title>Solicitud de acreditaciones de representante</title>
        <style type="text/css" media="screen">
        </style>
    </head>
    <body>
		${solicitante} ha solicitado mediante <a href="${urlSolicitud}">este documento firmado electrónicamente</a> una copia de seguridad de las 
		acreditaciones recibidas por el representante  '<b>${representative}</b>' en la fecha '<b>${dateStr}</b>'.<br/>
		<br/>
		Pulse <a href="${urlDescarga}">aquí</a> para proceder con la descarga del archivo con la copia de seguridad.
		<br/><br/>
		Saludos.
		<br/><b><g:message code="urlSufixMsg"/>:</b><br/>
		<br/><b><g:message code="urlSufixMsg"/>:</b><br/>
		<br/><b><g:message code="urlSufixMsg"/>:</b><br/>
    </body>
</html>
