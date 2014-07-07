<!DOCTYPE html>
<html>
<head>
    <title>PolymerTest - dialog</title>
    <asset:javascript src="utilsVS.js"/>
    <asset:stylesheet src="polymer.css"/>
    <meta name="viewport" content="width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=no">
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">

    <link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog-transition.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-overlay', file: 'core-overlay.html')}">



    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-advanced-search-dialog',
            file: 'votingsystem-advanced-search-dialog.html')}">

    <style>
    </style>
</head>

<body style="height: 800px;">

<paper-button label="Avanced search dialog" onclick="showAvancedSearchDialog()"></paper-button>

<paper-button label="Overlay dialog" onclick="showOverlayDialog()"></paper-button>
<paper-button label="Tag dialog" onclick="showTagDialog()"></paper-button>

<core-overlay id="confirmation" layout vertical center center-justified  class="card"
              transition="paper-dialog-transition-center" style="position: absolute; top:30px;">
    <h3>Dialog</h3>
    <input placeholder="say something..." autofocus>
    <div>I agree with this wholeheartedly.</div>
    <button core-overlay-toggle>OK</button>
</core-overlay>

<votingsystem-advanced-search-dialog id="advancedSearchDialog" transition="paper-dialog-transition-center" opened="true" >
</votingsystem-advanced-search-dialog>


<paper-button label="paperDialog" onclick="openWindow('')"></paper-button>


</body>
</html>
<asset:script>

    document.addEventListener('polymer-ready', function() {

    });

    document.getElementById("advancedSearchDialog").addEventListener('submit-form', function(e) {
        console.log(" ====== submit-form - addEventListener - e: " + e + " - detail: " + JSON.stringify(e.detail))
    });

    function showOverlayDialog() {
        var dialog = document.querySelector('#confirmation');
        dialog.toggle();
    }

    function showDialog() {
        var dialog = document.querySelector('#paperDialog');
        dialog.toggle();
    }

    function showAvancedSearchDialog() {
        var dialog = document.querySelector('#advancedSearchDialog');
        dialog.toggle();
    }

    function showTagDialog() {

    }


    function acceptButton() {
        console.log("acceptButton")
    }

</asset:script>
<asset:deferredScripts/>