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
    <link rel="import" href="${resource(dir: '/bower_components/core-animation', file: 'core-animation.html')}">

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
                <div id="hero2" class="card" hero-id="hero" hero>
                    <div id="bottom2" cross-fade>ddd</div>
                </div>

            </section>
        </core-animated-pages>
    </template>


<core-animation id="fadeout" duration="500">
    <core-animation-keyframe>
        <core-animation-prop name="opacity" value="1"></core-animation-prop>
    </core-animation-keyframe>
    <core-animation-keyframe>
        <core-animation-prop name="opacity" value="0"></core-animation-prop>
    </core-animation-keyframe>
</core-animation>

<div id="el">Fade me out</div><button onclick="fadeOut()">trigger fade</button>

</body>
<script>

    //http://stackoverflow.com/questions/12991164/maintaining-final-state-at-end-of-css3-animation
    function fadeOut() {
        var animation = new CoreAnimation();
        animation.duration = 500;
        animation.keyframes = [
            {opacity: 1},
            {opacity: 0},
            {display: 'none'}
        ];
        animation.target = document.getElementById('el');
        animation.play();
    }


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