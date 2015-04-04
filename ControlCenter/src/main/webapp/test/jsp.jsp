<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>

<fmt:setBundle basename="org.votingsystem.web.controlcenter.messages" var="bundle"/>
<html   xmlns="http://www.w3.org/1999/xhtml"
        xmlns:rich="http://richfaces.org/rich"
        xmlns:a4j="http://richfaces.org/a4j">
<head>
    <title>Test JSP</title>
    <link href="${config.resourceURL}/polymer/polymer.html" rel="import"/>
    <link href="${config.resourceURL}/font-roboto/roboto.html" rel="import"/>
    <link href="${config.resourceURL}/core-icon/core-icon.html" rel="import"/>
    <link href="${config.resourceURL}/core-icons/core-icons.html" rel="import"/>
    <link href="${config.resourceURL}/paper-button/paper-button.html" rel="import"/>

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

    <p:calendar id="popup" widgetVar="calendarWV"/>

    <f:view>
        <h1>
            <h:outputText value="Example of using JSF tags with other custom tags"/>
        </h1>
        <h:inputText value="#{config.resourceURL}">
        </h:inputText>.
    </f:view>


    <h2>- restURL: - ${config.resourceURL}</h2>
    <h2>- Property: - ${config.getProperty('vs.systemNIF')}</h2>
    <h2>- i18N: ${msg.publicDelegationLbl}</h2>
    <h2>- months1: ${msg.months1}</h2>
    <h2>- test1: ${msg.test1}</h2>
    <h2>- groupVS: ${groupvs.email}</h2>

    <input type="text" id="datepicker">

    InvalidErrorMsg: <fmt:message key="certByHEXNotFound" bundle="${bundle}">
        <fmt:param value="11111111111"/>
    </fmt:message><br/>


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