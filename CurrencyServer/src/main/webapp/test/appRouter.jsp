<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>
    <title><app-router></app-router></title>
    <script src="${resourceURL}/webcomponentsjs/webcomponents.min.js" type="text/javascript"></script>
    <link href="${resourceURL}/polymer/polymer.html" rel="import"/>
    <link href="${resourceURL}/core-scaffold/core-scaffold.html" rel="import"/>
    <link href="${resourceURL}/core-toolbar/core-toolbar.html" rel="import"/>
    <link href="${resourceURL}/core-menu/core-menu.html" rel="import"/>
    <link href="${resourceURL}/paper-item/paper-item.html" rel="import"/>
    <link href="${resourceURL}/core-item/core-item.html" rel="import"/>
    <link href="${resourceURL}/core-icon/core-icon.html" rel="import"/>
    <link href="${resourceURL}/core-animated-pages/core-animated-pages.html" rel="import"/>
    <link href="${resourceURL}/core-header-panel/core-header-panel.html" rel="import"/>
    <link href="${resourceURL}/core-animated-pages/transitions/slide-from-right.html" rel="import"/>
    <link href="${resourceURL}/core-ajax/core-ajax.html" rel="import"/>
    <link href="${resourceURL}/app-router/app-router.html" rel="import"/>

    <style>
        /** main content */
        :host /deep/ core-header-panel[main] {
            background-color: #fff;
        }
        .content {
            padding: 20px;
        }

        /** sidebar */
        core-header-panel core-item {
            padding: 2px 12px;
            line-height: 40px;
        }
        core-header-panel  core-item:not(:first-of-type) {
            border-top: 1px solid #f5f5f5;
        }
        core-header-panel core-item:hover,
        core-header-panel core-item:focus {
            background-color: #fcfcfc;
            color: #222;
        }
        core-header-panel core-item[active] {
            border-right: 6px solid #f5f5f5;
        }
    </style>
</head>
<body unresolved fullbleed='true'>

<app-router>
    <app-route path="/" import="/pages/home-page.html"></app-route>
    <app-route path="/demo/:pathArg1" import="/pages/demo-page.html"></app-route>
    <app-route path="/notes" import="/pages/notes-page.html"></app-route>
    <app-route path="*" import="/pages/not-found-page.html"></app-route>
</app-router>




<polymer-element name="sidebar-layout" attributes="selected" noscript>
    <template>
        <core-scaffold>
            <core-header-panel navigation flex mode="seamed">
                <core-toolbar>app-router-examples</core-toolbar>
                <core-item icon="home" label="Home" active?="{{selected == 'home'}}"><a href="/#"></a></core-item>
                <core-item icon="polymer" label="Data Binding" active?="{{selected == 'demo'}}"><a href="/#/demo/1337?queryParam1=Routing%20with%20Web%20Components!"></a></core-item>
                <core-item icon="info-outline" label="Notes" active?="{{selected == 'notes'}}"><a href="/#/notes"></a></core-item>
                <core-item icon="exit-to-app" label="GitHub"><a href="https://github.com/erikringsmuth/app-router"></a></core-item>
            </core-header-panel>
            <div tool>
                <content select=".title"></content>
            </div>
            <div class="content">
                <content></content>
            </div>
        </core-scaffold>
    </template>
</polymer-element>


</body>
</html>
<script>

</script>
