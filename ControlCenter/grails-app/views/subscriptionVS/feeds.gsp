<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
<vs-innerpage-signal caption="<g:message code="subscriptionLbl"/>"></vs-innerpage-signal>
<div class="pageContentDiv" style="font-size: 1.2em; margin: 30px auto;">
    <a href="${createLink(controller:'subscriptionVS', action:'elections')}">
        <i class="fa fa-rss-square" style="margin:3px 0 0 10px; color: #f0ad4e;"></i> <g:message code="subscribeToVotingFeedsLbl"/>
    </a>
</div>
</body>
</html>