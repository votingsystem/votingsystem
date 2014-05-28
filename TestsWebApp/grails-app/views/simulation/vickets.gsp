<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main"/>
		<title><g:message code="simulationWebAppCaption"/></title>
	</head>
	<body>
        <div class="row" style="">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li class="active"><g:message code="vicketsOperationsLbl"/></li>
            </ol>
        </div>
		<div class="pageContent" style="position:relative;">

			<div style="width: 50%;height: 50%;overflow: auto;margin: auto;top: 0; left: 0; bottom: 0; right: 0;">
                <div style="margin: 50px 0 20 0; font-weight: bold; font-size: 2em; color: #6c0404;">
                    <g:message code="vicketsOperationsLbl"/>
                </div>
                <a id="initUserBaseDataButton" href="${createLink(controller: 'vicket', action:'initUserBaseData', absolute:true)}"
                   class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
                    <g:message code="initUserBaseDataButton"/>
                </a>
                <a id="makeDepositButton" href="${createLink(controller: 'vicket', action:'deposit', absolute:true)}"
                   class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
                    <g:message code="makeDepositButton"/>
                </a>
                <a id="simulationRunningButton" href="#" onclick="showSimulationRunningDialog('Mensaje de la página principal');"
                   class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
                    TEST
                </a>
			</div>
		</div>
	<div style="display:none;">
		<g:include view="/include/simulationRunningDialog.gsp" style="display:none;"/>
        <g:include view="/include/dialog/addClaimFieldDialog.gsp"/>
	</div>
	</body>
	<asset:script>

		$(function() { });

        function openWindow(targetURL) {
            var width = 1000
            var height = 800
            var left = (screen.width/2) - (width/2);
            var top = (screen.height/2) - (height/2);
            var title = ''

            var newWindow =  window.open(targetURL, title, 'toolbar=no, scrollbars=yes, resizable=yes, '  +
                    'width='+ width +
                    ', height='+ height  +', top='+ top +', left='+ left + '');
        }

	</asset:script>
</html>