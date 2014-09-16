<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/core-overlay', file: 'core-overlay.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/balance-details']"/>">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/tagvs-select-dialog']"/>">
</head>
<body>

    <button onclick="document.querySelector('#balanceDetails').tapHandler()" style="margin:10px;">Show balance</button>
    <button onclick="document.querySelector('#tagDialog').show()" style="margin:10px;">Tag dialog</button>



<tagvs-select-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                                serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></tagvs-select-dialog>



</body>
</html>
<asset:script>


</asset:script>
<asset:deferredScripts/>