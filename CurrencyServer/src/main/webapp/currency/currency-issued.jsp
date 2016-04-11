<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module id="currency-issued">
    <template>
        <style>
            .currencyIssuedBlock { border: 1px solid #6c0404; margin: 20px;
                box-shadow: 0 8px 6px -6px #888;min-width: 100px;
            }
            .currencyIssuedBalance { font-size: 1.2em; color: #6c0404; text-align: center;  padding: 2px; }
            .tagDesc { background: #6c0404; color: #f9f9f9; padding: 2px;text-align: center; font-size: 0.9em;}
        </style>
        <div class="pagevs">
            <div hidden="{{okListHidden}}">
                <h3 class="sectionHeader">${msg.activesLbl}</h3>
                <div class="layout flex horizontal wrap around-justified">
                    <template is="dom-repeat" items="{{currencyIssuedDto.okList}}">
                        <div>
                            <div class="currencyIssuedBlock">
                                <div class="currencyIssuedBalance"><span>{{item.amount}}</span> <span>{{item.currencyCode}}</span></div>
                                <div class="tagDesc">{{item.name}}</div>
                            </div>
                        </div>
                    </template>
                </div>
            </div>
            <div hidden="{{expendedListHidden}}" style="margin: 15px 0 0 0;">
                <h3 class="sectionHeader">${msg.expendedLbl}</h3>
                <div class="layout flex horizontal wrap around-justified">
                    <template is="dom-repeat" items="{{currencyIssuedDto.expendedList}}">
                        <div>
                            <div class="currencyIssuedBlock">
                                <div class="currencyIssuedBalance"><span>{{item.amount}}</span> <span>{{item.currencyCode}}</span></div>
                                <div class="tagDesc">{{item.name}}</div>
                            </div>
                        </div>
                    </template>
                </div>
            </div>
            <div hidden="{{lapsedListHidden}}" style="margin: 15px 0 0 0;">
                <h3 class="sectionHeader">${msg.lapsedLbl}</h3>
                <div class="layout flex horizontal wrap around-justified">
                    <template is="dom-repeat" items="{{currencyIssuedDto.lapsedList}}">
                        <div>
                            <div class="currencyIssuedBlock">
                                <div class="currencyIssuedBalance"><span>{{item.amount}}</span> <span>{{item.currencyCode}}</span></div>
                                <div class="tagDesc">{{item.name}}</div>
                            </div>
                        </div>
                    </template>
                </div>
            </div>
            <div hidden="{{errorListHidden}}" style="margin: 15px 0 0 0;">
                <h3 class="sectionHeader">${msg.errorLbl}</h3>
                <div class="layout flex horizontal wrap around-justified">
                    <template is="dom-repeat" items="{{currencyIssuedDto.errorList}}">
                        <div>
                            <div class="currencyIssuedBlock">
                                <div class="currencyIssuedBalance"><span>{{item.amount}}</span> <span>{{item.currencyCode}}</span></div>
                                <div class="tagDesc">{{item.name}}</div>
                            </div>
                        </div>
                    </template>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'currency-issued',
            properties: {
                url:{type:String, observer:'getHTTP'},
                currencyCode:{type:String, observer:'currencyCodeChanged'},
                currencyIssuedDto:{type:Object, observer:'currencyIssuedDtoChanged'}
            },
            ready: function() {
                console.log(this.tagName + " - ready - ")
            },
            currencyCodeChanged: function() {
                this.getHTTP(vs.contextURL + "/rest/currency/issued/currencyCode/" + this.currencyCode)
            },
            currencyIssuedDtoChanged: function() {
                console.log(this.tagName + " - currencyIssuedDto: " + this.currencyIssuedDto)
                this.okListHidden = (this.currencyIssuedDto.okList.length == 0)
                this.expendedListHidden = (this.currencyIssuedDto.expendedList.length == 0)
                this.lapsedListHidden = (this.currencyIssuedDto.lapsedList.length == 0)
                this.errorListHidden = (this.currencyIssuedDto.errorList.length == 0)
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                vs.getHTTPJSON(targetURL, function(responseText){
                    this.currencyIssuedDto = toJSON(responseText)
                }.bind(this))
            }
        })
    </script>
</dom-module>