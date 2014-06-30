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



    <style>
    html,body {
        height: 100%;
        margin: 0;

        font-family: 'RobotoDraft', sans-serif;
    }
    core-header-panel {
        height: 100%;
        overflow: auto;
        -webkit-overflow-scrolling: touch;
    }
    core-toolbar {
        background: #03a9f4;
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

<body style="margin:20px auto 0px auto;">
    <div layout horizontal center-justified>
        <paper-input floatinglabel label="<g:message code="onlyNumbersLbl" />" validate="^[0-9]*$" error="<g:message code="onlyNumbersErrorLbl" />"></paper-input>
    </div>


<polymer-element name="intro-tag" noscript>
    <template>
        <!-- bind yourName to the published property, name -->
        <p>{{yourName}}</p>
        <!-- bind yourName to the value attribute -->
        <p>What's your name? <input value="{{yourName}}" placeholder="Enter name..."></p>
    </template>
</polymer-element>

<intro-tag></intro-tag>

<div layout horizontal center center-justified style="height:100px;"><div>OMG, centered!</div> </div>

<div class="button raised">
    <div class="center" fit>SUBMIT</div>
    <paper-ripple fit></paper-ripple>
</div>

<div class="card" style="margin:0px auto 0px auto; width:200px;">
    <img style="height: 100px;" src="${assetPath(src: 'avatar-01.svg')}">
    <paper-ripple class="recenteringTouch" fit></paper-ripple>
</div>


<div layout horizontal center center-justified style="border: 2px solid #cc1606; width:500px;">
    <div flex three>flex three</div><div>div2</div><div flex two>flex two</div>
</div>

<div layout horizontal style="border: 2px solid #cc1606; width:500px; margin:10px auto 10px auto;">
    <div>div1</div><div flex style="margin:0px auto 0px auto;">div2 (flex)</div><div>div3</div>
</div>

<div layout vertical center center-justified style="border: 2px solid #cc1606; width:500px; height: 200px; margin:10px 0px 0px 0px;">
    <div>div1</div><div flex style="margin:0px auto 0px auto;">div2 (flex)</div><div>div3</div>
</div>
<div>div1</div><div>div2</div><div>div3</div>
<script>

</script>
</body>
</html>