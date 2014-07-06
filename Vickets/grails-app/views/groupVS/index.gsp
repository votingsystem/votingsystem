<!DOCTYPE html>
<html>
<head>
    <g:if test="${!params.iframe}">
        <meta name="layout" content="main" />
    </g:if>
    <asset:stylesheet src="vicket_groupvs.css"/>
    <link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
</head>
<body>
<div class="pageContenDiv" style="margin: 0px auto 0px auto;padding:0px 30px 0px 30px;">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="groupvsLbl"/></li>
        </ol>
    </div>
    <div style="display: table;width:90%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="groupvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;" class="form-control">
                <option value="ACTIVE"  style="color:#59b;"> - <g:message code="selectActiveGroupvsLbl"/> - </option>
                <option value="PENDING" style="color:#fba131;"> - <g:message code="selectPendingGroupvsLbl"/> - </option>
                <option value="CANCELLED" style="color:#cc1606;"> - <g:message code="selectClosedGroupvsLbl"/> - </option>
            </select>
        </div>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <polymer-element name="groupvs-details" attributes="groupvs index isHero">
        <template>
            <style>
            .view {
                background-color: tomato;
            }
            </style>
            <div class="view" flex vertical center center-justified layout hero-id="groupvs-{{index}}" hero?="{{isHero}}">
                <span cross-fade>{{groupvs.name}}</span>
            </div>
            </template>
        <script>
            Polymer('groupvs-details', {
                isSelected: false
            })
        </script>
    </polymer-element>



    <polymer-element name="groupvs-list" attributes="url">
        <template>
            <style>

            .card {
                position: relative;
                display: inline-block;
                width: 300px;
                height: 200px;
                vertical-align: top;
                background-color: #fff;
                box-shadow: 0 12px 15px 0 rgba(0, 0, 0, 0.24);
                margin: 10px;
            }
            </style>
            <asset:stylesheet src="vicket_groupvs.css"/>
            <core-ajax id="ajax" auto url="{{url}}" response="{{groupvsData}}" handleAs="json" method="get"
                       contentType="json" on-core-complete="{{ajaxComplete}}"></core-ajax>
            <core-icon-button icon="{{$.pages.selected != 0 ? 'arrow-back' : 'menu'}}" on-tap="{{back}}"></core-icon-button>

            <core-animated-pages id="pages" flex selected="0" on-core-animated-pages-transition-end="{{transitionend}}" transitions="cross-fade-all hero-transition">

                <div id="_groupvsList" class="" style="" flex horizontal wrap around-justified layout hero-p>

                    <template repeat="{{groupvs, i in groupvsData.groupvsList}}">
                        <div id="{{groupvs.id}}" groupvs="{{groupvs}}" on-tap="{{selectView}}"
                             class='card groupvsDiv item {{ groupvs.state | groupvsClass }}' hero-p isHero="{{$.pages.selected === i + 1 || $.pages.selected === 0}}" cross-fade>
                            <div class='groupvsSubjectDiv'>{{groupvs.name}}</div>
                            <div class='numTotalUsersDiv text-right'>{{groupvs.numActiveUsers}} <g:message code="usersLbl"/></div>
                            <div class='groupvsInfoDiv'>{{groupvs.description | getHtml}}</div>
                            <div style="position: relative;display: {{(groupvs.state == 'CANCELLED')?'block':'none'}};">
                                <div class='groupvsMessageCancelled' style=""><g:message code="groupvsCancelledLbl"/></div>
                            </div>
                            <div class='groupvsRepresentativeDiv text-right'>{{groupvs | getRepresentativeName}}</div>
                        </div>
                    </template>
                </div>

                <template repeat="{{groupvs, i in groupvsData.groupvsList}}">
                    <groupvs-details vertical layout groupvs="{{groupvs}}" index="{{i}}" hero-p isHero="{{$.pages.selected === item + 1 || $.pages.selected === 0}}"></groupvs-details>
                </template>

            </core-animated-pages>

        </template>
        <script>
            Polymer('groupvs-list', {
                ready: function() {
                    this.$.ajax.addEventListener('core-complete', function() {
                        $('p').each(function(index, item) {
                            if($.trim($(item).text()) === "") {
                                $(item).slideUp(); // $(item).remove();
                            }
                        });
                    })
                },
                selectView :  function(e) {
                    var i = e.target.templateInstance.model.i;
                    this.$.pages.selected = i+1;
                },
                back:function() {
                    this.lastSelected = this.$.pages.selected;
                    console.log("this.lastSelected: " + this.lastSelected);
                    this.$.pages.selected = 0;
                },
                getRepresentativeName:function(groupvs) {
                    return groupvs.representative.firstName + " " + groupvs.representative.lastName
                },
                getHtml:function(htmlEncoded) {
                    var element = document.createElement("div");
                    element.innerHTML = htmlEncoded;
                    //Firefox bug with innerHTML
                    return (typeof element.innerText == 'undefined' ? htmlEncoded:element.innerText);
                },
                groupvsClicked:function(groupvsURL) {
                    window.location.href = groupvsURL
                },
                groupvsClass:function(state) {
                    switch (state) {
                        case 'ACTIVE': return "groupvsActive"
                        case 'PENDING': return "groupvsPending"
                        case 'CANCELLED': return "groupvsFinished"
                    }
                }
            });
        </script>
    </polymer-element>
    <groupvs-list id="groupvsList" url="${createLink(controller: 'groupVS')}?menu=${params.menu}"></groupvs-list>

</div>
</body>
</html>
<asset:script>
    $(function() {
        $("#navBarSearchInput").css( "visibility", "visible" );
        $('#groupvsTypeSelect').on('change', function (e) {
            var groupvsType = $(this).val()
            var optionSelected = $("option:selected", this);
            console.log("groupvsTypeSelect - selected: " + groupvsType)
            var targetURL = "${createLink(controller: 'groupVS')}?menu=" + menuType;
            if("" != groupvsType) {
                history.pushState(null, null, targetURL);
                targetURL = targetURL + "&state=" + groupvsType
            }
            document.querySelector("#groupvsList").url = targetURL
        });
    })

    function processUserSearch(textToSearch) {
        $("#pageInfoPanel").text("<g:message code="searchResultLbl"/> '" + textToSearch + "'")
        $('#pageInfoPanel').css("display", "block")
        document.querySelector("#groupvsList").url = "${createLink(controller: 'search', action: 'groupVS')}?searchText=" + textToSearch
    }
</asset:script>