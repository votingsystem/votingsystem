<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
</head>
<body>
<div class="pageContenDiv" style="max-width: 1300px; margin: 0px auto 0px auto;">
    <div style="margin:0px 30px 0px 30px;">
        <div class="row">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'userVS', action:'search')}"><g:message code="usersvsLbl"/></a></li>
                <li class="active"><g:message code="vicketSourceListPageLbl"/></li>
            </ol>
        </div>

        <div style="display: table;width:90%;vertical-align: middle;margin:0px 0 10px 0px;">
            <div style="display:table-cell;margin: auto; vertical-align: top;">
                <select id="vicketSourceStateSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;"
                        class="form-control" onchange="vicketSourceState(this)">
                    <option value="ACTIVE"  style="color:#388746;"> - <g:message code="selectActiveVicketSourceLbl"/> - </option>
                </select>
            </div>
        </div>

        <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
            background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

        <polymer-element name="vicket-source-list" attributes="url">
            <template>
                <style></style>
                <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get" contentType="json"></core-ajax>
                <div flex horizontal wrap around-justified layout>
                    <template repeat="{{vicketSource in responseData.vicketSourceList}}">
                        <div class='card vicketSourceDiv {{vicketSource | stateClass}}' on-click="{{showVicketSourceDetails}}">
                            <div class='vicketSourceSubjectDiv'><h2>{{vicketSource.name}}</h2></div>
                            <div class='vicketSourceDescriptionDiv'><p>{{vicketSource.description | getHtml}}</p></div>
                            <div class='vicketSourceReasonDiv'>
                                <p>{{vicketSource.reason}}</p>
                            </div>
                        </div>
                    </template>
                </div>
            </template>
            <script>
                Polymer('vicket-source-list', {
                    showVicketSourceDetails:function(e) {
                        var targetURL = "${createLink( controller:'userVS')}/" +
                                e.target.templateInstance.model.vicketSource.id + "?menu=" + menuType
                        window.location.href = targetURL
                    },
                    stateClass:function(vicketSource) {
                        if('ACTIVE' == vicketSource.state) return "vicketSourceActive";
                        if('PENDING' == vicketSource.state) return "vicketSourcePending";
                        if('CLOSED' == vicketSource.state) return "vicketSourceFinished";
                    },
                    getHtml:function(htmlEncoded) {
                        var element = document.createElement("div");
                        element.innerHTML = htmlEncoded;
                        //Firefox bug with innerHTML
                        return (typeof element.innerText == 'undefined' ? htmlEncoded:element.innerText);
                    }
                });
            </script>
        </polymer-element>
        <vicket-source-list id="vicketSourceList"></vicket-source-list>

        <div id="vicketSourceList" class="row container"><ul></ul></div>
    </div>
</div>

</body>
</html>
<asset:script>
    document.querySelector("#vicketSourceList").url = "<g:createLink controller="userVS" action="vicketSourceList"/>?menu=" + menuType

    function vicketSourceState(selected) {
        var optionSelected = selected.value
        console.log("vicketSourceStateSelect: " + optionSelected)
        var targetURL = "${createLink(controller: 'userVS', action:'vicketSourceList')}?menu=" + menuType;
        if("" != optionSelected) {
            targetURL = targetURL + "&state=" + vicketSourceState
        }
        history.pushState(null, null, targetURL);
        document.querySelector("#vicketSourceList").url = targetURL
    }
</asset:script>
