<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module id="user-accounts">
    <template>
        <style>
            .accountBlock { border: 1px solid #888; margin: 0px 10px 10px 0px;
                background-color: #f9f9f9; border-radius: 2px;min-width: 100px;
                box-shadow: 0 8px 6px -6px #888;cursor: pointer; display: table;
            }
            .accountBalance { font-size: 1.2em; color: #434343; text-align: center;  padding: 2px; }
            .tagDesc { background: #888; color: #f9f9f9; padding: 2px;text-align: center; font-size: 0.9em;}
            .sectionHeader {
                color: #f9f9f9;
                text-align: center;
                font-size: 1.2em;
                margin: 0 0 15px 0; font-size: 1.2em;
                text-decoration: none; background-color: #888;width: 100%;
            }
            .subSection { border-bottom: 1px solid #ccc;width: 100%;margin: 10px 10px 0 10px; }
            .currencyInfo {color:#888;border-bottom: 1px solid #888;font-weight: bold;}
        </style>
        <div class="pagevs vertical layout center">
            <h3 class="sectionHeader">${msg.accountsLbl}</h3>
            <template is="dom-repeat" items="{{userAccountsCurrencyList}}" as="currencyCode">
                <div class="currencyInfo">
                    {{getCurrencyCodeAmount('userAccounts', currencyCode)}} {{currencyCode}}</div>
                <div class="layout horizontal center center-justified subSection">
                    <template is="dom-repeat" items="{{getAccountList(currencyCode)}}" as="account">
                        <div class="accountBlock">
                            <div class="accountBalance"><span>{{account.amount}}</span> <span style="font-size: 0.9em;">
                                {{getCurrencySymbol(account.currencyCode)}}</span></div>
                            <div class="tagDesc">{{account.tag.name}}</div>
                        </div>
                    </template>
                </div>
            </template>
        </div>
    </template>
    <script>
        Polymer({
            is:'user-accounts',
            properties: {
                url:{type:String, value: vs.contextURL + '/rest/currencyAccount/user', observer:'getHTTP'}
            },
            getCurrencyCodeAmount:function(selectedSection,currencyCode) {
                return this.accountsInfoDto.amounts[currencyCode]
            },
            getStr: function (tag) {
                return JSON.stringify(tag);
            },
            getAccountList: function (currencyCode) {
                return this.accountsInfoDto[currencyCode];
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
                vs.getHTTPJSON(targetURL, function(responseText, statusCode){
                    this.accountsInfoDto = toJSON(responseText)
                    this.userAccountsCurrencyList = Object.keys(this.accountsInfoDto)
                    this.calculateMapTotals(this.accountsInfoDto);
                }.bind(this))
            }
        })
    </script>
</dom-module>