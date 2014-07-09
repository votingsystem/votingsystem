<html>
    <head>
        <g:javascript library="jquery" plugin="jquery"/>
        <asset:stylesheet src="jquery-ui-1.10.4.custom.min.css"/>
        <script type="text/javascript" src="${resource(dir: 'bower_components/jquery-ui', file: 'jquery-ui.min.js')}"></script>
        <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>

        <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>
        <script type="text/javascript" src="${resource(dir: 'bower_components/bootstrap/dist/js', file: 'bootstrap.min.js')}"></script>

        <asset:stylesheet src="votingSystem.css"/>
        <asset:javascript src="utilsVS.js"/>
        <g:include view="/include/utils_js.gsp"/>
        <style type="text/css" media="screen">
        	#content a{margin: 0px 100px 0px 0px;}
        </style>
    </head>
    <body>
		<div class="container">
        	<a class="headerTitle" href="${grailsApplication.config.grails.serverURL}">${message(code: 'serverNameLbl', null)}</a>
			<div id="tabs" style="min-height: 700px;">
			  <ul>
			    <li><a href="#tabs-1" style="font-size: 0.8em;"><span><g:message code="infoLabel"/></span></a></li>
			    <li><a href="serviceList" style="font-size: 0.8em;"><span><g:message code="serviceURLSMsg"/></span></a></li>
			    <li><a href="appData" style="font-size: 0.8em;"><span><g:message code="appDataLabel"/></span></a></li>
			  </ul>
			  <div id="tabs-1">
			  	<div class="container"  style="height:100%;">
			  		<div id="content" class="content">
			           <div class="mainLinkContainer">
				           <div class="mainLink"><a href="${grailsApplication.config.grails.serverURL}/">${message(code: 'mainPageLabel', null)}</a></div>
				           <div class="mainLink"><a href="https://github.com/votingsystem/votingsystem/tree/master/AccessControl">${message(code: 'sourceCodeLabel', null)}</a></div>
				           <div class="mainLink"><a href="https://github.com/votingsystem/votingsystem/wiki/Control-de-Acceso">${message(code: 'wikiLabel', null)}</a></div>
			           </div>
			           <p id="contentText" style="margin: 40px 0px 0px 0px;">${message(code: 'urlMatch', null)}: <b>${grailsApplication.config.grails.serverURL}</b></p>
   			           <p>
                           <i class="fa fa-gears" style="color:#388746;"></i>
                           <a id="clientToolLink" class="appLink" style="color: #09287e; font-size: 0.9em;"
                              href="${grailsApplication.config.grails.serverURL}/app/ClientTool.zip">
                               <g:message code="clientToolLinkText"/>
                           </a>
			           </p>
					</div>
			  	</div>
			  </div>
			</div>
			<div class="infoFooter" style="margin: 0px auto 20px 0;width:100%;font-size: 0.7em; position:relative;">
				<p style="text-align: center;">
					<a  class="appLink" href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}">
						${message(code: 'emailLabel', null)}</a>
				</p>	
		   	</div>	
		<div>
	</body>
</html>
<asset:script>
    $(function() {
        $( "#tabs" ).tabs()
    });
</asset:script>