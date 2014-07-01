<!DOCTYPE html>
<html>
<head>
    <title>PolymerTest - dialog</title>
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog-transition.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog.html')}">



    <style>
        body {
            padding: 0;
            margin: 0;
            -webkit-transform: translateZ(0);
            transform: translateZ(0);
            font-family: RobotoDraft, 'Helvetica Neue', Helvetica, Arial;
            font-size: 16px;
            background: #eee;
            color: rgba(0, 0, 0, 0.87);
        }

        paper-dialog {
            width: 50%;
            min-width: 430px;
        }

        p {
            margin-bottom: 0;
        }

        paper-dialog paper-button {
            font-weight: bold;
        }

        paper-button[default] {
            color: #4285f4;
        }
    </style>
</head>

<body>
<paper-button label="Transition A" onclick="showDialog()"></paper-button>

<paper-dialog id="paperDialog" heading="Dialog" transition="paper-dialog-transition-center" style="display:none;">
    <p>Lorem ipsum dolor sit amet, doming noster at quo, nostrud lucilius rationibus ea duo. Vim no mucius dolores. No bonorum voluptatum vis, has iudicabit consectetuer ne. Nullam sensibus vim id, et quo graeci perpetua.</p>

    <p>Id qui scripta laboramus dissentiet, verterem partiendo vim at. Stet dissentiet ut mei. Iriure facilis eloquentiam pro eu, nec an esse inciderint. In meliore abhorreant sea. Eros nostro ocurreret at nec. Cu per regione persecuti.</p>

    <p>Lorem ipsum dolor sit amet, doming noster at quo, nostrud lucilius rationibus ea duo. Vim no mucius dolores. No bonorum voluptatum vis, has iudicabit consectetuer ne. Nullam sensibus vim id, et quo graeci perpetua.</p>

    <paper-button label="More Info..." dismissive></paper-button>
    <paper-button label="Decline" affirmative></paper-button>
    <paper-button label="Accept" affirmative default onClick="acceptButton()"></paper-button>

</paper-dialog>
</body>
</html>
<asset:script>
    document.addEventListener('polymer-ready', function() {

    });

    function showDialog() {
        var dialog = document.querySelector('#paperDialog');
        dialog.toggle();
    }

    function acceptButton() {
        console.log("acceptButton")
    }

</asset:script>
<asset:deferredScripts/>