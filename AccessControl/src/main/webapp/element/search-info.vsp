<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="search-info">
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
        <div hidden="{{!isVisible}}" class="eventSearchInfo">
            <div class="eventSearchText">{{searchResultMsg}}</div>
            <div hidden="{{!beginSearchMsg}}" class="eventSearchDateBegin">{{beginSearchMsg}}</div>
            <div hidden="{{!endSearchMsg}}" class="eventSearchDateFinish">{{endSearchMsg}}</div>
        </div>
    </template>
    <script>
        Polymer({
            is:'search-info',
            ready: function() {
                this.isVisible = false,
                this.searchResultMsgTemplate = "${msg.searchResultMsg}"
                this.beginSearchMsgTemplate = "${msg.beginSearchInfoLbl}"
                this.endSearchMsgTemplate = "${msg.endSearchInfoLbl}"
            },
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
</dom-module>