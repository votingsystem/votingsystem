<%@ page contentType="text/html" %>
<%@page pageEncoding="UTF-8"%>
<html>
    <head>
        <title>Solicitud de historial de votación de representante</title>
        <style type="text/css" media="screen">
        </style>
    </head>
    <body>
		${solicitante} ha solicitado mediante <a href="${urlSolicitud}">este documento firmado electrónicamente</a> una copia de seguridad del
		historial de votaciones del representante  '<b>${representative}</b>' entre las fechas '<b>${dateFromStr}</b>' y '<b>${dateToStr}</b>'.<br/>
		<br/>
		Pulse <a href="${urlDescarga}">aquí</a> para proceder con la descarga del archivo con la copia de seguridad.
		<br/><br/>
		Saludos.
    </body>
</html>
