<!DOCTYPE html>
<%@page pageEncoding="ISO-8859-1"%>
<html>
	<head>
        <link rel="stylesheet" href="${grailsApplication.config.grails.serverURL}/css/pdf.css" type="text/css"/>
	</head>
	<body>
		<div class="pdf">
			<div class="title">${eventVS?.subject}</div>
			<div class="content">${raw(eventVS?.content)}</div>
		</div> 			
	</body>
</html>