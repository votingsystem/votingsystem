<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/core-overlay', file: 'core-overlay.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/balance-details.gsp']"/>">
</head>
<body>
<!--
<div layout vertical style="width:1200px;height: 1200px;margin:0px auto; ">
    <balance-details id="balanceDetails"></balance-details>
</div>-->

<button onclick="document.querySelector('#balanceDetails').tapHandler()"style="margin:10px;">Show balance</button>
<section>
    <balance-details id="balanceDetails"></balance-details>
</section>

<!-- a simple dialog element made with core-overlay -->

</body>
</html>
<asset:script>

    document.addEventListener('polymer-ready', function() { });


</asset:script>
<asset:deferredScripts/>