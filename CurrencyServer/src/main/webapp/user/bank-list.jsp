<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="bank-list">
    <template>
        <style>
            .bank {border: 1px solid #6c0404; margin: 10px; padding:0 10px 10px 10px;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24); text-align: center;
                cursor: pointer; text-overflow: ellipsis; max-width: 300px;-moz-border-radius: 3px; border-radius: 4px;
            }
        </style>
        <div class="layout flex horizontal wrap around-justified">
            <template is="dom-repeat" items="{{bankListDto}}" as="bank">
                <div class="bank" on-click="showDetails">
                    <div class="nameColumn" style="font-size: 1.2em;">{{bank.name}}</div>
                    <div class="descriptionColumn">{{bank.description}}</div>
                </div>
            </template>
        </div>
    </template>
    <script>
        Polymer({
            is:'bank-list',
            properties: {
                bankListDto: {type:Object, value: {}},
                url: {type:String, value: vs.contextURL + "/rest/user/bankList", observer:'getHTTP'}
            },
            ready: function() { console.log(this.tagName + " - ready")},
            showDetails: function(e) {
                page.show(vs.contextURL + "/rest/user/id/" + e.model.bank.id)
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                new XMLHttpRequest().header("Content-Type", "application/json").get(targetURL, function(responseText){
                    this.bankListDto = toJSON(responseText)
                }.bind(this));
            }
        });
    </script>
</dom-module>
