<html>
<head>
    <meta name="layout" content="main" />
</head>
<body>
    <div class="text-left" style="max-width: 1000px; margin:auto;">
        <div style="margin: 30px auto;"><votingSystem:feed href="${createLink(controller:'subscriptionVS', action:'elections')}">
            <g:message code="subscribeToVotingFeedsLbl"/></votingSystem:feed>
        </div>
        <!--<div style="margin: 30px auto;"><votingSystem:feed href="${createLink(controller:'subscriptionVS', action:'manifests')}">
            <g:message code="subscribeToManifestFeedsLbl"/></votingSystem:feed>
        </div>
        <div style="margin: 30px auto;"><votingSystem:feed href="${createLink(controller:'subscriptionVS', action:'claims')}">
            <g:message code="subscribeToClaimFeedsLbl"/></votingSystem:feed>
        </div>-->
    </div>
</body>
</html>