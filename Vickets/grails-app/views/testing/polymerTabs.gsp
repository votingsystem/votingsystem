<!DOCTYPE html>
<html>
<head>
    <title></title>
    <meta name="viewport" content="width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes">
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">



    <link rel="import" href="${resource(dir: '/bower_components/core-header-panel', file: 'core-header-panel.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-toolbar', file: 'core-toolbar.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-tabs', file: 'paper-tabs.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-item', file: 'core-item.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-menu-button', file: 'core-menu-button.html')}">

    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>



    <style>
    html,body {
        height: 100%;
        margin: 0;
        background-color: #E5E5E5;
        font-family: 'RobotoDraft', sans-serif;
    }
    core-header-panel {
        height: 100%;
        overflow: auto;
        -webkit-overflow-scrolling: touch;
    }
    core-toolbar {
        background: #cc1606;
        color: white;
    }
    paper-tabs {
        width: 100%;
        -webkit-user-select: none;
        -moz-user-select: none;
        -ms-user-select: none;
        user-select: none;
    }
    .container {
        width: 80%;
        margin: 50px auto;
    }
    @media (min-width: 481px) {
        paper-tabs {
            width: 200px;
        }
        .container {
            width: 400px;
        }
    }
    </style>
</head>

<body unresolved touch-action="auto">
<core-header-panel>
    <core-toolbar>
        <paper-tabs selected="all" valueattr="name" self-end>
            <paper-tab name="all"><g:message code="searchLbl" /></paper-tab>
            <paper-tab name="favorites"><g:message code="favoritesLbl" /></paper-tab>
        </paper-tabs>
    </core-toolbar>

    <!-- main page content will go here -->
    <div style="margin:50px 0px 0px 0px;">
        <div layout horizontal center center-justified style="height:100px;"><div>OMG, centered!</div> </div>

        <paper-input floatinglabel label="<g:message code="onlyNumbersLbl" />" validate="^[0-9]*$" error="<g:message code="onlyNumbersErrorLbl" />"></paper-input>

    <core-item icon="settings" label="Settings"></core-item>
        <paper-input floatinglabel label="Sólo números (floatinglabel)" validate="^[0-9]*$" error="Is not a number"> </paper-input>
    </div>

    <core-menu-button icon="menu">

        <core-item>
            <i id="expandMenuIcon" class="fa fa-bars navbar-text navBar-vicket-icon navbar-left" style="margin: 5px 10px 0 15px;"></i>hghg</core-item>
        <core-item icon="add" label="Add"></core-item>
        <core-item icon="search" label="Search"></core-item>

    </core-menu-button>


</core-header-panel>


<script>
    var tabs = document.querySelector('paper-tabs');
    tabs.addEventListener('core-select', function() {
        console.log("Selected: " + tabs.selected);
    });

    console.log("======== ")
</script>
</body>
</html>