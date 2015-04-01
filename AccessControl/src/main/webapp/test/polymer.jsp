<!DOCTYPE html>
<html>
<head>
    <link href="${config.resourceURL}/paper-input/paper-input.html" rel="import"/>
    <link href="${config.resourceURL}/paper-button/paper-button.html" rel="import"/>
    <link href="${config.resourceURL}/paper-ripple/paper-ripple.html" rel="import"/>
    <link href="${config.resourceURL}/paper-button/paper-button.html" rel="import"/>
    <link href="${config.webURL}/element/eventvs-addoption-dialog.vsp" rel="import"/>
    <link href="${config.webURL}/element/eventvs-admin-dialog.vsp" rel="import"/>
    <link href="${config.webURL}/element/votevs-result-dialog.vsp" rel="import"/>
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
    </div>
    <div layout vertical>
        <paper-input id="transactionvsSubject" floatinglabel label="${msg.subjectLbl}" required></paper-input>
    </div>
    <eventvs-option-dialog id="addVotingOptionDialog"></eventvs-option-dialog>
    <eventvs-admin-dialog id="eventVSAdminDialog"></eventvs-admin-dialog>
    <votevs-result-dialog id="votevsResultDialog"></votevs-result-dialog>
</div>
</body>
</html>
<script>

    function showVotevsResultDialog() {
        document.querySelector('#votevsResultDialog').show({statusCode:200, optionSelected:"Pruebas"})
    }

    document.addEventListener('polymer-ready', function() {
        document.querySelector("#addVotingOptionDialog").addEventListener('on-submit', function (e) {
            console.log("polymer.gsp - e.detail: " + e.detail)
        })
    });

</script>
