<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="bank-list">
    <template>
        <style>
            .bank {border: 1px solid #ccc; margin: 10px; padding:3px 7px 7px 7px;
                box-shadow: 0 2px 2px 0 #bbb; text-align: center; background-color: #f9f9f9;
                cursor: pointer; text-overflow: ellipsis; max-width: 300px; border-radius: 3px;
            }
            .bank:hover { border-color: #ba0011; }
            .bank:active { background-color: #eee;}
        </style>
        <div class="pagevs">
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{bankListDto}}" as="bank">
                    <div class="bank" on-click="showDetails">
                        <div style="font-size: 1.2em; border-bottom: 1px dotted #6c0404;margin: 0 0 2px 0;">{{bank.name}}</div>
                        <div>{{bank.description}}</div>
                    </div>
                </template>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'bank-list',
            properties: {
                url: {type:String, value: vs.contextURL + "/rest/user/bankList", observer:'getHTTP'}
            },
            ready: function() { console.log(this.tagName + " - ready")},
            showDetails: function(e) {
                page.show(vs.contextURL + "/rest/currencyAccount/bank/id/" + e.model.bank.id)
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                vs.getHTTPJSON(targetURL, function(responseText){
                    this.bankListDto = toJSON(responseText)
                }.bind(this))
            }
        });
    </script>
</dom-module>
