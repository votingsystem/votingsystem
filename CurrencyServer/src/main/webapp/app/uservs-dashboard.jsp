<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="user-dashboard">
    <template>
        <style>
            .transBlock { border: 1px solid #6c0404; margin: 10px;
                box-shadow: 0 5px 5px 0 #ccc;
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
            <div id="FROM_BANK" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransFromBank}}</div>
                <div class="transDesc">${msg.bankInputLbl}</div>
            </div>
            <div id="FROM_USER" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransFromUser}}</div>
                <div class="transDesc">${msg.transactionFromUser}</div>
            </div>
            <div id="CURRENCY_PERIOD_INIT" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransCurrencyInitPeriod}}</div>
                <div class="transDesc">${msg.currencyPeriodInitLbl}</div>
            </div>
            <div id="CURRENCY_PERIOD_INIT_TIME_LIMITED" class="transBlock" on-click="transBlockSelected">
                <div class="numTrans">{{dashBoardDto.numTransCurrencyInitPeriodTimeLimited}}</div>
                <div class="transDesc">${msg.lapsedLbl}</div>
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
        is:'user-dashboard',
        properties: {
            dashBoardDto:{type:Object, value:null, observer:'dashBoardDtoChanged'},
            url:{type:String, value:vs.contextURL + "/rest/app/userDashboard", observer:'getHTTP'}
        },
        ready: function() {
            this.lapse = window.location.href.substr(window.location.href.lastIndexOf('/') + 1);
            if(isNumber(this.lapse)) {
                this.$.transactionLapsedSelect.value = this.lapse
                this.url = vs.contextURL + "/rest/app/userDashboard/hoursAgo/" + this.$.transactionLapsedSelect.value
            } else this.lapse = null
            console.log(this.tagName + " - ready - lapse: " + this.lapse)
        },
        dashBoardDtoChanged: function() {
            if(!this.dashBoardDto) return;
            console.log(this.tagName + " - dashBoardDtoChanged:" + JSON.stringify(this.dashBoardDto))
            console.log(this.tagName + " - timePeriod:" + JSON.stringify(this.dashBoardDto.timePeriod) )
            this.dateFrom = new Date(this.dashBoardDto.timePeriod.dateFrom)
            this.dateTo = new Date(this.dashBoardDto.timePeriod.dateTo)
        },
        selectAction: function() {
            var servicePath = "/rest/app/userDashboard/hoursAgo/" + this.$.transactionLapsedSelect.value
            var targetURL = vs.contextURL + servicePath
            var newURL = vs.contextURL + "/spa.xhtml#!" + servicePath
            history.pushState(null, null, newURL);
            this.url = targetURL
        },
        transBlockSelected: function(e) {
            page.show(vs.contextURL + "/rest/transaction/from/" + this.dateFrom.urlFormatWithTime() + "/to/" +
                    this.dateTo.urlFormatWithTime() + "?transactionType=" + e.target.parentNode.id)
        },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            vs.getHTTPJSON(targetURL, function(responseText){
                this.dashBoardDto = toJSON(responseText)
            }.bind(this))
        }
    });
</script>
</dom-module>
