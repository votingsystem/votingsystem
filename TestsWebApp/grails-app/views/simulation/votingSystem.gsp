<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="simulationWebAppCaption"/></title>
</head>
<body>
<div class="pageContenDiv">
    <div style="padding: 0px 30px 0px 30px;">
        <div class="row">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li class="active"><g:message code="votingSystemOperationsLbl"/></li>
            </ol>
        </div>
        <div class="text-center" style="margin: 0px auto 0px auto; font-weight: bold; font-size: 2em; color: #6c0404;">
            <g:message code="votingSystemOperationsLbl"/>
        </div>
        <div>
            <a id="initElectionProtocolSimulationButton" href="${createLink(controller: 'electionProtocolSimulation', action:'inputData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
                <g:message code="initElectionProtocolSimulationButton"/>
            </a>
            <a id="initManifestProtocolSimulationButton" href="${createLink(controller: 'manifestProtocolSimulation', action:'inputData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
                <g:message code="initManifestProtocolSimulationButton"/>
            </a>
            <a id="initClaimProtocolSimulationButton" href="${createLink(controller: 'claimProtocolSimulation', action:'inputData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
                <g:message code="initClaimProtocolSimulationButton"/>
            </a>
            <a id="initTimeStampProtocolSimulationButton" href="${createLink(controller: 'timeStampSimulation', action:'inputData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
                <g:message code="initTimeStampProtocolSimulationButton"/>
            </a>
            <a id="initMultiSignProtocolSimulationButton" href="${createLink(controller: 'multiSignSimulation', action:'inputData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
                <g:message code="initMultiSignProtocolSimulationButton"/>
            </a>
            <a id="initEncryptionProtocolSimulationButton" href="${createLink(controller: 'encryptionSimulation', action:'inputData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
                <g:message code="initEncryptionProtocolSimulationButton"/>
            </a>
            <a id="simulationRunningButton" href="#" onclick="showSimulationRunningDialog('Mensaje de la página principal');"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
                TEST
            </a>
        </div>
    </div>
</div>
<div style="display:none;">
    <g:include view="/include/simulationRunningDialog.gsp" style="display:none;"/>
    <g:include view="/include/dialog/addClaimFieldDialog.gsp"/>
</div>
</body>
</html>
<asset:script>

    $(function() { });

</asset:script>