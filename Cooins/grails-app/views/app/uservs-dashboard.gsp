<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="core-ajax" file="core-ajax.html"/>
<vs:webresource dir="paper-dropdown-menu" file="paper-dropdown-menu.html"/>
<vs:webresource dir="paper-dropdown" file="paper-dropdown.html"/>
<vs:webresource dir="core-menu" file="core-menu.html"/>

<polymer-element name="uservs-dashboard" attributes="dataMap">
    <template>
        <style>
            .transBlock { border: 1px solid #6c0404; margin: 10px;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
                cursor: pointer;
            }
            .numTrans { font-size: 2em; color: #6c0404; text-align: center;}
            .transDesc {background: #6c0404; color: #f9f9f9; padding: 5px;}
            .colored {
                color: #6c0404;
            }
        </style>
        <core-ajax id="ajax" url="{{url}}" handleAs="json" response="{{dataMap}}" method="get" contentType="json"
                   on-core-response="{{ajaxResponse}}"></core-ajax>
        <div id="selectorContainer" layout horizontal center center-justified relative>
            <paper-dropdown-menu id="dropDownMenu" halign="right" on-core-select="{{selectAction}}">
                <paper-dropdown class="dropdown colored" transition="">
                    <core-menu selected="0">
                        <paper-item><g:message code="lastHourEventsLbl"/></paper-item>
                        <paper-item><g:message code="last12HourEventsLbl"/></paper-item>
                        <paper-item><g:message code="last24HourEventsLbl"/></paper-item>
                    </core-menu>
                </paper-dropdown>
            </paper-dropdown-menu>
        </div>
        <div layout flex horizontal wrap around-justified>
            <div id="FROM_BANKVS" class="transBlock" on-click="{{transBlockSelected}}">
                <div class="numTrans">{{transactionVSData.numTransFromBankVS}}</div>
                <div class="transDesc">TransFromBankVS</div>
            </div>
            <div id="FROM_USERVS" class="transBlock" on-click="{{transBlockSelected}}">
                <div class="numTrans">{{transactionVSData.numTransFromUserVS}}</div>
                <div class="transDesc">TransFromUserVS</div>
            </div>
            <div id="FROM_GROUP_TO_MEMBER" class="transBlock" on-click="{{transBlockSelected}}">
                <div class="numTrans">{{transactionVSData.numTransFromGroupVSToMember}}</div>
                <div class="transDesc">numTransFromGroupVSToMember</div>
            </div>
            <div id="FROM_GROUP_TO_MEMBER_GROUP" class="transBlock" on-click="{{transBlockSelected}}">
                <template if="{{transactionVSData.transFromGroupVSToMemberGroup.numTrans > 0}}">
                    <div class="numTrans">{{transactionVSData.transFromGroupVSToMemberGroup.numTrans}} trans -
                    {{transactionVSData.transFromGroupVSToMemberGroup.numUsers}} users</div>
                </template>
                <template if="{{transactionVSData.transFromGroupVSToMemberGroup.numTrans === 0}}">
                    <div class="numTrans">0</div>
                </template>
                <div class="transDesc">TransFromGroupVSToMemberGroup</div>
            </div>
            <div id="FROM_GROUP_TO_ALL_MEMBERS" class="transBlock" on-click="{{transBlockSelected}}">
                <template if="{{transactionVSData.numTransFromGroupVSToAllMembers.numTrans > 0}}">
                    <div class="numTrans">{{transactionVSData.numTransFromGroupVSToAllMembers.numTrans}} trans -
                    {{transactionVSData.numTransFromGroupVSToAllMembers.numUsers}} users</div>
                </template>
                <template if="{{transactionVSData.numTransFromGroupVSToAllMembers.numTrans === 0}}">
                    <div class="numTrans">0</div>
                </template>
                <div class="transDesc">numTransFromGroupVSToAllMembers</div>
            </div>
            <div id="COOIN_INIT_PERIOD" class="transBlock" on-click="{{transBlockSelected}}">
                <div class="numTrans">{{transactionVSData.numTransCooinInitPeriod}}</div>
                <div class="transDesc">TransCooinInitPeriod</div>
            </div>
            <div id="COOIN_INIT_PERIOD_TIME_LIMITED" class="transBlock" on-click="{{transBlockSelected}}">
                <div class="numTrans">{{transactionVSData.numTransCooinInitPeriodTimeLimited}}</div>
                <div class="transDesc">TransCooinInitPeriodTimeLimited</div>
            </div>
            <div id="COOIN_REQUEST" class="transBlock" on-click="{{transBlockSelected}}">
                <div class="numTrans">{{transactionVSData.numTransCooinRequest}}</div>
                <div class="transDesc">TransCooinRequest</div>
            </div>
            <div id="COOIN_SEND" class="transBlock" on-click="{{transBlockSelected}}">
                <div class="numTrans">{{transactionVSData.numTransCooinSend}}</div>
                <div class="transDesc">TransCooinSend</div>
            </div>
            <div id="CANCELLATION" class="transBlock" on-click="{{transBlockSelected}}">
                <div class="numTrans">{{transactionVSData.numTransCancellation}}</div>
                <div class="transDesc">TransCancellation</div>
            </div>
        </div>
    </template>
<script>
    Polymer('uservs-dashboard', {
        initialized:false,
        ready: function() { },
        dateFrom:null,
        dateTo:null,
        publish: {
            dataMap: {value: {}}
        },
        dataMapChanged: function() {
            console.log(this.tagName + " - dataMapChanged:" + JSON.stringify(this.dataMap))
            if(!this.dataMap) return;
            this.transactionVSData = this.dataMap.transactionVSData
            this.dateFrom = new Date(this.dataMap.transactionVSData.timePeriod.dateFrom)
            this.dateTo = new Date(this.dataMap.transactionVSData.timePeriod.dateTo)
        },
        selectAction: function(e, details) {
            if(!this.initialized) {
                this.initialized = true
                return
            }
            if(details.isSelected) {
                var numHours
                if("<g:message code="lastHourEventsLbl"/>" === details.item.innerHTML) numHours = 1
                if("<g:message code="last12HourEventsLbl"/>" === details.item.innerHTML) numHours = 12
                if("<g:message code="last24HourEventsLbl"/>" === details.item.innerHTML) numHours = 24
                var targetURL = "${createLink( controller:'app', action:"userVS", absolute:true)}/" + numHours
                console.log(this.tagName + " - targetURL: " + targetURL)
                this.$.ajax.url = targetURL
                this.$.ajax.go()
            }
        },
        transBlockSelected: function(e) {
            var targetURL = "${createLink( controller:'transactionVS', action:"from", absolute:true)}/" +
                    this.dateFrom.urlFormatWithTime() + "/to/" + this.dateTo.urlFormatWithTime() +
                    "?transactionvsType=" + e.target.parentNode.id
            console.log(this.tagName + " - showTransBlock - targetURL: " + targetURL)
            loadURL_VS(targetURL)
        }
    });
</script>
</polymer-element>
