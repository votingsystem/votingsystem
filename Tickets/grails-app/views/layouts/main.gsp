<!DOCTYPE html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title><g:message code="appTitle"/></title>
    <r:external uri="/images/euro_16.png"/>
	<r:require module="masterStyles"/>
	<g:layoutHead/>
	<r:layoutResources />
</head>
	<body>
		<div id="appLogo" role="banner" style="padding:10px 0px 10px 0px;">
			<a href="${grailsApplication.config.grails.serverURL}" style="margin:0px 10px 5px 15px;">
				<g:message code="appTitle"/>
			</a>
		</div>
		<g:layoutBody/>
		<div class="footer" role="contentinfo"></div>
		<r:layoutResources/>
	</body>
</html>