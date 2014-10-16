<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <title><g:message code="simulationWebAppCaption"/></title>
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="vicketsOperationsLbl"/>"></votingsystem-innerpage-signal>
    <div layout vertical class="pageContentDiv">
        <div class="text-center" style="margin: 50px 0 20 0; font-weight: bold; font-size: 2em; color: #6c0404;">
            <g:message code="vicketsOperationsLbl"/>
        </div>
        <div>
            <a id="initUserBaseDataButton" href="${createLink(controller: 'vicket', action:'initUserBaseData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="initUserBaseDataButton"/>
            </a>
            <a id="makeTransactionVSButton" href="${createLink(controller: 'vicket', action:'transactionvs', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="makeTransactionVSButton"/>
            </a>
            <a href="${createLink(controller: 'vicket', action:'addUsersToGroup', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="addUsersToGroupButton"/>
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