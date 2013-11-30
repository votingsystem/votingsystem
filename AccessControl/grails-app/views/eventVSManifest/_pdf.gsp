<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
 "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
 <%@page pageEncoding="ISO-8859-1"%>
<html>
	<head>
			<link rel="stylesheet" href="${grailsApplication.config.grails.serverURL}/css/pdf.css" type="text/css"/>
	</head>
	<body>
		<div class="pdf">
			<div class="titulo">${eventVS.subject}</div>
			<div class="cuerpo">${eventVS.content}</div>
		</div> 			
	</body>
</html>