<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dropdown-menu', file: 'paper-dropdown-menu.html')}">

<polymer-element name="uservs-home" attributes="dataMap">
    <template>
        <style>
            .transBlock { border: 1px solid #6c0404; margin: 10px;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
            }
            .numTrans { font-size: 2em; color: #6c0404; text-align: center;}
            .transDesc {background: #6c0404; color: #f9f9f9; padding: 5px;}
        </style>
        <core-ajax id="ajax" url="{{url}}" handleAs="json" response="{{dataMap}}" method="get" contentType="json"
                   on-core-response="{{ajaxResponse}}"></core-ajax>
        <div layout horizontal center center-justified id="selectorContainer">
            <paper-dropdown-menu id="dropDownMenu" valueattr="label" halign="right" on-core-select="{{selectAction}}"
                                 selected="<g:message code="lastHourEventsLbl"/>" relatedTarget="{{$.selectorContainer}}">
                <paper-item label="<g:message code="lastHourEventsLbl"/>"></paper-item>
                <paper-item label="<g:message code="last12HourEventsLbl"/>"></paper-item>
                <paper-item label="<g:message code="last24HourEventsLbl"/>"></paper-item>
            </paper-dropdown-menu>
        </div>
        {{timePeriodStr}}
        <div layout flex horizontal wrap around-justified>
            <div class="transBlock">
                <div class="numTrans">{{transactionVSData.numTransFromBankVS}}</div>
                <div class="transDesc">TransFromBankVS</div>
            </div>
            <div class="transBlock">
                <div class="numTrans">{{transactionVSData.numTransFromUserVS}}</div>
                <div class="transDesc">TransFromUserVS</div>
            </div>
            <div class="transBlock">
                <div class="numTrans">{{transactionVSData.numTransFromUserVSToUserVS}}</div>
                <div class="transDesc">TransFromUserVSToUserVS</div>
            </div>
            <div class="transBlock">
                <div class="numTrans">{{transactionVSData.numTransFromGroupVSToMemberGroup}}</div>
                <div class="transDesc">TransFromGroupVSToMemberGroup</div>
            </div>
            <div class="transBlock">
                <div class="numTrans">{{transactionVSData.numTransFromGroupVSToAllMembers}}</div>
                <div class="transDesc">TransFromGroupVSToAllMembers</div>
            </div>
            <div class="transBlock">
                <div class="numTrans">{{transactionVSData.numTransVicketInitPeriod}}</div>
                <div class="transDesc">TransVicketInitPeriod</div>
            </div>
            <div class="transBlock">
                <div class="numTrans">{{transactionVSData.numTransVicketInitPeriodTimeLimited}}</div>
                <div class="transDesc">TransVicketInitPeriodTimeLimited</div>
            </div>
            <div class="transBlock">
                <div class="numTrans">{{transactionVSData.numTransVicketRequest}}</div>
                <div class="transDesc">TransVicketRequest</div>
            </div>
            <div class="transBlock">
                <div class="numTrans">{{transactionVSData.numTransVicketSend}}</div>
                <div class="transDesc">TransVicketSend</div>
            </div>
            <div class="transBlock">
                <div class="numTrans">{{transactionVSData.numTransVicketCancellation}}</div>
                <div class="transDesc">TransVicketCancellation</div>
            </div>
            <div class="transBlock">
                <div class="numTrans">{{transactionVSData.numTransCancellation}}</div>
                <div class="transDesc">TransCancellation</div>
            </div>
        </div>
    </template>
<script>
    Polymer('uservs-home', {
        initialized:false,
        ready: function() { },
        publish: {
            dataMap: {value: {}}
        },
        dataMapChanged: function() {
            console.log(this.tagName + " - dataMapChanged: " + JSON.stringify(this.dataMap))
            if(!this.dataMap) return;
            this.dataMapStr = JSON.stringify(this.dataMap)
            this.timePeriodStr = JSON.stringify(this.dataMap.timePeriod)
            this.transactionVSData = this.dataMap.transactionVSData
        },
        selectAction: function(e, details) {
            if(!this.initialized) {
                this.initialized = true
                return
            }
            if(details.isSelected) {
                var numHours
                if("<g:message code="lastHourEventsLbl"/>" === details.item.label) numHours = 1
                if("<g:message code="last12HourEventsLbl"/>" === details.item.label) numHours = 12
                if("<g:message code="last24HourEventsLbl"/>" === details.item.label) numHours = 24
                var targetURL = "${createLink( controller:'app', action:"userVS", absolute:true)}/last/" + numHours
                console.log(this.tagName + " - targetURL: " + targetURL)
                this.$.ajax.url = targetURL
                this.$.ajax.go()
            }
        }
    });
</script>
</polymer-element>
