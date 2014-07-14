<!DOCTYPE html>
<html>
<head>
    <title>PolymerTest - forms</title>
    <meta name="viewport" content="width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes">

    <asset:stylesheet src="polymer.css"/>

    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">

    <link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog-transition.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-fab', file: 'paper-fab.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-icons/iconsets', file: 'icons.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">


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

        paper-fab {
            transition: transform 500ms cubic-bezier(0.4, 0, 0.2, 1) 500ms, background-color 500ms cubic-bezier(0.4, 0, 0.2, 1);
            -webkit-transition: -webkit-transform 500ms cubic-bezier(0.4, 0, 0.2, 1) 500ms, background-color 500ms cubic-bezier(0.4, 0, 0.2, 1);
            -webkit-transform: scale(0);
            transform: scale(0);
            -webkit-transform-origin: 50% 50%;
            transform-origin: 50% 50%;
        }

        paper-fab[showing] {
            -webkit-transform: none;
            transform: none;
        }

        /* TODO(kschaaf): fix more generally */
        paper-fab[disabled] {
            pointer-events: none;
        }

    </style>
</head>

<body style="height: 800px; width: 1000px; margin 0ps">
<div>
    <paper-button label="Test button" onclick="showDialog()"></paper-button>
</div>


<polymer-element name="advanced-search-form">
    <template>

        <paper-input id="numberId" value={{dateFromValue}} on-input="{{validateForm}}" floatinglabel label="Sólo números"
                     validate="^[0-9]*$" error="Input is not a number!">
        </paper-input>

        <paper-input id="dateFrom" value={{dateFromValue}} on-input="{{validateForm}}" floatinglabel label="Sólo fechas (paper-input)"
                     validate="^(?:(?:31(\/|-|\.)(?:0?[13578]|1[02]))\1|(?:(?:29|30)(\/|-|\.)(?:0?[1,3-9]|1[0-2])\2))(?:(?:1[6-9]|[2-9]\d)?\d{2})$|^(?:29(\/|-|\.)0?2\3(?:(?:(?:1[6-9]|[2-9]\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\d|2[0-8])(\/|-|\.)(?:(?:0?[1-9])|(?:1[0-2]))\4(?:(?:1[6-9]|[2-9]\d)?\d{2})$"
                     error="Input is not a date!">
        </paper-input>

        <paper-fab id="check" icon="check" hidden?="{{!isValidForm}}" on-tap="{{submitForm}}" showing></paper-fab>
        <paper-fab id="bug" icon="bug-report" showing?="{{bugShowing}}" on-tap="{{toggleBugButton}}"></paper-fab>


        <div>
            <paper-button on-click="{{toggleBugButton}}" label="Toggle bug button"></paper-button>
        </div>

        <p>Form date: <input type='date' value="{{yourName}}" placeholder="Enter name..." on-change="{{validateForm}}"></p>

    </template>

    <script>
        Polymer('advanced-search-form', {
            ready: function() { },
            dateFromValue:'',
            bugShowing:true,
            isValidForm:false,

            toggleBugButton: function() {
                console.log('toggleBugButton')
                this.bugShowing = !this.bugShowing;
                this.$.confirmation.toggle()
            },
            validateForm: function() {
                if(this.$ == null) return
                if(this.$.dateFrom.inputValue.length > 0 && !this.$.dateFrom.invalid) {
                   this.isValidForm = true;
                } else this.isValidForm = false;
                console.log('validateForm - dateFrom invalid: ' + this.$.dateFrom.invalid + " - isValidForm: " +
                        this.isValidForm + " - inputValue.length: " + this.$.dateFrom.inputValue.length +
                        " - value.length: " + this.$.dateFrom.value.length)
            },
            submitForm: function() {
                console.log('submitForm')
            }

        });

    </script>
</polymer-element>

<advanced-search-form></advanced-search-form>


<!-- say-hello element publishes the 'name' property -->
<polymer-element name="say-hello" attributes="name">
    <template>
        Hello, <b>{{name}}</b>!
    </template>
    <script>
        Polymer('say-hello', {
            ready: function() {
                this.name = 'Voter'
            }
        });
    </script>
</polymer-element>
<polymer-element name="intro-tag" noscript>
    <template>
        <!-- bind yourName to the published property, name -->
        <p><say-hello name="{{yourName}}"></say-hello></p>
        <!-- bind yourName to the value attribute -->
        <p>What's your name? <input type='date' value="{{yourName}}" placeholder="Enter name..."></p>
    </template>
</polymer-element>

<intro-tag></intro-tag>


<polymer-element name="form-test">
    <template>
        <form id="myForm" on-submit="{{ submitForm }}">
            <input class="text" value="{{ someValue}}" name="text">
            <button type="submit">Submit</button>
        </form>
    </template>
    <script>
        Polymer('form-test', {
            submitForm: function(e) {
                e.preventDefault();
                this.$.myForm.submit();
            }
        });
    </script>
</polymer-element>

<form-test></form-test>

</body>
</html>
<asset:script>
    document.addEventListener('polymer-ready', function() {

    });

    function showDialog() {

    }

    function acceptButton() {
        console.log("acceptButton")
    }

</asset:script>
<asset:deferredScripts/>