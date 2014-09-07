<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <title><g:message code="simulationWebAppCaption"/></title>
</head>
<body>
<div>
    <div style="padding: 0px 30px 0px 30px;">
        <div class="text-center" style="margin: 0px auto 0px auto; font-weight: bold; font-size: 2em; color: #6c0404;">
            <g:message code="votingSystemOperationsLbl"/>
        </div>
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

        <div class="text-center" style="margin: 30px 0 10px 0; font-weight: bold; font-size: 2em; color: #6c0404;">
            <g:message code="vicketsOperationsLbl"/>
        </div>
        <a href="${createLink(controller: 'vicket', action:'addUsersToGroup', absolute:true)}"
           class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
            <g:message code="addUsersToGroupButton"/>
        </a>
        <a id="initUserBaseDataButton" href="${createLink(controller: 'vicket', action:'initUserBaseData', absolute:true)}"
           class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
            <g:message code="initUserBaseDataButton"/>
        </a>
        <a id="makeDepositButton" href="${createLink(controller: 'vicket', action:'deposit', absolute:true)}"
           class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:400px;">
            <g:message code="makeDepositButton"/>
        </a>
    </div>
</div>
</body>
<asset:script>

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