<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module id="system-accounts">
    <template>
        <style>
            .accountBlock { border: 1px solid #888; margin: 5px 10px 20px 10px;
                background-color: #f9f9f9; border-radius: 2px;min-width: 100px;
                box-shadow: 0 3px 3px 0 rgba(0, 0, 0, 0.24); cursor: pointer; display: table;
            }
            .accountBalance { font-size: 1.2em; color: #434343; text-align: center;  padding: 10px; }
            .tagDesc { background: #888; color: #f9f9f9; padding: 5px;text-align: center; }
            .sectionHeader {
                color: #f9f9f9;
                text-align: center;
                font-size: 1.2em;
                margin: 0 0 15px 0; font-size: 1.2em;
                text-decoration: none; background-color: #888;width: 100%;
            }
            .subSection {
                border-bottom: 1px solid #ccc;width: 100%;margin: 10px 10px 0 10px;
            }
            .currencyInfo {color:#888;border-bottom: 1px solid #888;font-weight: bold;}
        </style>
        <div class="pagevs vertical layout center">
            <h3 class="sectionHeader">${msg.systemAccountsLbl}</h3>
            <template is="dom-repeat" items="{{systemAccountsCurrencyList}}" as="currencyCode">
                <div class="currencyInfo">
                    {{getSystemCurrencyCodeAmount('systemAccounts', currencyCode)}} {{currencyCode}}</div>
                <div class="layout horizontal center center-justified subSection">
                    <template is="dom-repeat" items="{{getSystemAccountsList(currencyCode)}}" as="systemAccount">
                        <div class="accountBlock">
                            <div class="accountBalance"><span>{{systemAccount.amount}}</span> <span style="font-size: 0.9em;">
                                {{getCurrencySymbol(systemAccount.currencyCode)}}</span></div>
                            <div class="tagDesc">{{systemAccount.tag.name}}</div>
                        </div>
                    </template>
                </div>
            </template>
            <h3 class="sectionHeader">${msg.userAccountsLbl}</h3>
            <template is="dom-repeat" items="{{userAccountsCurrencyList}}" as="currencyCode">
                <div class="currencyInfo">
                    {{getSystemCurrencyCodeAmount('userAccounts', currencyCode)}} {{currencyCode}}</div>
                <div class="layout horizontal center center-justified subSection">
                    <template is="dom-repeat" items="{{getUserAccountsTagList(currencyCode)}}" as="tag">
                        <div class="accountBlock">
                            <div class="accountBalance"><span>{{tag.amount}}</span> <span style="font-size: 0.9em;">
                                {{getCurrencySymbol(tag.currencyCode)}}</span></div>
                            <div class="tagDesc">{{tag.name}}</div>
                        </div>
                    </template>
                </div>
            </template>

            <h3 class="sectionHeader">${msg.bankInputsLbl}</h3>
            <template is="dom-repeat" items="{{bankInputsCurrencyList}}" as="currencyCode">
                <div class="currencyInfo">
                    {{getSystemCurrencyCodeAmount('bankInputs', currencyCode)}} {{currencyCode}}</div>
                <div class="layout horizontal center center-justified subSection">
                    <template is="dom-repeat" items="{{getBankInputsTagList(currencyCode)}}" as="tag">
                        <div class="accountBlock">
                            <div class="accountBalance"><span>{{tag.amount}}</span> <span style="font-size: 0.9em;">
                                {{getCurrencySymbol(tag.currencyCode)}}</span></div>
                            <div class="tagDesc">{{tag.name}}</div>
                        </div>
                    </template>
                </div>
            </template>


        </div>
    </template>
    <script>
        Polymer({
            is:'system-accounts',
            properties: {
                url:{type:String, value: vs.contextURL + '/rest/currencyAccount/system', observer:'getHTTP'}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            getSystemCurrencyCodeAmount:function(selectedSection,currencyCode) {
                return this.accountsInfoDto[selectedSection].amounts[currencyCode]
            },
            getSystemAccountsList: function (currencyCode) {
                return this.accountsInfoDto.systemAccounts[currencyCode];
            },
            getUserAccountsTagList: function (currencyCode) {
                return this.accountsInfoDto.userAccounts[currencyCode];
            },
            getBankInputsTagList: function (currencyCode) {
                return this.accountsInfoDto.bankInputs[currencyCode];
            },
            calculateMapTotals:function(map) {
                var currencyList = Object.keys(map)
                map.amounts = {}
                currencyList.forEach(function(currencyCode, index) {
                    var currencyItemList = map[currencyCode]
                    var currencyAmount = 0
                    currencyItemList.forEach(function(element, index) {
                        currencyAmount += element.amount
                    })
                    map.amounts[currencyCode] = currencyAmount
                })
            },
            getCurrencySymbol:function(currencyCode) {
                return vs.getCurrencySymbol(currencyCode)
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                vs.getHTTPJSON(targetURL, function(responseText){
                    this.accountsInfoDto = toJSON(responseText)
                    this.systemAccountsCurrencyList = Object.keys(this.accountsInfoDto.systemAccounts)
                    this.userAccountsCurrencyList = Object.keys(this.accountsInfoDto.userAccounts)
                    this.bankInputsCurrencyList = Object.keys(this.accountsInfoDto.bankInputs)
                    this.calculateMapTotals(this.accountsInfoDto.systemAccounts);
                    this.calculateMapTotals(this.accountsInfoDto.userAccounts);
                    this.calculateMapTotals(this.accountsInfoDto.bankInputs);
                }.bind(this))
            }
        })
    </script>
</dom-module>