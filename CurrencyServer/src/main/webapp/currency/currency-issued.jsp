<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module id="currency-issued">
    <template>
        <style>
            .currencyIssuedBlock { border: 1px solid #6c0404; margin: 20px;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24); cursor: pointer;
            }
            .currencyIssuedBalance { font-size: 2em; color: #6c0404; text-align: center;  padding: 10px; }
            .tagDesc { background: #6c0404; color: #f9f9f9; padding: 5px;text-align: center; }
            .sectionHeader { font-size: 1.8em; font-weight: bold; color: #ba0011;text-align: center;text-decoration: underline; }
        </style>
        <div hidden="{{okListHidden}}">
            <div class="sectionHeader">${msg.activesLbl}</div>
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
            <div class="sectionHeader">${msg.expendedLbl}</div>
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
            <div class="sectionHeader">${msg.lapsedLbl}</div>
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
            <div class="sectionHeader">${msg.errorLbl}</div>
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
    </template>
    <script>
        Polymer({
            is:'currency-issued',
            properties: {
                url:{type:String, observer:'getHTTP'},
                currencyIssuedDto:{type:Object, observer:'currencyIssuedDtoChanged'}
            },
            ready: function() {
                console.log(this.tagName + " - ready - ")
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
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.currencyIssuedDto = toJSON(rawData.response)
                }.bind(this));
            }
        })
    </script>
</dom-module>