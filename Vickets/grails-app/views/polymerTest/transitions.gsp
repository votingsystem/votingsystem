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

    <link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">

    <style>
    #hero1 {
        position: absolute;
        top: 0;
        left: 0;
        width: 300px;
        height: 300px;
        background-color: orange;
    }
    #hero2 {
        position: absolute;
        top: 200px;
        left: 300px;
        width: 300px;
        height: 300px;
        background-color: orange;
    }
    #bottom1, #bottom2 {
        position: absolute;
        bottom: 0;
        top: 0;
        left: 0;
        height: 50px;
        width: 300px;
    }
    #bottom1 {
        background-color: blue;
    }
    #bottom2 {
        background-color: green;
    }

    </style>
</head>

<body style="margin:20px auto 0px auto;">

    <template id="template1" is="auto-binding">

        <core-icon-button icon="{{$.pages.selected != 0 ? 'arrow-back' : 'menu'}}" on-tap="{{selectView}}" style=""></core-icon-button>


        <core-animated-pages id="pages" transitions="hero-transition cross-fade" selected="{{page}}">
            <section id="page1">
                <div id="hero1" hero-id="hero" hero>
                    <div id="bottom1" cross-fade></div>
                </div>

            </section>
            <section id="page2">
                <div id="hero2" hero-id="hero" hero>
                    <div id="bottom2" cross-fade></div>
                </div>

            </section>
        </core-animated-pages>
    </template>


</body>
<script>
    var template1 = document.querySelector('#template1')

    template1.addEventListener('template-bound', function(e) {
        var scope = e.target;
        scope.selectView = function(e) {
            console.log(" ==== selectView: " + e)
            this.$.pages.selected = this.$.pages.selected == 0 ? 1 :0;
        }

    })

</script>
</html>