<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="org.votingsystem.web.accesscontrol.messages" var="bundle"/>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:ui="http://java.sun.com/jsf/facelets"
      xmlns:h="http://java.sun.com/jsf/html"
      xmlns:f="http://java.sun.com/jsf/core">
<head>
    <title>Test JSP</title>
    <link href="${config.resourceURL}/polymer/polymer.html" rel="import"/>
    <link href="${config.resourceURL}/font-roboto/roboto.html" rel="import"/>
    <link href="${config.resourceURL}/core-icon/core-icon.html" rel="import"/>
    <link href="${config.resourceURL}/core-icons/core-icons.html" rel="import"/>
    <link href="${config.resourceURL}/paper-button/paper-button.html" rel="import"/>
    <script src="${config.resourceURL}/pikaday/pikaday.js" type="text/javascript"></script>
    <link href="${config.resourceURL}/pikaday/css/pikaday.css" media="all" rel="stylesheet" />

    <style>
        body {
            font-family: RobotoDraft, 'Helvetica Neue', Helvetica, Arial;
            font-size: 14px;
            margin: 0;
            padding: 24px;
            -webkit-tap-highlight-color: rgba(0,0,0,0);
            -webkit-touch-callout: none;
        }
        section { padding: 20px 0; }
        section > div {
            padding: 14px;
            font-size: 16px;
        }
        paper-button.colored { color: #4285f4; }
        paper-button[raised].colored {
            background: #4285f4;
            color: #fff;
        }
        paper-button.custom > core-icon { margin-right: 4px; }
        paper-button.hover:hover { background: #eee; }
        paper-button.blue-ripple::shadow #ripple { color: #4285f4; }
    </style>
</head>
<body bgcolor="white">

    <h2>- contextURL: - ${config.webURL}</h2>
    <h2>- resURL: - ${config.resourceURL}</h2>
    <h2>- Property: - ${config.getProperty('vs.systemNIF')}</h2>
    <h2>- i18N: ${msg.publicDelegationLbl}</h2>
    <h2>- votingsystemPageLbl: ${msg.votingsystemPageLbl}</h2>
    <h2>- test1: ${msg.test1}</h2>
    <h2>- groupVS: ${groupvs.email}</h2>

    <input type="text" id="datepicker">

    InvalidErrorMsg: <fmt:message key="hashCertVSCurrencyInvalidErrorMsg" bundle="${bundle}">
        <fmt:param value="11111111111"/>
    </fmt:message><br/>

Yeps
    <h:outputFormat value="Hello, {0}! You are visitor number {1} to the page.">
        <f:param value="Voter" />
        <f:param value="100"/>
    </h:outputFormat>


    <section>
        <div>Raised buttons</div>

        <paper-button raised>button</paper-button>
        <paper-button raised class="colored">colored</paper-button>
        <paper-button raised disabled>disabled</paper-button>
        <paper-button raised noink>noink</paper-button>
    </section>
    <script>
        var picker = new Pikaday( {
                    i18n: {
                        previousMonth : "${msg.previousMonth}",
                        nextMonth     : "${msg.nextMonth}",
                        months        : ${msg.months_pk},
                        weekdays      : ${msg.weekdays_pk},
                        weekdaysShort : ${msg.weekdaysShort}
                    },
                    showWeekNumber:true,
                    format: 'YYYY',
                    field: document.getElementById('datepicker'),
                    yearRange: [2010,2020]
                });
    </script>
</body>
</html>