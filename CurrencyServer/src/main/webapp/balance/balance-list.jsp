<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="balance-list">
    <template>
        <style>
            .tableHeadervs {
                margin: 0px 0px 0px 0px;
                color:#6c0404;
                border-bottom: 1px solid #6c0404;
                background: white;
                font-weight: bold;
                padding:5px 0px 5px 0px;
                width: 100%;
            }
            .tableHeadervs div {
                text-align:center;
            }
            .rowvs {
                border-bottom: 1px solid #ccc;
                padding: 10px 0px 10px 0px;
                width: 100%;
            }
            .rowvs div {  text-align:center; }
        </style>
        <!--JavaFX Webkit gives problems with tables and templates -->
        <div style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
            <div class="tableHeadervs layout horizontal center center-justified">
                <div class="flex" style="width: 80px;">${msg.tagLbl}</div>
                <div class="flex" style="width:80px;">${msg.amountLbl}</div>
                <div>${msg.currencyLbl}</div>
                <div class="flex">${msg.lastUpdateLbl}</div>
            </div>
            <div>
                <template is="dom-repeat" items="{{balanceList.accounts}}">
                    <div class="rowvs" layout horizontal center center-justified>
                        <div class="flex" style="width: 80px;">{{tagDescription(item.tag.name)}}</div>
                        <div class="flex" style="width:80px;">{{formatAmount(item.amount)}}</div>
                        <div>{{item.currency}}</div>
                        <div class="flex">{{item.lastUpdated}}</div>
                    </div>
                </template>
            </div>
        </div>

    </template>
    <script>
        Polymer({
            is:'balance-list',
            properties: {
                url:{type:String, observer:'getHTTP'}
            },
            ready: function() {},
            tagDescription: function(tagName) {
                switch (tagName) {
                    case 'WILDTAG': return "${msg.wildTagLbl}".toUpperCase()
                    default: return tagName
                }
            },
            formatAmount: function(amount) {
                if(typeof amount == 'number') return amount.toFixed(2)
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.balanceList = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>