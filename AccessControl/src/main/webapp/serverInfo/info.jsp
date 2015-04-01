<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:set var="conf" value="${app}" />
<html>
<head>
    <title></title>
    <meta name="viewport" content="width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes">
    <link href="${config.resourceURL}/polymer/polymer.html" rel="import"/>
    <link href="${config.resourceURL}/font-roboto/roboto.html" rel="import"/>
    <link href="${config.resourceURL}/paper-tabs/paper-tabs.html" rel="import"/>
    <style>
    html,body {
        height: 100%;
        margin: 0;
        font-family: 'RobotoDraft', sans-serif;
    }
    .headerTitle {
        font-size: 26px;
        font-weight:bold;
        line-height: 1;
        display:block;
        margin: 10px auto 0px auto;
        color:#ba0011;
        text-decoration: none;
    }
    </style>
</head>
<body >
<h3 style="text-align: center;">
    <a class="headerTitle" href="${config.webURL}">${msg.appTitle}</a>
</h3>
<polymer-element name="info-page-tabs">
    <template>
        <style shim-shadowdom>
        .tabContent {
            padding: 10px 20px 10px 20px;
            margin:0px auto 0px auto;
            width:auto;
        }
        paper-tabs.transparent-teal {
            background-color: transparent;
            color:#ba0011;
            box-shadow: none;
            cursor: pointer;
        }

        paper-tabs.transparent-teal::shadow #selectionBar {
            background-color: #ba0011;
        }

        paper-tabs.transparent-teal paper-tab::shadow #ink {
            color: #ba0011;
        }
        </style>
        <div  style="width: 1000px; margin:0px auto 0px auto;">
            <paper-tabs  style="width: 1000px;margin:0px auto 0px auto;" class="transparent-teal center" valueattr="name"
                         selected="{{selectedTab}}"  on-core-select="{{tabSelected}}" noink>
                <paper-tab name="info" style="width: 400px">${msg.infoLbl}</paper-tab>
                <paper-tab name="serviceList">${msg.serviceListLbl}</paper-tab>
                <paper-tab name="appData">${msg.appDataLabel}</paper-tab>
            </paper-tabs>
            <div id="infoDiv" class="tabContent" style="display:{{selectedTab == 'info'?'block':'none'}}">
                <div class="mainLink"><a href="http://www.sistemavotacion.org">${msg.webSiteLbl}</a></div>
                <div class="mainLink"><a href="https://github.com/votingsystem/votingsystem/tree/master/AccessControl">
                    ${msg.sourceCodeLbl}</a>
                </div>
                <div class="mainLink"><a href="https://github.com/votingsystem/votingsystem/wiki/Control-de-Acceso">${msg.wikiLabel}</a></div>
            </div>
        </div>
    </template>

    <script>
        Polymer('info-page-tabs', {
            selectedTab:'info'
        });
    </script>
</polymer-element>
<info-page-tabs style="width: 1000px;"></info-page-tabs>
</body>
</html>
