<!DOCTYPE html>
<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
        <link rel="stylesheet" href="${resource(dir:'css',file:'bootstrap.css')}" />
        <link rel="stylesheet" href="${resource(dir:'css',file:'bootstrap-responsive.css')}" />
        <link rel="stylesheet" href="${resource(dir:'css',file:'appData.css')}" />
        <g:layoutHead />
    </head>
    <body>
         <div class="container">
	        <div role="banner">
	        	<a class="headerTitle" href="${grailsApplication.config.grails.serverURL}">${message(code: 'nombreServidorLabel', null)}</a>
	        </div>
			<g:layoutBody/>

				<div class="footer" role="contentinfo">
					<hr/>
		        	<a href="mailto:${grailsApplication.config.SistemaVotacion.emailAdmin}">${message(code: 'emailLabel', null)}</a>
		        </div>

			<div id="spinner" class="spinner" style="display:none;"><g:message code="spinner.alt" default="Loading&hellip;"/></div>
		</div>
    </body>
</html>