<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <title><g:message code="simulationWebAppCaption"/></title>
</head>
<body>
    <div layout vertical class="pageContentDiv" style="margin: 0px auto 0px auto;padding:0px 30px 0px 30px;">
        <div >
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
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="initElectionProtocolSimulationButton"/>
            </a>
            <a id="initManifestProtocolSimulationButton" href="${createLink(controller: 'manifestProtocolSimulation', action:'inputData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="initManifestProtocolSimulationButton"/>
            </a>
            <a id="initClaimProtocolSimulationButton" href="${createLink(controller: 'claimProtocolSimulation', action:'inputData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="initClaimProtocolSimulationButton"/>
            </a>
            <a id="initTimeStampProtocolSimulationButton" href="${createLink(controller: 'timeStampSimulation', action:'inputData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="initTimeStampProtocolSimulationButton"/>
            </a>
            <a id="initMultiSignProtocolSimulationButton" href="${createLink(controller: 'multiSignSimulation', action:'inputData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="initMultiSignProtocolSimulationButton"/>
            </a>
            <a id="initEncryptionProtocolSimulationButton" href="${createLink(controller: 'encryptionSimulation', action:'inputData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="initEncryptionProtocolSimulationButton"/>
            </a>
        </div>
    </div>
</body>
</html>
<asset:script>
</asset:script>