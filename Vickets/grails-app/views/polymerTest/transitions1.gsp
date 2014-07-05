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
    <link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-animated-pages/transitions', file: 'hero-transition.html')}">

    <link rel="import" href="${resource(dir: '/bower_components/core-icons', file: 'core-icons.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">



    <style>  </style>
</head>

<body style="margin:20px auto 0px auto;">


    <polymer-element name="x-el" noscript>
        <template>
            <style>
            #hero {
                position: absolute;
                top: 0;
                right: 0;
                width: 50px;
                height: 300px;
                background-color: blue;
            }
            </style>
            <div id="hero" hero-id="bar" hero></div>
        </template>
    </polymer-element>

    <polymer-element name="x-page-1" noscript>
        <template>
            <style>
            #hero1 {
                position: absolute;
                top: 0;
                left: 0;
                width: 300px;
                height: 300px;
                background-color: orange;
            }
            </style>
            <div id="hero1" hero-id="foo" hero></div>
            <div id="hero2" hero-id="bar" hero></div>
        </template>
    </polymer-element>

    <polymer-element name="x-page-2" noscript>
        <template>
            <style>
            #hero1 {
                position: absolute;
                top: 200px;
                left: 300px;
                width: 300px;
                height: 300px;
                background-color: orange;
            }
            #hero2 {
                background-color: blue;
                height: 150px;
                width: 400px;
            }
            </style>
            // The below element is one level of shadow from the core-animated-pages and will
            // be transitioned.
            <div id="hero1" hero-id="foo" hero></div>
            // The below element contains a hero inside its shadowRoot making it two levels away
            // from the core-animated-pages, and will not be transitioned.
            <x-el></x-el>
        </template>
    </polymer-element>

    <template id="template2" is="auto-binding">
        <core-icon-button icon="{{$.pages.selected != 0 ? 'arrow-back' : 'menu'}}" on-tap="{{selectView}}" style="fill: red;"></core-icon-button>
        <core-animated-pages  id="pages" transitions="hero-transition">
            <x-page-1></x-page-1>
            <x-page-2></x-page-2>
        </core-animated-pages>
    </template>

</body>
<script>
    var template2 = document.querySelector('#template2')

    template2.addEventListener('template-bound', function(e) {
        var scope = e.target;
        scope.selectView = function(e) {
            console.log(" ==== selectView: " + e)
            this.$.pages.selected = this.$.pages.selected == 0 ? 1 :0;
        }
    })
</script>
</html>