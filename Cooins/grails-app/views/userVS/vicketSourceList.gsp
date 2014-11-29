<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
<vs-innerpage-signal caption="<g:message code="bankVSListPageLbl"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <div style="margin:0px 30px 0px 30px;">
        <div layout horizontal center center-justified>
            <select id="bankVSStateSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;"
                     onchange="bankVSState(this)">
                <option value="ACTIVE"  style="color:#388746;"> - <g:message code="selectActiveBankVSLbl"/> - </option>
            </select>
        </div>

        <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
            background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

        <polymer-element name="cooin-source-list" attributes="url">
            <template>
                <style></style>
                <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get" contentType="json"></core-ajax>
                <div flex horizontal wrap around-justified layout>
                    <template repeat="{{bankVS in responseData.bankVSList}}">
                        <div class='card bankVSDiv {{bankVS | stateClass}}' on-click="{{showBankVSDetails}}">
                            <div class='bankVSSubjectDiv'><h2>{{bankVS.name}}</h2></div>
                            <div class='bankVSDescriptionDiv'><p>{{bankVS.description | getHtml}}</p></div>
                            <div class='bankVSReasonDiv'>
                                <p>{{bankVS.reason}}</p>
                            </div>
                        </div>
                    </template>
                </div>
            </template>
            <script>
                Polymer('cooin-source-list', {
                    showBankVSDetails:function(e) {
                        var targetURL = "${createLink( controller:'userVS')}/" +
                                e.target.templateInstance.model.bankVS.id + "?menu=" + menuType
                        window.location.href = targetURL
                    },
                    stateClass:function(bankVS) {
                        if('ACTIVE' == bankVS.state) return "bankVSActive";
                        if('PENDING' == bankVS.state) return "bankVSPending";
                        if('CLOSED' == bankVS.state) return "bankVSFinished";
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
        <cooin-source-list id="bankVSList"></cooin-source-list>

        <div id="bankVSList" class="row container"><ul></ul></div>
    </div>
</div>

</body>
</html>
<asset:script>
    document.querySelector("#bankVSList").url = "<g:createLink controller="userVS" action="bankVSList"/>?menu=" + menuType

    function bankVSState(selected) {
        var optionSelected = selected.value
        console.log("bankVSStateSelect: " + optionSelected)
        var targetURL = "${createLink(controller: 'userVS', action:'bankVSList')}?menu=" + menuType;
        if("" != optionSelected) {
            targetURL = targetURL + "&state=" + bankVSState
        }
        history.pushState(null, null, targetURL);
        document.querySelector("#bankVSList").url = targetURL
    }
</asset:script>
<asset:deferredScripts/>
