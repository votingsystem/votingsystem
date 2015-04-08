<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>
    <title>WebView Test</title>

    <link href="${resourceURL}/polymer-localstorage/polymer-localstorage.html" rel="import"/>
    <link rel="import" href="${resourceURL}/polymer-ui-toggle-button/polymer-ui-toggle-button.html">
    <link href="${resourceURL}/paper-radio-button/paper-radio-button.html" rel="import"/>

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



            <paper-radio-button toggles></paper-radio-button>

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
<script>

</script>
