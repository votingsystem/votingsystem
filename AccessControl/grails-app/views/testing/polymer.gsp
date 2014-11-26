<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-ripple', file: 'paper-ripple.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/add-control-center-dialog']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/eventvs-addoption-dialog']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/eventvs-admin-dialog']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/votevs-result-dialog']"/>">

</head>
<body>
<div layout vertical style="width:1200px;height: 1200px;margin:0px auto; ">
    <div layout horizontal>
        <paper-button raised onclick="showMessageVS('msg msg msg ', 'caption', null, false)" style="margin:10px;">
            Message dialog        <i class="fa fa-pencil-square-o"></i></paper-button>
        <paper-button raised onclick="document.querySelector('#addControlCenterDialog').show()" style="margin:10px; ">addControlCenterDialog <i class="fa fa-check"></i></paper-button>
        <paper-button raised onclick="document.querySelector('#addVotingOptionDialog').toggle()" style="margin:10px; ">addVotingOptionDialog <i class="fa fa-check"></i></paper-button>
        <paper-button raised onclick="document.querySelector('#eventVSAdminDialog').opened = true" style="margin:10px; ">eventVSAdminDialog</paper-button>
        <paper-button raised onclick="showVotevsResultDialog()" style="margin:10px; ">votevsResultDialog</paper-button>

        <g:datePicker name="dateBegin" value="${new Date()}" precision="minute" relativeYears="[0..1]"/>
    </div>
    <div layout vertical>
        <paper-input id="transactionvsSubject" floatinglabel label="<g:message code="subjectLbl"/>" required></paper-input>
    </div>
    <add-control-center-dialog id="addControlCenterDialog"></add-control-center-dialog>
    <eventvs-option-dialog id="addVotingOptionDialog"></eventvs-option-dialog>
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
            console.log("polymer.gsp - e.detail: " + e.detail)
        })
    });

</asset:script>
<asset:deferredScripts/>