<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-table/vs-table.html" rel="import"/>
<link href="../transaction/transaction-data.vsp" rel="import"/>

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
                font-weight: bold;
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
            #vsTable::shadow .vs-${msg.subjectLbl}-th{
                width:300px;
            }
            #vsTable::shadow .vs-${msg.dateLbl}-th{
                width: 100px;
            }
            #vsTable::shadow .vs-${msg.tagLbl}-th{
                width: 110px;
            }
            #vsTable::shadow .vs-${msg.amountLbl}-td{
                text-align: right;
                width: 100px;
            }
            .pageTitle {color: #6c0404; font-weight: bold; font-size: 1.3em; text-align: center;
                margin:10px 0 10px 0; cursor: pointer;}
            .pageTitle:hover { text-decoration: underline; }
        </style>
        <transaction-data id="transactionViewer"></transaction-data>
        <div style="padding: 0 20px;">
            <div class="horizontal layout center center-justified">
                <div class="flex"></div>
                <div class="pageTitle" on-click="goToUserPage">{{userName}}</div>
                <div class="flex" style="font-size: 0.7em; color: #888; font-weight: normal; text-align: right;">{{description}}</div>
            </div>
            <div class="horizontal layout center center-justified" style="text-align: center;font-weight: bold; color: #888; margin:0 0 8px 0;">
                <div><span>{{getDate(balance.timePeriod.dateFrom)}}</span> - <span>{{getDate(balance.timePeriod.dateTo)}}</span></div>
            </div>
            <div style="margin: 0 auto;max-width: 1000px;">
                <div style="font-weight: bold;color:#6c0404; text-align: center;text-decoration: underline;margin: 0px auto 10px auto;font-size: 1.1em;">
                    ${msg.cashLbl}
                </div>
                <div class="vertical layout center center-justified">
                    <div>
                        <template is="dom-repeat" items="{{curencyBalanceList}}" as="currencyData">
                            <div class="horizontal layout center">
                                <div style="color: #888;font-size: 1.2em;width: 40px;margin: 0 0 0 20px;">{{currencyData.name}}</div>
                                <div class="vertical layout center">
                                    <div style="text-align: right;width: 100px;">{{currencyData.totalStr}} {{currencyData.symbol}}</div>
                                    <div hidden="{{!isTimeLimited(currencyData)}}">{{currencyData.timeLimitedStr}} {{currencyData.symbol}}</div>
                                </div>
                            </div>
                            <div class="horizontal layout wrap" style="width:400px;">
                                <template is="dom-repeat" items="{{getTagList(currencyData.name)}}" as="tag">
                                    <div class="horizontal layout center" style="margin: 0 0 0 20px; border-bottom: dashed 1px #ccc;height: 2em;">
                                        <div style="width: 50px;font-size: 0.8em;">{{tagDescription(tag)}}</div>
                                        <div class="vertical layout center" style="font-size: 0.7em;">
                                            <div style="text-align: right;width: 80px;">{{formatAmount(currencyData.name, tag)}} {{currencyData.symbol}}</div>
                                            <div hidden="{{!isTimeLimited(currencyData.name, tag)}}" title="{{getTimeLimitedForTagMsg(currencyData.name, tag)}}">
                                                {{getTimeLimitedForTag(currencyData.name, tag)}} {{currencyData.symbol}}</div>
                                        </div>
                                    </div>
                                </template>
                            </div>
                        </template>
                    </div>
                </div>
            </div>
        </div>

        <div style="font-weight: bold;color:#6c0404; text-align: center;text-decoration: underline;margin: 15px auto;font-size: 1.1em;">
            ${msg.weekMovementsLbl}
        </div>
        <div class="horizontal layout center center-justified" style="margin: 10px auto;">
            <vs-table id="vsTable"
                      searchable
                      pagesize="100"
                      pagetext="${msg.pageLbl}:"
                      pageoftext="${msg.ofLbl}"
                      itemoftext="${msg.ofLbl}">
                <vs-column name="${msg.dateLbl}"
                           type="date"
                           searchable
                           sortable
                           required></vs-column>

                <vs-column name="${msg.subjectLbl}"
                           type="string"
                           sortable
                           searchable
                           required
                           default=""></vs-column>

                <vs-column name="${msg.tagLbl}"
                           type="string"
                           sortable
                           searchable
                           required
                           default=""></vs-column>

                <vs-column name="${msg.amountLbl}"
                           type="html"
                           sortable
                           searchable
                           required
                           default=""></vs-column>

                <vs-column name="${msg.currencyLbl}"
                           type="html"
                           searchable
                           sortable
                           required
                           data-choices='{"":"", "EUR":"Euro", "USD":"Dollar", "CNY":"Yuan", "JPY":"Yen"}'></vs-column>
            </vs-table>

            <div style="height: 100%;">
                <div>
                    <filter-result-report id="filterResultReport"></filter-result-report>
                </div>
                <div class="flex"></div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'balance-user',
            properties: {
                url:{type:String, observer:'getHTTP'},
                balance: {type:Object, observer:'balanceChanged'},
                description: {type:String}
            },
            ready: function(e) {
                console.log(this.tagName + " - ready")
                //var toMap = {"EUR":{"HIDROGENO":880.5, "NITROGENO":100},  "DOLLAR":{"WILDTAG":345}}
                //var fromMap = {"EUR":{"HIDROGENO":580.5, "OXIGENO":250}, "DOLLAR":{"WILDTAG":245}}
                //var resultCalc = calculateBalanceResultMap(toMap, fromMap)
                //calculateUserBalanceSeries(toMap.EUR, fromMap.EUR, {})
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
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
            goToUserPage:function() {
                page.show(vs.contextURL + "/rest/user/id/" + this.balance.user.id)
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
                    currencyInfo =  "<span style='color: red;'>" + transaction.currencyCode + "</span>"
                    amountInfo = "<span style='color: red;'>" + transaction.amount.toAmountStr() + "</span>"
                } else if("to" === origin) {
                    currencyInfo =  transaction.currencyCode
                    amountInfo = transaction.amount.toAmountStr()
                } else throw new Error('getTableRow - unknown transaction origin: ' + origin);

                return { ${msg.subjectLbl}: transaction.subject,
                        ${msg.dateLbl}: new Date(transaction.dateCreated).getDayWeekFormat(),
                        ${msg.amountLbl}:amountInfo,
                        ${msg.currencyLbl}:currencyInfo,
                        ${msg.tagLbl}: transaction.tags[0],
                        'origin':origin, 'transaction':transaction}
            },
            balanceChanged:function() {
                console.log(this.tagName + " - this.balance: ", this.balance)
                this.description = "NIF:" + this.balance.user.nif + " - IBAN: " + this.balance.user.iban
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
                this.$.vsTable.data = JSON.parse(JSON.stringify(tableRows));//deep copy so that they have independent data source.
                this.$.filterResultReport.tableRows = this.$.vsTable.data

                this.$.vsTable.addEventListener('after-td-click', function(e) {
                    console.log('after-td-click', e.detail);
                    this.$.transactionViewer.show(e.detail.row.transaction)
                }.bind(this));
                this.$.vsTable.addEventListener('filter', function (e) {
                    console.log("vsTable - NoFilteredRows:", e.detail);
                    this.$.filterResultReport.tableRows = e.detail
                }.bind(this))

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
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
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
                <div class="horizontal layout center" style=" border-bottom: solid 1px #888; border-top: solid 1px #888;">
                    <div style="color: #888;font-size: 1.2em;width: 70px;">{{currencyData.name}}</div>
                    <div class="horizontal layout center" style="font-size: 0.9em;">
                        <div style="text-align: right;width: 100px;">{{currencyData.total}} {{currencyData.symbol}}</div>
                        <div style="border-left: 1px solid #888; padding: 0 0 0 3px;margin:0 0 0 3px;width: 100px;">
                            <div style="text-align: right;">{{currencyData.incomes}} {{currencyData.symbol}}</div>
                            <div style="color: red;text-align: right;">{{currencyData.expenses}} {{currencyData.symbol}}</div>
                        </div>
                    </div>
                </div>
                <div style="margin: 0 0 0 20px;">
                    <template is="dom-repeat" items="{{getTagList(currencyData.name)}}" as="tag">
                        <div class="horizontal layout center" style="margin: 3px 0 0 0; border-bottom: dashed 1px #ccc;">
                            <div style="width: 70px;font-size: 0.8em;">{{tag.name}}</div>
                            <div class="horizontal layout center" style="font-size: 0.6em;">
                                <div style="text-align: right;width: 80px;">{{tag.total}} {{currencyData.symbol}}</div>
                                <div style="border-left: 1px solid #888; padding: 0 0 0 3px;margin:0 0 0 3px;width: 100px;">
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
                console.log("currencyMap", this.currencyMap)


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