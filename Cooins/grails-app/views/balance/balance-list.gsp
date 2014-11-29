<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="balance-list" attributes="url">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .tableHeadervs {
                margin: 0px 0px 0px 0px;
                color:#6c0404;
                background: white;
                border-bottom: 1px solid #6c0404;
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
            .rowvs div {
                text-align:center;
            }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{balanceList}}" handleAs="json" method="get" contentType="json"></core-ajax>
        <!--JavaFX Webkit gives problems with tables and templates -->
        <div style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
            <div layout horizontal center center-justified class="tableHeadervs">
                <div flex style="width: 80px;"><g:message code="tagLbl"/></div>
                <div flex style="width:80px;"><g:message code="amountLbl"/></div>
                <div style=""><g:message code="currencyLbl"/></div>
                <div flex style=""><g:message code="lastUpdateLbl"/></div>
            </div>
            <div>
                <template repeat="{{account in balanceList.accounts}}">
                    <div class="rowvs" layout horizontal center center-justified>
                        <div flex style="width: 80px;">{{account.tag.name | tagDescription}}</div>
                        <div flex style="width:80px;">{{account.amount | formatAmount}}</div>
                        <div style="">{{account.currency}}</div>
                        <div flex style="">{{account.lastUpdated}}</div>
                    </div>
                </template>
            </div>
        </div>

    </template>
    <script>
        Polymer('balance-list', {
            ready: function() {},
            tagDescription: function(tagName) {
                switch (tagName) {
                    case 'WILDTAG': return "<g:message code="wildTagLbl"/>".toUpperCase()
                    default: return tagName
                }
            },
            formatAmount: function(amount) {
                if(typeof amount == 'number') return amount.toFixed(2)
            }
        });
    </script>
</polymer-element>