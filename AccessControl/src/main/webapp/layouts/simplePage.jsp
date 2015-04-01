<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <script src="${config.resourceURL}/webcomponentsjs/webcomponents.min.js" type="text/javascript"></script>
    <link href="${config.resourceURL}/polymer/polymer.html" rel="import"/>
    <link href="${config.resourceURL}/font-roboto/roboto.html" rel="import"/>
    <link href="${config.resourceURL}/font-awesome/css/font-awesome.min.css" media="all" rel="stylesheet" />
    <link href="${config.webURL}/css/votingSystem.css" media="all" rel="stylesheet" />
    <script src="${config.webURL}/js/utilsVS.js" type="text/javascript"></script>
    <jsp:include page="/include/utils_js.jsp"/>
    <link href="${config.webURL}/element/alert-dialog.vsp" rel="import"/>
    <link href="${config.resourceURL}/vs-innerpage-signal/vs-innerpage-signal.html" rel="import"/>
    <link href="${config.resourceURL}/paper-button/paper-button.html" rel="import"/>
    <!--<script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite-compressed.js'></script>-->

</head>
<body id="voting_system_page" style="margin:0px auto 0px auto; max-width: 1200px;">
    <div id="pageLayoutDiv" style="display:none;">
        <g:layoutBody/>
    </div>
    <div id="loadingDiv" style="width: 30px;margin: 100px auto 0px auto">
        <i class="fa fa-cog fa-spin" style="font-size:3em;color:#ba0011;"></i>
    </div>
    <alert-dialog id="_votingsystemMessageDialog"></alert-dialog>
    <core-signals id="coreSignals"></core-signals>
</body>
</html>
<script>
    document.querySelector('#coreSignals').addEventListener('core-signal-vs-innerpage', function(e) {
        sendSignalVS(e.detail)
    });

    document.addEventListener('polymer-ready', function() {
        document.querySelector('#pageLayoutDiv').style.display = 'block';
        document.querySelector('#loadingDiv').style.display = 'none';
        updateMenuLinks()
    });
</script>
