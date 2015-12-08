<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module id="system-accounts">
    <template>
        <style>
            .accountBlock { border: 1px solid #6c0404; margin: 20px;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24); cursor: pointer;
            }
            .accountBalance { font-size: 2em; color: #6c0404; text-align: center;  padding: 10px; }
            .tagDesc { background: #6c0404; color: #f9f9f9; padding: 5px;text-align: center; }
            .sectionHeader { font-size: 1.8em; font-weight: bold; color: #ba0011;text-align: center;text-decoration: underline; }
        </style>
        <div class="sectionHeader">${msg.systemAccountsLbl}</div>
        <div class="layout flex horizontal wrap around-justified">
            <template is="dom-repeat" items="{{systemAccountsDto.accountList}}">
                <div>
                    <div class="accountBlock">
                        <div class="accountBalance"><span>{{item.amount}}</span> <span>{{item.currencyCode}}</span></div>
                        <div class="tagDesc">{{item.tag.name}}</div>
                    </div>
                </div>
            </template>
        </div>
        <div class="sectionHeader" style="margin: 30px 0 0 0;">${msg.userVSAccountsLbl}</div>
        <div class="layout flex horizontal wrap around-justified">
            <template is="dom-repeat" items="{{systemAccountsDto.tagVSBalanceList}}">
                <div class="accountBlock">
                    <div class="accountBalance"><span>{{item.amount}}</span> <span>{{item.currencyCode}}</span></div>
                    <div class="tagDesc">{{item.name}}</div>
                </div>
            </template>
        </div>
        <div class="sectionHeader" style="margin: 30px 0 0 0;">${msg.bankVSInputsLbl}</div>
        <div class="layout flex horizontal wrap around-justified">
            <template is="dom-repeat" items="{{systemAccountsDto.bankVSBalanceList}}">
                <div class="accountBlock">
                    <div class="accountBalance"><span>{{item.amount}}</span> <span>{{item.currencyCode}}</span></div>
                    <div class="tagDesc">{{item.name}}</div>
                </div>
            </template>
        </div>
    </template>
    <script>
        Polymer({
            is:'system-accounts',
            properties: {
                systemAccountsDto: {type:Object},
                url:{type:String, value: contextURL + '/rest/currencyAccount/system', observer:'getHTTP'}
            },
            ready: function() {
                console.log(this.tagName + " - ready - ")
            },
            systemAccountsDtoChanged: function() {
                console.log(this.tagName + " - systemAccountsDto: " + this.systemAccountsDto)
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.systemAccountsDto = toJSON(rawData.response)
                }.bind(this));
            }
        })
    </script>
</dom-module>