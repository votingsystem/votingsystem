<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="transactions-counter">
    <template>
        <div style="text-decoration: underline;color:#888;">${msg.movementsLbl}</div>
        <template is="dom-repeat" items="[[transactionsMap.children]]" as="currencyNode">
            <div on-click="nodeClick" style="[[_getCurrencyStyle(currencyNode.name)]]">[[currencyNode.name]] - [[currencyNode.numTotalTransactions]]</div>
            <template is="dom-repeat" items="[[currencyNode._children]]" as="secondLevelNode">
                <div on-click="nodeClick" style="[[_getNodeStyle(secondLevelNode.name)]]">[[_getsSecondLevelNodeName(secondLevelNode)]] - [[secondLevelNode.numTotalTransactions]]</div>
            </template>
        </template>
    </template>
    <script>
        (function () {
            Polymer({
                is:'transactions-counter',
                properties: {
                    transactionsMap: {type: Object}
                },
                ready: function() { },
                _getCurrencyStyle: function(currencyCode) {
                    return "cursor:pointer;font-size: 0.9em;font-weight: bold;color:" + TransactionsStats.getColorScale(currencyCode) + ";"
                },
                nodeClick: function(e) {
                    var selectedNode = e.model.secondLevelNode ? e.model.secondLevelNode : e.model.currencyNode
                    this.fire("node-click", {node: selectedNode})
                },
                _getNodeStyle: function(nodeName) {
                    return "cursor:pointer;font-size: 0.9em;margin:0 0 0 7px;color:" + TransactionsStats.getColorScale(nodeName) + ";"
                },
                _getsSecondLevelNodeName: function(secondLevelNode) {
                    if(secondLevelNode.description) return secondLevelNode.description
                    else return secondLevelNode.name
                },
                load:function(transactionsMap) {
                    this.transactionsMap = transactionsMap
                }
            });
        })()
    </script>
</dom-module>