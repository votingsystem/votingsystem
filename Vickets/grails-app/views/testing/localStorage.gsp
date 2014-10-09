<!DOCTYPE html>
<html>
<head>
    <title>WebView Test</title>
    <meta name="layout" content="simplePage" />
    <link rel="import" href="${resource(dir: '/bower_components/polymer-localstorage', file: 'polymer-localstorage.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/polymer-ui-toggle-button', file: 'polymer-ui-toggle-button.html')}">

</head>
<body id="voting_system_page">
    <polymer-element name="x-test1">
        <template>
            string entered below will be stored in localStorage and automatically retrived from localStorage when the page is reloaded<br>
            <input value="{{value}}">
            <polymer-localstorage name="polymer-localstorage-x-test1" value="{{value}}"></polymer-localstorage>
        </template>
        <script>
            Polymer('x-test1');
        </script>
    </polymer-element>

    <x-test1></x-test1>
    <br><br>

    <polymer-element name="x-test2">
        <template>
            <polymer-ui-toggle-button value="{{mode}}"></polymer-ui-toggle-button>
            <polymer-localstorage name="polymer-localstorage-x-test2" value="{{mode}}"></polymer-localstorage>
        </template>
        <script>
            Polymer('x-test2', {
                mode: false
            });
        </script>
    </polymer-element>

    <x-test2></x-test2>
</body>
</html>
<asset:script>

</asset:script>
<asset:deferredScripts/>