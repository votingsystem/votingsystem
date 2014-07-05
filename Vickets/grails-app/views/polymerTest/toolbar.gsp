<!DOCTYPE html>
<html>
<head>
    <title></title>
    <meta name="viewport" content="width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes">
    <asset:stylesheet src="polymer.css"/>
    <asset:stylesheet src="vickets.css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">



    <link rel="import" href="${resource(dir: '/bower_components/core-header-panel', file: 'core-header-panel.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-toolbar', file: 'core-toolbar.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-tabs', file: 'paper-tabs.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">




    <link rel="import" href="${resource(dir: '/bower_components/core-scaffold', file: 'core-scaffold.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-header-panel', file: 'core-header-panel.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-menu', file: 'core-menu.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-item', file: 'core-item.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-item', file: 'paper-item.html')}">


    <link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-toolbar', file: 'core-toolbar.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-menu', file: 'core-submenu.html')}">


    <style>
        html,body {
            height: 100%;
            margin: 0;

            font-family: 'RobotoDraft', sans-serif;
        }
        core-toolbar {
            background-color: #ba0011;
            border-bottom: 1px solid #f9f9f9;
            color:#f9f9f9;
        }


    </style>

</head>

<body style="margin:0px auto 0px auto;">
<core-toolbar id="mainToolBar" style="display: none;">
    <core-icon-button layout horizontal center-justified on-tap="{{menuAction}}">
        <div layout horizontal center center-justified>
            <i class="fa fa-plus"></i>
        </div>

        </core-icon-button>
    <div flex><g:message code="appTitle"/></div>
    <core-icon-button icon="more" on-tap="{{moreAction}}"></core-icon-button>
</core-toolbar>
</body>
</html>
<asset:script>
    document.addEventListener('polymer-ready', function() {
        var mainToolBar = document.querySelector('#mainToolBar')
        mainToolBar.style.display = 'block';
    });
</asset:script>
<asset:deferredScripts/>