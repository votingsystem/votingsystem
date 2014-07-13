<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="balance-list" attributes="url">
    <template>
        <style> </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{balanceList}}" handleAs="json" method="get"
                   contentType="json"></core-ajax>
        <!--JavaFX Webkit gives problems with tables and templates -->
        <div style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
            <div layout horizontal center center-justified class="tableHeadervs">
                <div flex style="width: 80px;"><g:message code="tagLbl"/></div>
                <div flex style="width:80px;"><g:message code="amountLbl"/></div>
                <div flex style="width:120px;"><g:message code="currencyLbl"/></div>
                <div flex style="width:200px;"><g:message code="lastUpdateLbl"/></div>
            </div>
            <div>
                <template repeat="{{account in balanceList.accounts}}">
                    <div layout horizontal center center-justified class="rowvs">
                        <div flex style="width: 80px;">{{account.tag.name}}</div>
                        <div flex style="width:80px;">{{account.amount | formatAmount}}</div>
                        <div flex style="width:120px;">{{account.currency}}</div>
                        <div flex style="width:200px;">{{account.lastUpdated}}</div>
                    </div>
                </template>
            </div>
        </div>

    </template>
    <script>
        Polymer('balance-list', {
            ready: function() {},
            formatAmount: function(amount) {
                if(amount) return amount.toFixed(2)
            }
        });
    </script>
</polymer-element>