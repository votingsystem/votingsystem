<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/core-overlay', file: 'core-overlay.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/balance-details.gsp']"/>">
</head>
<body>

    <button onclick="document.querySelector('#balanceDetails').tapHandler()"style="margin:10px;">Show balance</button>

    <balance-details id="balanceDetails"></balance-details>

    <core-ajax auto url="http://vickets:8086/Vickets/reports/forWeek?date=20140721" handleAs="json" contentType="json"></core-ajax>
    <!--<template id="test" repeat="{{userBalances.userBalanceList}}">
        <div>{{IBAN}}</div>
        <template repeat="{{transactionFromList}}">
            <div>{{amount}}</div>
        </template>
    </template>-->

</body>
</html>
<asset:script>

    document.addEventListener('polymer-ready', function() {
            var ajax = document.querySelector("core-ajax");
            ajax.addEventListener("core-response", function(e) {
                //document.querySelector('#test').model = {userBalances: e.detail.response.userBalances};
                //document.querySelector('#balanceDetails').opened = true
                document.querySelector('#balanceDetails').balance = e.detail.response.userBalances.userBalanceList[0]
            });
        });

</asset:script>
<asset:deferredScripts/>