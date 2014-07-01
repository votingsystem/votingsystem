<!DOCTYPE html>
<html>
<head>
    <title></title>
    <meta name="viewport" content="width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes">
    <asset:stylesheet src="polymer.css"/>
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
    <link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-toolbar', file: 'core-toolbar.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-menu', file: 'core-submenu.html')}">


    <style>
    :host {
        position: absolute;
        width: 100%;
        height: 100%;
        box-sizing: border-box;
        top: 0px;
        left: 0px;
    }
    #core_scaffold {
        position: absolute;
        top: 0px;
        right: 0px;
        bottom: 0px;
        left: 0px;
        width: 100%;
        height: 100%;
    }
    #core_header_panel {
        background-color: rgb(255, 255, 255);
    }
    #core_toolbar {
        background-color: rgb(79, 125, 201);
        color: rgb(255, 255, 255);
    }
    #core_menu {
        font-size: 16px;
    }
    </style>

</head>

<body style="margin:0px auto 0px auto;">
<core-scaffold id="core_scaffold" style="">
    <core-header-panel mode="seamed" id="core_header_panel" navigation flex>
        <core-toolbar id="core_toolbar"></core-toolbar>
        <core-menu valueattr="label" id="core_menu" theme="core-light-theme">
            <core-item label="Item1" icon="settings" id="core_item" horizontal center layout></core-item>
            <core-item label="Item2" icon="settings" id="core_item1" horizontal center layout></core-item>
        </core-menu>
    </core-header-panel>
    <div id="div" tool>Title</div>
</core-scaffold>
</body>
</html>