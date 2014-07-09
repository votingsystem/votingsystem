<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-money.png')}" type="image/x-icon">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title><g:message code="appTitle"/></title>
        <g:javascript library="jquery" plugin="jquery"/>
        <asset:stylesheet src="jquery-ui-1.10.4.custom.min.css"/>
        <script type="text/javascript" src="${resource(dir: 'bower_components/jquery-ui', file: 'jquery-ui.min.js')}"></script>
        <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>

        <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>
        <script type="text/javascript" src="${resource(dir: 'bower_components/bootstrap/dist/js', file: 'bootstrap.min.js')}"></script>

        <asset:stylesheet src="vickets.css"/>
        <asset:javascript src="utilsVS.js"/>
        <g:include view="/include/utils_js.gsp"/>
        <style type="text/css" media="screen">
        	#content a{margin: 0px 100px 0px 0px;}
        </style>
    </head>
    <body>
    <div class="container">
        <h2><a class="headerTitle" href="${grailsApplication.config.grails.serverURL}">${message(code: 'appTitle', null)}</a></h2>
        <ul class="nav nav-tabs">
            <li class="active" style="">
                <a href="#mainTab" data-toggle="tab" style="padding: 5px 30px 5px 30px;"><g:message code="infoLbl"/></a>
            </li>
            <li style="">
                <a href="#serviceList" data-toggle="tab" style="padding: 5px 30px 5px 30px;"><g:message code="serviceListLbl"/></a>
            </li>
            <li style="">
                <a href="#appData" data-toggle="tab" style="padding: 5px 30px 5px 30px;"><g:message code="appDataLabel"/></a>
            </li>
        </ul>
        <div class="tab-content" style="min-height: 600px;">
            <div class="tab-pane fade in active" id="mainTab">
                <div class="container"  style="height:100%;">
                    <div id="content" class="content">
                        <div class="mainLinkContainer">
                            <div class="mainLink"><a href="http://www.sistemavotacion.org"><g:message code="webSiteLbl"/></a></div>
                            <div class="mainLink"><a href="https://github.com/votingsystem/votingsystem/tree/master/Vickets">
                                <g:message code="sourceCodeLbl"/></a>
                            </div>
                        </div>
                        <p id="contentText" style="margin: 40px 0px 0px 0px;">${message(code: 'urlMatch', null)}: <b>${grailsApplication.config.grails.serverURL}</b></p>
                    </div>
                </div>
            </div>
            <div class="tab-pane fade" id="serviceList" style="top:0px;">
                <g:include controller="serverInfo" action="serviceList" />
            </div>
            <div class="tab-pane fade" id="appData" style="top:0px;">
                <g:include controller="serverInfo" action="appData" />
            </div>
        </div>


        <div class="infoFooter" style="margin: 0px auto 20px 0;width:100%;font-size: 0.7em; position:relative;">
            <p style="text-align: center;">
                <a  class="appLink" href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}">
                    ${message(code: 'emailLabel', null)}</a>
            </p>
        </div>

        </div>
	</body>
</html>
<asset:script>
      $(function() {
          $( "#tabs" ).tabs()
      });

</asset:script>
<asset:deferredScripts/>