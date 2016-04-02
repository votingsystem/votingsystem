<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="bank-list">
    <template>
        <style>
            .bank {border: 1px solid #6c0404; margin: 10px; padding:0 10px 10px 10px;
                box-shadow: 0 5px 5px 0 #bbb; text-align: center;
                cursor: pointer; text-overflow: ellipsis; max-width: 300px; border-radius: 4px;
            }
            .bank:hover { border-color: orange; }
        </style>
        <div class="pagevs">
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{bankListDto}}" as="bank">
                    <div class="bank" on-click="showDetails">
                        <div class="nameColumn" style="font-size: 1.2em;">{{bank.name}}</div>
                        <div class="descriptionColumn">{{bank.description}}</div>
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
                page.show(vs.contextURL + "/rest/user/id/" + e.model.bank.id)
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
