<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-table/vs-table.html" rel="import"/>
<link href="../transaction/transaction-data.vsp" rel="import"/>
<link href="../resources/bower_components/vs-datepicker/vs-datepicker.html" rel="import"/>


<dom-module name="balance-user">
    <template>
        <style>
            #vsTable::shadow table{
                color: #333;
                border-spacing: 0;
                border-collapse: collapse;
                font-size: 14px;
                border: 1px solid #bfbfbf;
                box-shadow: 0 0 4px rgba(0,0,0,0.10);
            }
            #vsTable::shadow th.sortable,
            #vsTable::shadow .rows td {
                white-space: nowrap;
                padding: 8px 5px;
                border: 1px solid #bfbfbf;
                text-align: center;
            }
            #vsTable::shadow tbody tr.rows:hover,
            #vsTable::shadow .rows.selected {
                background-color: #f3f3f3;
                color: #1B8CCD;
            }
            #vsTable::shadow th {
                border: 1px solid #bfbfbf;
                background: #efefef;
                color: #434343;
            }
            #vsTable::shadow .search-head input {
                width: 40px;
                min-width: 90%;
            }
            #vsTable::shadow .paging {
                display: block;
                float: left;
            }
            #vsTable::shadow .pager input {
                border: none;
                outline: none;
                background: #fafafa;
            }
            #vsTable::shadow .pagesize {
                display: none;
                float: left;
            }
            #vsTable::shadow .summary {
                display: block;
                float: right;
            }
            #vsTable::shadow tfoot td {
                border: 1px solid #bfbfbf   ;
                background-color: #fafafa;
            }
            #vsTable::shadow .vs-${msg.subjectLbl}-th{ width:300px; }
            #vsTable::shadow .vs-${msg.dateLbl}-th{ width: 100px; }
            #vsTable::shadow .vs-${msg.tagLbl}-th{ width: 110px; }
            #vsTable::shadow .vs-${msg.amountLbl}-th{  width: 130px;  }
            #vsTable::shadow .rows .vs-${msg.amountLbl}-td{ text-align: right; }
            .pageTitle {color: #6c0404; font-size: 1.3em; text-align: center; margin:10px 0 10px 0 }
            .iconSelector {cursor: pointer;color: dodgerblue; font-size: 1.3em;}
        </style>
        <transaction-data id="transactionViewer"></transaction-data>
        <vs-datepicker id="datePicker" input-hidden month-labels='[${msg.monthsShort}]' day-labels='[${msg.weekdaysShort}]'
                       caption="${msg.selectWeekLbl}"></vs-datepicker>

        <div id="mainDiv" style="display: none;">
        <div style="padding: 0 20px;">
            <div class="horizontal layout center center-justified">
                <div class="flex"></div>
                <div class="pageTitle">
                    <span>{{userName}}</span>
                </div>
                <div class="flex" style="text-align: left;">
                    <i on-click="showUserInfo" class="fa fa-info-circle iconSelector" style="margin: 0 0 0 30px;font-size: 1.1em;"></i>
                </div>
            </div>
            <div class="horizontal layout center center-justified" style="text-align: center;color: #888; margin:0 0 8px 0;">
                <div id="previousWeekSelector"  on-click="goPreviousPeriod">
                    <i class="fa fa-angle-double-left iconSelector" style="margin: 0 20px 0 0; "></i></div>
                <div on-click="selectWeek" style="cursor: pointer;"><span>{{dateFromStr}}</span> - <span>{{dateToStr}}</span></div>
                <div id="nextWeekSelector" on-click="goNextPeriod">
                    <i class="fa fa-angle-double-right iconSelector" style="margin: 0 0 0 20px;"></i></div>
            </div>
            <div style="margin: 0 auto;max-width: 1000px;">
                <div style="color:#6c0404; text-align: center;text-decoration: underline;margin: 0px auto 10px auto;font-size: 1.1em;">
                    ${msg.cashLbl}
                </div>
                <div class="horizontal layout">
                    <template is="dom-repeat" items="{{curencyBalanceList}}" as="currencyData">
                        <div style="width:400px;border-left: 1px solid #bfbfbf;">
                            <div class="horizontal layout center" style="background-color: #efefef;">
                                <div style="color: #434343;font-size: 1.2em;width: 50px;margin: 0 0 0 20px;">{{currencyData.name}}</div>
                                <div class="vertical layout center">
                                    <div style="text-align: right;width: 130px;">{{currencyData.totalStr}} {{currencyData.symbol}}</div>
                                    <div hidden="{{!isTimeLimited(currencyData)}}">{{currencyData.timeLimitedStr}} {{currencyData.symbol}}</div>
                                </div>
                            </div>
                            <div class="horizontal layout wrap">
                                <template is="dom-repeat" items="{{getTagList(currencyData.name)}}" as="tag">
                                    <div class="horizontal layout center" style="margin: 0 0 0 20px; border-bottom: dashed 1px #ccc;height: 2em;">
                                        <div style="width: 60px;font-size: 0.8em;">{{tagDescription(tag)}}</div>
                                        <div class="vertical layout center" style="font-size: 0.7em;">
                                            <div style="text-align: right;width: 100px;">{{formatAmount(currencyData.name, tag)}} {{currencyData.symbol}}</div>
                                            <div hidden="{{!isTimeLimited(currencyData.name, tag)}}" title="{{getTimeLimitedForTagMsg(currencyData.name, tag)}}">
                                                {{getTimeLimitedForTag(currencyData.name, tag)}} {{currencyData.symbol}}</div>
                                        </div>
                                    </div>
                                </template>
                            </div>
                        </div>
                    </template>
                </div>
            </div>
        </div>

        <div id="movementsDiv">
            <div style="color:#6c0404; text-align: center;text-decoration: underline;margin: 15px auto;font-size: 1.1em;">
                <select id=periodSelect" style="margin:10px auto 0px auto;color:black; width: 250px;"
                        on-change="periodSelectChange" class="form-control" value="{{selectedPeriod}}">
                    <option value="WEEK" style="color:#6c0404;font-size: 1.2em;"> ${msg.weekMovementsLbl} </option>
                    <option value="MONTH" style="color:#6c0404;"> ${msg.monthMovementsLbl} </option>
                    <option value="YEAR" style="color:#6c0404;"> ${msg.yearMovementsLbl} </option>
                </select>

            </div>

            <div class="expensesTable horizontal layout" style="margin: 10px auto; width: 1100px;">
                <vs-table id="vsTable"
                          searchable
                          pagesize="1000"
                          pagetext="${msg.pageLbl}:"
                          pageoftext="${msg.ofLbl}"
                          itemoftext="${msg.ofLbl}">
                    <vs-column name="${msg.dateLbl}"
                               type="date"
                               searchable
                               sortable></vs-column>

                    <vs-column name="${msg.subjectLbl}"
                               type="string"
                               sortable
                               searchable></vs-column>

                    <vs-column name="${msg.tagLbl}"
                               type="string"
                               sortable
                               searchable></vs-column>

                    <vs-column name="${msg.amountLbl}"
                               type="amount"
                               sortable
                               searchable></vs-column>
                </vs-table>
                <div>
                    <filter-result-report id="filterResultReport"></filter-result-report>
                </div>
            </div>
        </div>

        </div>
    </template>
    <script>
        Polymer({
            is:'balance-user',
            properties: {
                url:{type:String, observer:'getHTTP'},
                selectedPeriod:{type:String, value:'WEEK'}, //one of [WEEK, MONTH, YEAR]
                balance: {type:Object, observer:'balanceChanged'},
                maxSubjectLength: {type:Number, value:50},
                baseDate:{type:Date, value:new Date()}
            },
            ready: function(e) {
                console.log(this.tagName + " - ready")
                this.$.datePicker.addEventListener('date-selected-accept', function(e) {
                    if(e.detail.getTime() > new Date().getTime()) alert("${msg.dateAfterTodayERRORMsg}")
                    else {
                        this.baseDate = e.detail
                        this.getHTTP("${contextURL}/rest/balance/user/id/" +
                                this.balance.user.id + e.detail.getURL(this.selectedPeriod))
                    }
                }.bind(this))
            },
            selectWeek:function() {
                this.$.datePicker.show()
            },
            showUserInfo: function(e) {
                alert(this.description)
            },
            formatAmount: function(currency, tag) {
                var amount = this.balance.balancesCash[currency][tag]
                if(typeof amount === 'number') return amount.toAmountStr()
                else return amount
            },
            tagDescription: function(tagName) {
                switch (tagName) {
                    case 'WILDTAG': return "${msg.wildTagLbl}".toUpperCase()
                    default: return tagName
                }
            },
            getTimeLimitedForTagAmount: function(currency, tag) {
                var expendedFromTag = 0
                if(this.balance.balancesFrom[currency] != null && this.balance.balancesFrom[currency][tag] != null) {
                    expendedFromTag = this.balance.balancesFrom[currency][tag]
                }
                expendedFromTag = expendedFromTag == null? 0 : expendedFromTag
                var tagToExpend = this.balance.balancesTo[currency][tag].timeLimited
                var tagCash = tagToExpend - expendedFromTag
                return tagCash
            },
            getTimeLimitedForTagMsg: function(currency, tag) {
                var tagCash = this.getTimeLimitedForTagAmount(currency, tag)
                return "${msg.timeLimitedForTagMsg}".format(tagCash.toAmountStr(), currency,  this.tagDescription(tag))
            },
            getTimeLimitedForTag: function(currency, tag) {
                var expendedFromTag
                if(this.balance.balancesFrom[currency] != null && this.balance.balancesFrom[currency][tag] != null) {
                    expendedFromTag = this.balance.balancesFrom[currency][tag]
                }
                expendedFromTag = (expendedFromTag == null? 0 : expendedFromTag)
                var tagToExpend = this.balance.balancesTo[currency][tag].timeLimited
                return tagToExpend - expendedFromTag
            },
            isTimeLimited: function(item, tag) {
                if(!tag) {
                    if(item.timeLimited > 0) return true;
                    else return false;
                } else {
                    if(this.balance.balancesTo[item][tag].timeLimited == 0) return false
                    else return true
                }
            },
            getTagList: function(currency) {
                return Object.keys(this.balance.balancesCash[currency]);
            },
            getMapKeys: function(e) {
                if(e == null) return []
                else return Object.keys(e)
            },
            getTableRow:function(transaction, origin) {
                if("from" === origin) {
                    amount = - transaction.amount
                } else if("to" === origin) {
                    amount = transaction.amount
                } else throw new Error('getTableRow - unknown transaction origin: ' + origin);
                return {${msg.subjectLbl}: transaction.subject.length > this.maxSubjectLength ?
                            transaction.subject.substring(0, this.maxSubjectLength) + "..." : transaction.subject,
                        ${msg.dateLbl}: new Date(transaction.dateCreated).toISOStr(),
                        ${msg.amountLbl}:{amount:amount, currencyCode:transaction.currencyCode},
                        ${msg.tagLbl}: transaction.tags[0],
                        'origin':origin, 'transaction':transaction}
            },
            balanceChanged:function() {
                console.log(this.tagName + " - this.balance: ", this.balance)
                this.description = "NIF:" + this.balance.user.nif + "<br/> IBAN: " + this.balance.user.iban
                this.userName = this.balance.user.name
                if('SYSTEM' == this.balance.user.type) {
                    this.description = "IBAN: " + this.balance.user.iban
                } else if('BANK' == this.balance.user.type) {
                } else if('USER' == this.balance.user.type) {
                    this.userName = this.balance.user.firstName + " " + this.balance.user.lastName
                }
                var tableRows = []
                this.balance.transactionFromList.forEach(function (element, index, array) {
                    tableRows.push(this.getTableRow(element, 'from'))
                }.bind(this))
                this.balance.transactionToList.forEach(function (element, index, array) {
                    tableRows.push(this.getTableRow(element, 'to'))
                }.bind(this))
                if(tableRows.length > 0) {
                    this.$.vsTable.data = JSON.parse(JSON.stringify(tableRows));//deep copy so that they have independent data source.
                    this.$.filterResultReport.tableRows = this.$.vsTable.data

                    this.$.vsTable.addEventListener('after-td-dbclick', function(e) {
                        console.log('after-td-click', e.detail);
                        this.$.transactionViewer.show(e.detail.row.transaction)
                    }.bind(this));
                    this.$.vsTable.addEventListener('filter', function (e) {
                        this.$.filterResultReport.tableRows = e.detail
                    }.bind(this))
                    this.$.movementsDiv.style.display = 'block'
                } else {
                    this.$.movementsDiv.style.display = 'none'
                }

                var curencyBalanceList = []
                Object.keys(this.balance.balancesCash).forEach(function(currency, currencyIndex, currencyArray) {
                    var currencyMap = {name:currency, total:0, timeLimited:0}
                    currencyMap.symbol = vs.getCurrencySymbol(currency)
                    Object.keys(this.balance.balancesCash[currency]).forEach(function(tag, tagIndex, tagArray) {
                        currencyMap.total = currencyMap.total + this.balance.balancesCash[currency][tag]
                        currencyMap.timeLimited = currencyMap.timeLimited + this.getTimeLimitedForTagAmount(currency, tag)
                    }.bind(this))
                    currencyMap.totalStr = currencyMap.total.toAmountStr()
                    currencyMap.timeLimitedStr = currencyMap.timeLimited.toAmountStr()
                    curencyBalanceList.push(currencyMap)
                }.bind(this))
                this.async(function(){
                    this.curencyBalanceList = curencyBalanceList
                }.bind(this))

                if(new Date().getMonday().getTime() === this.balance.timePeriod.dateFrom.getDate().getTime()) {
                    this.$.nextWeekSelector.style.display = 'none'
                } else this.$.nextWeekSelector.style.display = 'block'

                this.dateFromStr = this.balance.timePeriod.dateFrom.getDayWeekFormat()
                this.dateToStr = this.balance.timePeriod.dateTo.getDayWeekFormat()

                this.$.datePicker.setDate(this.balance.timePeriod.dateFrom.getDate())
                this.$.mainDiv.style.display = 'block'
            },
            goNextPeriod: function () {
                var currentPeriodEnd =  this.balance.timePeriod.dateTo.getDate();
                currentPeriodEnd.setDate(currentPeriodEnd.getDate() + 1); //one day from next period
                this.getHTTP("${contextURL}/rest/balance/user/id/" +
                        this.balance.user.id + currentPeriodEnd.getURL(this.selectedPeriod))
            },
            goPreviousPeriod: function () {
                var currentPeriodBegin =  this.balance.timePeriod.dateFrom.getDate();
                currentPeriodBegin.setDate(currentPeriodBegin.getDate() - 1); //one day from previous period
                this.getHTTP("${contextURL}/rest/balance/user/id/" + this.balance.user.id +
                        currentPeriodBegin.getURL(this.selectedPeriod))
            },
            periodSelectChange: function (e) {
                this.selectedPeriod = e.target.value
                switch(this.selectedPeriod) {
                    case 'WEEK':
                        this.$.datePicker.caption = "${msg.selectWeekLbl}"
                        break;
                    case 'MONTH':
                        this.$.datePicker.caption = "${msg.selectMonthLbl}"
                        break;
                    case 'YEAR':
                        this.$.datePicker.caption = "${msg.selectYearLbl}"
                        break;
                }
                console.log("periodSelectChange - selectedPeriod: " + this.selectedPeriod)
                this.getHTTP("${contextURL}/rest/balance/user/id/" + this.balance.user.id + this.baseDate.getURL(this.selectedPeriod))
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL + " - selectedPeriod:" + this.selectedPeriod)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.balance = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>

<dom-module name="filter-result-report">
    <template>
        <div style="min-width: 280px;">
            <template is="dom-repeat" items="{{currencyList}}" as="currencyData">
                <div class="horizontal layout center" style=" border-bottom: solid 1px #888;background-color: #efefef;">
                    <div style="color: #434343;font-size: 1.2em;width: 50px;padding:0 0 0 10px;">{{currencyData.name}}</div>
                    <div class="horizontal layout center" style="font-size: 0.9em;">
                        <div style="text-align: right;width: 120px;">{{currencyData.total}} {{currencyData.symbol}}</div>
                        <div style="border-left: 1px solid #888; padding: 0 0 0 3px;margin:0 0 0 3px;width: 120px;">
                            <div style="text-align: right;">{{currencyData.incomes}} {{currencyData.symbol}}</div>
                            <div style="color: red;text-align: right;">{{currencyData.expenses}} {{currencyData.symbol}}</div>
                        </div>
                    </div>
                </div>
                <div style="margin: 0 0 0 20px;">
                    <template is="dom-repeat" items="{{getTagList(currencyData.name)}}" as="tag">
                        <div class="horizontal layout center" style="margin: 3px 0 0 0; border-bottom: dashed 1px #ccc;">
                            <div style="width: 60px;font-size: 0.8em;">{{tag.name}}</div>
                            <div class="horizontal layout center" style="font-size: 0.6em;">
                                <div style="text-align: right;width: 100px;">{{tag.total}} {{currencyData.symbol}}</div>
                                <div style="border-left: 1px solid #888; padding: 0 0 0 3px;margin:0 0 0 3px;width: 120px;">
                                    <div style="text-align: right;">{{tag.incomes}} {{currencyData.symbol}}</div>
                                    <div style="color: red;text-align: right;">{{tag.expenses}} {{currencyData.symbol}}</div>
                                </div>
                            </div>
                        </div>
                    </template>
                </div>
            </template>
        </div>
    </template>
    <script>
        Polymer({
            is:'filter-result-report',
            properties: {
                tableRows: {type:Object, observer:'tableRowsChanged'},
                currencyList: {type:Object}
            },
            ready: function() {
            },
            tableRowsChanged: function() {
                this.currencyMap = {}
                this.currencyList = []
                this.tableRows.forEach(function(row, index, array) {
                    var transaction = row.transaction
                    if(this.currencyMap[transaction.currencyCode]) {
                        tagMap = this.currencyMap[transaction.currencyCode]
                    } else {
                        tagMap = {}
                        this.currencyMap[transaction.currencyCode] = tagMap
                    }
                    if(tagMap[transaction.tags[0]]) {
                        tagBalanceMap = tagMap[transaction.tags[0]]
                    } else {
                        tagBalanceMap = {}
                        tagMap[transaction.tags[0]] = tagBalanceMap
                    }
                    this.updateTagBalanceMap(tagBalanceMap, transaction.amount, row.origin)
                }.bind(this))

                var currencyList = []
                Object.keys(this.currencyMap).forEach(function(currency, currencyIndex, currencyArray) {
                    var currencyData = {}
                    var taglist = Object.keys(this.currencyMap[currency])
                    taglist.forEach(function(tag, tagIndex, tagArray) {
                        var tagBalanceMap = this.currencyMap[currency][tag]
                        currencyData.name = currency
                        currencyData.symbol = vs.getCurrencySymbol(currencyData.name)
                        if(currencyData.total) {
                            currencyData.total = currencyData.total + tagBalanceMap.total
                            currencyData.incomes = currencyData.incomes + tagBalanceMap.incomes
                            currencyData.expenses = currencyData.expenses + tagBalanceMap.expenses
                        } else {
                            currencyData.total = tagBalanceMap.total
                            currencyData.incomes = tagBalanceMap.incomes
                            currencyData.expenses = tagBalanceMap.expenses
                        }
                    }.bind(this))
                    currencyList.push(this.formatMap(currencyData))
                    this.async(function(){
                        this.currencyList = currencyList
                    }.bind(this))
                }.bind(this))
            },
            updateTagBalanceMap:function(tagBalanceMap, amount, origin) {
                if("to" === origin) {
                } else if ("from" === origin) {
                    amount = -amount
                } else throw new Error('getTableRow - unknown transaction origin: ' + row.origin);
                if(tagBalanceMap.total) {
                    if(amount > 0) tagBalanceMap.incomes += amount
                    else tagBalanceMap.expenses += amount
                    tagBalanceMap.total += amount
                } else {
                    if(amount > 0) {
                        tagBalanceMap.incomes = amount
                        tagBalanceMap.expenses = 0
                    } else {
                        tagBalanceMap.incomes = 0
                        tagBalanceMap.expenses = amount
                    }
                    tagBalanceMap.total = amount
                }
            },
            getTagList: function(currency) {
                var result = [];
                Object.keys(this.currencyMap[currency]).forEach(function(tag, taIndex, tagArray) {
                    var tagData = this.currencyMap[currency][tag]
                    tagData.name = tag
                    result.push(this.formatMap(tagData))
                }.bind(this));
                return result
            },
            formatMap: function(map) {
                map.total = map.total.toAmountStr()
                map.incomes = map.incomes.toAmountStr()
                map.expenses = map.expenses.toAmountStr()
                return map
            }
        });
    </script>
</dom-module>