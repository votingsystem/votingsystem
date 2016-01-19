<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-expandable-div/vs-expandable-div.html" rel="import"/>
<link href="../resources/bower_components/vs-datepicker/vs-datepicker.html" rel="import"/>

<link href="../resources/d3.html" rel="import"/>
<link href="transactions-treemap.vsp" rel="import"/>
<link href="transactions-scatter.vsp" rel="import"/>

<dom-module name="transactions-dashboard">
    <style>
        :host {
            display: block;
            width:100%;
            height: 100%;
            position: relative;
        }
    </style>
    <template>
        <div class="horizontal layout">
            <div id="selectorDiv" style="border: 1px solid #ccc; margin: 5px;border-radius: 3px;padding: 5px;">
                <div class="horizontal layout" style="font-size: 0.9em;margin: 5px 0 5px 0;">
                    <vs-datepicker id="dateFromDatepicker" years-back="5" month-labels='[${msg.monthsShort}]' day-labels='[${msg.weekdaysShort}]' time-enabled
                        caption="${msg.fromLbl}" style="margin:0 5px 0 0;"> </vs-datepicker>
                    <vs-datepicker id="dateToDatepicker" years-back="5" month-labels='[${msg.monthsShort}]' day-labels='[${msg.weekdaysShort}]' time-enabled
                                   caption="${msg.toLbl}"> </vs-datepicker>
                </div>
                <div class="horizontal layout"  style="margin: 5px 10px 5px 10px;">
                    <input class="form-control" type="text" id="searchInput" value='' style="width: 140px;margin:0 10px 0 0;" placeholder="${msg.searchLbl}"/>
                    <button on-click="getHTTP"><i id="searchIcon" class="fa fa-search" style="margin:0px 3px 0px 0px;" ></i> ${msg.searchLbl}</button>
                </div>
                <div class="horizontal layout center center-justified" style="width: 260px; margin: 0 5px 0 5px; font-size: 0.75em; padding: 0 6px 0 6px;">
                    <input id="timeLimitedCheckBox" type="checkbox" value="timeLimited" on-click="timeCheckboxSelected" checked="true" style="">
                    <span style="color:#ba0011;"><i class="fa fa-clock-o"></i> ${msg.timeLimitedLbl}</span>
                    <input id="timeFreeCheckBox" type="checkbox" value="timeFree" on-click="timeCheckboxSelected" style="margin:0 0 0 10px;" checked="true">${msg.timeFreeLbl}
                </div>
                <div style="padding:5px 0 0 0; width: 100%;">
                    <vs-expandable-div id="currencyExpDiv" caption="${msg.currencyLbl}"></vs-expandable-div>
                </div>
                <div style="margin:5px 0 0 0;width: 100%;">
                    <vs-expandable-div id="transactionTypeExpDiv" caption="${msg.movementTypeLbl}"></vs-expandable-div>
                </div>
                <div style="margin:5px 0 0 0;width: 100%;">
                    <vs-expandable-div id="tagExpDiv" caption="${msg.tagLbl}"></vs-expandable-div>
                </div>
            </div>
            <div id="treemapZoomableDiv">
                <transactions-treemap id="transactionTreemap" on-filter-request="filterChart"></transactions-treemap>
            </div>
        </div>
        <div style="display: none;">
            <div id="transactionTypeExpDivContent">
                <template is="dom-repeat" items="[[transactionTypes]]" as="item">
                    <div style="display: block;">
                        <label for="[[item]]" style$="[[_getItemStyle(item)]]">
                            <input id="[[item]]" type="checkbox" value="[[item]]" on-click="typeCheckboxSelected"
                                   checked="true">[[_getTransactionTypesDescription(item)]]</label>
                    </div>
                </template>
            </div>
            <div id="currencyExpDivContent">
                <template is="dom-repeat" items="[[currencyCodes]]" as="curencyItem">
                    <div style="display: block;">
                        <label for="[[curencyItem]]" style$="[[_getItemStyle(curencyItem)]]">
                            <input id="[[curencyItem]]" type="checkbox" value="[[curencyItem]]" on-click="currencyCheckboxSelected"
                                   checked="true">[[curencyItem]]</label>
                    </div>
                </template>
            </div>
            <div id="tagExpDivContent">
                <template is="dom-repeat" items="[[tags]]" as="tagItem">
                    <div style="display: block;">
                        <label for="[[tagItem]]" style$="[[_getItemStyle(tagItem)]]">
                            <input id="[[tagItem]]" type="checkbox" value="[[tagItem]]" on-click="tagCheckboxSelected"
                                   checked="true">[[tagItem]]</label>
                    </div>
                </template>
            </div>
        </div>

        <div>
            <transactions-scatter id="transactionsScatter" style="height: 300px; width: 100%;"></transactions-scatter>
        </div>
    </template>
    <script>
    (function() {
        Polymer({
            is:'transactions-dashboard',
            properties: {
                orderBy: {type: String, value:"orderByType"}
            },
            ready: function() {
                //this.$.dateFromDatepicker.setDate(new Date().getMonday())
                this.$.dateFromDatepicker.setDate(DateUtils.parseInputType("2015-11-01"))
                this.$.dateToDatepicker.setDate(new Date())

                this.formatDate = d3.time.format("%Y%m%d_%M%S");

                this.getHTTP()
            },
            timeCheckboxSelected:function (e) {
                console.log("timeCheckboxSelected: " + e.target.value + " - checked: " + e.target.checked)
                if(e.target.checked) {
                    var index = this.transStats.transactionTimeFilter.indexOf(e.target.value)
                    this.transStats.transactionTimeFilter.splice(index, 1)
                } else {
                    this.transStats.transactionTimeFilter.push(e.target.value)
                }
                this.filterChart()
            },
            currencyCheckboxSelected: function(e) {
                if(e.target.checked) {
                    var index = this.transStats.transactionCurrencyFilter.indexOf(e.target.value)
                    this.transStats.transactionCurrencyFilter.splice(index, 1)
                } else {
                    this.transStats.transactionCurrencyFilter.push(e.target.value)
                }
                this.filterChart()
            },
            typeCheckboxSelected: function(e) {
                if(e.target.checked) {
                    var index = this.transStats.transactionTypeFilter.indexOf(e.target.value)
                    this.transStats.transactionTypeFilter.splice(index, 1)
                } else {
                    this.transStats.transactionTypeFilter.push(e.target.value)
                }
                this.filterChart()
            },
            tagCheckboxSelected: function(e) {
                if(e.target.checked) {
                    var index = this.transStats.transactionTagFilter.indexOf(e.target.value)
                    this.transStats.transactionTagFilter.splice(index, 1)
                } else {
                    this.transStats.transactionTagFilter.push(e.target.value)
                }
                this.filterChart()
            },
            filterChart:function (e) {
                this.$.transactionsScatter.filterChart(this.transStats)
                var filteredTransStats = new TransactionsStats()
                if(e && e.detail) this.orderBy = e.detail
                this.chartData.forEach(function(transactionvs) {
                    if(!this.transStats.checkFilters(transactionvs).filtered) filteredTransStats.pushTransaction(transactionvs, this.orderBy)
                }.bind(this))
                if(this.orderBy === "orderByType") treemapData = filteredTransStats.transactionsTreeByType
                if(this.orderBy === "orderByTag") treemapData = filteredTransStats.transactionsTreeByTag
                TransactionsStats.setCurrencyPercentages(treemapData)
                this.$.transactionTreemap.chart(treemapData)
            },
            _getItemStyle:function(item) {
                return 'font-weight: bold; color: ' + TransactionsStats.getColorScale(item) + ';'
            },
            _getTransactionTypesDescription:function(transactionType) {
                return transactionsMap[transactionType].lbl
            },
            getHTTP:function() {
                var targetURL = "/CurrencyServer/rest/transactionVS/from/" + this.formatDate(this.$.dateFromDatepicker.getDate()) +
                                "/to/" + this.formatDate(this.$.dateToDatepicker.getDate())
                if(this.$.searchInput.value != null && this.$.searchInput.value.trim() !== "") {
                    targetURL = targetURL + "?searchText=" + this.$.searchInput.value.trim()
                }
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.json(targetURL, function (json) {
                    this.chartData = json.resultList
                    this.transStats = new TransactionsStats()
                    this.chartData.forEach(function(transactionvs) {
                        this.transStats.pushTransaction(transactionvs, "orderByType")
                    }.bind(this))
                    this.transactionTypes = this.transStats.transactionTypes
                    this.tags = this.transStats.tags
                    this.currencyCodes = this.transStats.currencyCodes
                    this.$.transactionTypeExpDiv.appendContent(this.$.transactionTypeExpDivContent)
                    this.$.currencyExpDiv.appendContent(this.$.currencyExpDivContent)
                    this.$.tagExpDiv.appendContent(this.$.tagExpDivContent)
                    TransactionsStats.setCurrencyPercentages(this.transStats.transactionsTreeByType)
                    this.$.transactionsScatter.chart(this.chartData)
                    this.$.transactionTreemap.chart(this.transStats.transactionsTreeByType)
                }.bind(this))
            }
        });
    })();
    </script>
</dom-module>