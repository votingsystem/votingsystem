<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-html-echo', file: 'votingsystem-html-echo.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">


<polymer-element name="search-info" attributes="opened">
    <template>
        <style>
        .eventSearchInfo {
            margin: 0px auto 0px auto;
            display:table;
            font-size: 0.8em;
            padding: 0px 30px 7px 30px;
            border-bottom: 1pt solid #cccccc;
        }
        </style>
        <div class="eventSearchInfo" style="display:{{isVisible? 'block':'none'}}">
            <div class="eventSearchText">{{searchResultMsg}}</div>
            <div class="eventSearchDateBegin" style="display:{{beginSearchMsg? 'block':'none'}}">{{beginSearchMsg}}</div>
            <div class="eventSearchDateFinish" style="display:{{endSearchMsg? 'block':'none'}}">{{endSearchMsg}}</div>
        </div>
    </template>
    <script>
        Polymer('search-info', {
            isVisible :false,
            searchResultMsgTemplate:"<g:message code="searchResultMsg"/>",
            beginSearchMsgTemplate:"<g:message code="beginSearchInfoLbl"/>",
            endSearchMsgTemplate:"<g:message code="endSearchInfoLbl"/>",

            ready: function() {},
            show: function(textQuery, dateBeginFrom, dateBeginTo, dateFinishFrom, dateFinishTo) {
                this.searchResultMsg = searchResultMsgTemplate.format(textQuery)
                if(!(FormUtils.checkIfEmpty(dateBeginFrom) && FormUtils.checkIfEmpty(dateBeginTo))) {
                    this.beginSearchMsg = beginSearchMsgTemplate.format(dateBeginFrom, dateBeginTo)
                }
                if(!(FormUtils.checkIfEmpty(dateFinishFrom) && FormUtils.checkIfEmpty(dateFinishTo))) {
                    this.endSearchMsg = endSearchMsgTemplate.format(dateFinishFrom, dateFinishTo)
                }
                this.isVisible = true
            },
            hide: function() {
                this.isVisible = false
                this.searchResultMsg = null
                this.beginSearchMsg = null
                this.endSearchMsg = null
            }
        });
    </script>
</polymer-element>