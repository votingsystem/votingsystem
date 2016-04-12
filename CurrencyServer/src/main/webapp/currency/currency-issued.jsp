<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module id="currency-issued">
    <template>
        <style>
            .currencyIssuedBlock { border: 1px solid #888; margin: 5px;
                box-shadow: 0 8px 6px -6px #888;min-width: 100px;
            }
            .currencyIssuedBalance { font-size: 1.2em; color: #888; text-align: center;  padding: 2px; }
            .tagDesc { background: #888; color: #f9f9f9; padding: 2px;text-align: center; font-size: 0.9em;}
        </style>
        <div class="pagevs vertical layout center" >
            <template is="dom-repeat" items="{{currencyList}}" as="currencyCode">
                <h2 style="border-top: 1px dotted #888;width: 100%;text-align: center;">{{currencyCode}}</h2>
                <div hidden="{{currencyStateHidden('okList', currencyCode)}}">
                    <h3 class="sectionHeader" style="margin: 0 0 10px 0;">${msg.activesLbl}</h3>
                    <div class="layout flex horizontal wrap around-justified">
                        <template is="dom-repeat" items="{{tagList('okList',currencyCode)}}">
                            <div>
                                <div class="currencyIssuedBlock">
                                    <div class="currencyIssuedBalance"><span>{{item.amount}}</span> <span>{{item.currencyCode}}</span></div>
                                    <div class="tagDesc">{{item.name}}</div>
                                </div>
                            </div>
                        </template>
                    </div>
                </div>
                <div hidden="{{currencyStateHidden('expendedList', currencyCode)}}" style="margin: 15px 0 0 0;">
                    <h3 class="sectionHeader">${msg.expendedLbl}</h3>
                    <div class="layout flex horizontal wrap around-justified">
                        <template is="dom-repeat" items="{{tagList('expendedList',currencyCode)}}">
                            <div>
                                <div class="currencyIssuedBlock">
                                    <div class="currencyIssuedBalance"><span>{{item.amount}}</span> <span>{{item.currencyCode}}</span></div>
                                    <div class="tagDesc">{{item.name}}</div>
                                </div>
                            </div>
                        </template>
                    </div>
                </div>
                <div hidden="{{currencyStateHidden('lapsedList', currencyCode)}}" style="margin: 15px 0 0 0;">
                    <h3 class="sectionHeader">${msg.lapsedLbl}</h3>
                    <div class="layout flex horizontal wrap around-justified">
                        <template is="dom-repeat" items="{{tagList('lapsedList',currencyCode)}}">
                            <div>
                                <div class="currencyIssuedBlock">
                                    <div class="currencyIssuedBalance"><span>{{item.amount}}</span> <span>{{item.currencyCode}}</span></div>
                                    <div class="tagDesc">{{item.name}}</div>
                                </div>
                            </div>
                        </template>
                    </div>
                </div>
                <div hidden="{{currencyStateHidden('errorList', currencyCode)}}" style="margin: 15px 0 0 0;">
                    <h3 class="sectionHeader">${msg.errorLbl}</h3>
                    <div class="layout flex horizontal wrap around-justified">
                        <template is="dom-repeat" items="{{tagList('errorList',currencyCode)}}">
                            <div>
                                <div class="currencyIssuedBlock">
                                    <div class="currencyIssuedBalance"><span>{{item.amount}}</span> <span>{{item.currencyCode}}</span></div>
                                    <div class="tagDesc">{{item.name}}</div>
                                </div>
                            </div>
                        </template>
                    </div>
                </div>
            </template>
        </div>
    </template>
    <script>
        Polymer({
            is:'currency-issued',
            properties: { },
            ready: function() {
                console.log("ready");
                this.getHTTP(vs.contextURL + "/rest/currency/issued");
            },
            currencyStateHidden: function(currencyState, currencyCode) {
                if(this.currencyIssuedDto[currencyCode][currencyState].length === 0) return true;
                else return false;
            },
            tagList: function(currencyState, currencyCode) {
                return this.currencyIssuedDto[currencyCode][currencyState]
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log("getHTTP - targetURL: " + targetURL)
                vs.getHTTPJSON(targetURL, function(responseText){
                    this.currencyIssuedDto = toJSON(responseText)
                    this.currencyList = Object.keys(this.currencyIssuedDto)
                }.bind(this))
            }
        })
    </script>
</dom-module>