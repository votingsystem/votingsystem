<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-ripple', file: 'paper-ripple.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/add-control-center-dialog']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/eventvs-addoption-dialog']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/eventvs-admin-dialog']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/votevs-result-dialog']"/>">

</head>
<body>
<div layout vertical style="width:1200px;height: 1200px;margin:0px auto; ">
    <div layout horizontal>
        <votingsystem-button onclick="showMessageVS('msg msg msg ', 'caption', null, false)" style="margin:10px;">
            Message dialog        <i class="fa fa-pencil-square-o"></i></votingsystem-button>
        <votingsystem-button onclick="document.querySelector('#addControlCenterDialog').show()" style="margin:10px; ">addControlCenterDialog <i class="fa fa-check"></i></votingsystem-button>
        <votingsystem-button onclick="document.querySelector('#addVotingOptionDialog').toggle()" style="margin:10px; ">addVotingOptionDialog <i class="fa fa-check"></i></votingsystem-button>
        <votingsystem-button onclick="document.querySelector('#eventVSAdminDialog').opened = true" style="margin:10px; ">eventVSAdminDialog</votingsystem-button>
        <votingsystem-button onclick="showVotevsResultDialog()" style="margin:10px; ">votevsResultDialog</votingsystem-button>

        <g:datePicker name="dateBegin" value="${new Date()}" precision="minute" relativeYears="[0..1]"/>
    </div>
    <div layout vertical>
        <paper-input id="depositSubject" floatinglabel label="<g:message code="subjectLbl"/>" required></paper-input>
    </div>
    <add-control-center-dialog id="addControlCenterDialog"></add-control-center-dialog>
    <add-voting-option-dialog id="addVotingOptionDialog"></add-voting-option-dialog>
    <eventvs-admin-dialog id="eventVSAdminDialog"></eventvs-admin-dialog>
    <votevs-result-dialog id="votevsResultDialog"></votevs-result-dialog>
</div>
</body>
</html>
<asset:script>

    console.log("dateBegin.value: " + getDatePickerValue('dateBegin'))

    function showVotevsResultDialog() {
        document.querySelector('#votevsResultDialog').show({statusCode:200, optionSelected:"Pruebas"})
    }

    document.addEventListener('polymer-ready', function() {
        document.querySelector("#addVotingOptionDialog").addEventListener('on-submit', function (e) {
            console.log("========== e.detail: " + e.detail)
        })
    });

</asset:script>
<asset:deferredScripts/>