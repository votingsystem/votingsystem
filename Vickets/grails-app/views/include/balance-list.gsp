<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="balance-list" attributes="url">
    <template>
        <style></style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{balanceList}}" handleAs="json" method="get"
                   contentType="json"></core-ajax>
        <div layout vertical center style="max-width: 1200px; overflow:auto;">
            <table class="table white_headers_table" id="balance_table" style="">
                <thead>
                    <tr style="color: #ff0000;">
                        <th style="max-width:80px;"><g:message code="tagLbl"/></th>
                        <th style="max-width:80px;"><g:message code="amountLbl"/></th>
                        <th style="width: 120px;"><g:message code="currencyLbl"/></th>
                        <th style="width:200px;"><g:message code="lastUpdateLbl"/></th>
                    </tr>
                </thead>
                <tbody>
                    <template repeat="{{account in balanceList.accounts}}">
                        <tr>
                            <td class="text-center">{{account.tag.name}}</td>
                            <td class="text-center">{{account.amount | formatAmount}}</td>
                            <td class="text-center">{{account.currency}}</td>
                            <td class="text-center">{{account.lastUpdated}}</td>
                        </tr>
                    </template>
                </tbody>
            </table>
        </div>
    </template>
    <script>
        Polymer('balance-list', {
            ready: function() {},
            formatAmount: function(amount) {
                return amount.toFixed(2)
            }
        });
    </script>
</polymer-element>