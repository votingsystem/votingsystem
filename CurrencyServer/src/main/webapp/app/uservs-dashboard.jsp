<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="uservs-dashboard">
    <template>
        <style>
            .transBlock { border: 1px solid #6c0404; margin: 10px;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
                cursor: pointer;
            }
            .numTrans { font-size: 2em; color: #6c0404; text-align: center;}
            .transDesc {background: #6c0404; color: #f9f9f9; padding: 5px;}
        </style>
        <div class="layout horizontal center center-justified">
            <select id="transactionLapsedSelect" style="margin: 10px 0 10px 40px; width: 400px; font-size: 1.1em; height: 30px;"
                    on-change="selectAction" class="form-control">
                <option value="1">${msg.lastHourMovementsLbl}</option>
                <option value="12">${msg.last12HourMovementsLbl}</option>
                <option value="24">${msg.last24HourMovementsLbl}</option>
            </select>
        </div>
        <div class="layout flex horizontal wrap around-justified">
            <div id="FROM_BANKVS" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransFromBankVS}}</div>
                <div class="transDesc">${msg.bankVSInputLbl}</div>
            </div>
            <div id="FROM_USERVS" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransFromUserVS}}</div>
                <div class="transDesc">${msg.transactionVSFromUserVS}</div>
            </div>
            <div id="FROM_GROUP_TO_MEMBER_GROUP" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{fromGroupToMemberGroupInfo}}</div>
                <div class="transDesc">${msg.transactionVSFromGroupToMemberGroup}</div>
            </div>
            <div id="FROM_GROUP_TO_ALL_MEMBERS" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{fromGroupToAllMembersInfo}}</div>
                <div class="transDesc">${msg.transactionVSFromGroupToAllMembers}</div>
            </div>
            <div id="CURRENCY_PERIOD_INIT" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransCurrencyInitPeriod}}</div>
                <div class="transDesc">${msg.currencyPeriodInitLbl}</div>
            </div>
            <div id="CURRENCY_PERIOD_INIT_TIME_LIMITED" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransCurrencyInitPeriodTimeLimited}}</div>
                <div class="transDesc">${msg.currencyPeriodInitTimeLimitedLbl}</div>
            </div>
            <div id="CURRENCY_REQUEST" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransCurrencyRequest}}</div>
                <div class="transDesc">${msg.selectCurrencyRequestLbl}</div>
            </div>
            <div id="CURRENCY_SEND" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransCurrencySend}}</div>
                <div class="transDesc">${msg.selectCurrencySendLbl}</div>
            </div>
            <div id="CURRENCY_CHANGE" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransCurrencyChange}}</div>
                <div class="transDesc">${msg.selectCurrencyChangeLbl}</div>
            </div>
            <div id="CANCELATION" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransCancellation}}</div>
                <div class="transDesc">${msg.cancelationsLbl}</div>
            </div>
        </div>
    </template>
<script>
    Polymer({
        is:'uservs-dashboard',
        properties: {
            dashBoardDto:{type:Object, value:null, observer:'dashBoardDtoChanged'},
            url:{type:String, value:contextURL + "/rest/app/userVSDashboard", observer:'getHTTP'}
        },
        ready: function() {
            this.lapse = window.location.href.substr(window.location.href.lastIndexOf('/') + 1);
            if(isNumber(this.lapse)) {
                this.$.transactionLapsedSelect.value = this.lapse
                this.url = contextURL + "/rest/app/userVSDashboard/hoursAgo/" + this.$.transactionLapsedSelect.value
            } else this.lapse = null
            console.log(this.tagName + " - ready - lapse: " + this.lapse)
        },
        dashBoardDtoChanged: function() {
            if(!this.dashBoardDto) return;
            console.log(this.tagName + " - dashBoardDtoChanged:" + JSON.stringify(this.dashBoardDto))
            console.log(this.tagName + " - timePeriod:" + JSON.stringify(this.dashBoardDto.timePeriod) )
            this.dateFrom = new Date(this.dashBoardDto.timePeriod.dateFrom)
            this.dateTo = new Date(this.dashBoardDto.timePeriod.dateTo)
            if(this.dashBoardDto.transFromGroupVSToMemberGroup.numTrans > 0) {
                this.fromGroupToMemberGroupInfo = this.dashBoardDto.transFromGroupVSToMemberGroup.numTrans + " trans - " +
                        dashBoardDto.transFromGroupVSToMemberGroup.numUsers + " users"
            } else this.fromGroupToMemberGroupInfo = 0
            if(this.dashBoardDto.transFromGroupVSToAllMembers.numTrans > 0) {
                this.fromGroupToAllMembersInfo = this.dashBoardDto.transFromGroupVSToAllMembers.numTrans + " trans - " +
                        this.dashBoardDto.transFromGroupVSToAllMembers.numUsers + " users"
            } else this.fromGroupToAllMembersInfo = 0
        },
        selectAction: function() {
            var servicePath = "/rest/app/userVSDashboard/hoursAgo/" + this.$.transactionLapsedSelect.value
            var targetURL = contextURL + servicePath
            var newURL = contextURL + "/spa.xhtml#!" + servicePath
            history.pushState(null, null, newURL);
            this.url = targetURL
        },
        transBlockSelected: function(e) {
            page.show(contextURL + "/rest/transactionVS/from/" + this.dateFrom.urlFormatWithTime() + "/to/" +
                    this.dateTo.urlFormatWithTime() + "?transactionvsType=" + e.target.parentNode.id)
        },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                this.dashBoardDto = toJSON(rawData.response)
            }.bind(this));
        }
    });
</script>
</dom-module>
