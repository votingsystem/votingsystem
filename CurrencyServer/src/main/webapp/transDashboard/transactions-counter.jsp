<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="transactions-counter">
    <template>
        <div style="border-bottom: 1px solid #888;color:#888;">
            <div style="">${msg.numMovementsLbl}</div>
            <div style="font-size: 0.6em; margin:0 0 0 0;">{{orderByLbl}}</div>
        </div>
        <template is="dom-repeat" items="[[transactionsMap.children]]" as="currencyNode">
            <div on-click="nodeClick" style$="[[_getCurrencyStyle(currencyNode.name)]]">
                <div class="horizontal layout" style="border-bottom: 1px dotted #888;color:#888;">
                    <div on-click="nodeClick" on-mouseover="_nodeHovered" style$="[[_getCurrencyStyle(currencyNode.name)]]">[[currencyNode.name]] - [[currencyNode.numTotalTransactions]] ${msg.movementsLbl}</div>
                </div>
                <table>
                    <template is="dom-repeat" items="[[currencyNode._children]]" as="secondLevelNode">
                        <tr>
                            <td style="width: 50px;text-align: right;">[[secondLevelNode.numTotalTransactions]]</td>
                            <td on-click="nodeClick" on-mouseover="_nodeHovered" style$="[[_getNodeStyle(secondLevelNode.name)]]">
                                [[_getsSecondLevelNodeName(secondLevelNode)]]
                            </td>
                        </tr>
                    </template>
                </table>
            </div>
        </template>
    </template>
    <script>
        (function () {
            Polymer({
                is:'transactions-counter',
                properties: {
                    transactionsMap: {type: Object},
                    orderByLbl: {type: String, value:"${msg.orderByType}"}
                },
                ready: function() { },
                _getCurrencyStyle: function(currencyCode) {
                    return "cursor:pointer;font-size: 0.9em;font-weight: bold;color:" + TransactionsStats.getColorScale(currencyCode) + ";"
                },
                nodeClick: function(e) {
                    var selectedNode = e.model.secondLevelNode ? e.model.secondLevelNode : e.model.currencyNode
                    this.fire("node-click", {node: selectedNode})
                },
                _nodeHovered: function(e) {
                    var selectedNode = e.model.secondLevelNode ? e.model.secondLevelNode : e.model.currencyNode
                    this.fire("node-hover", {node: selectedNode})
                },
                _getNodeStyle: function(nodeName) {
                    return "cursor:pointer;font-size: 0.9em;margin:0 0 0 7px;color:" + TransactionsStats.getColorScale(nodeName) + ";"
                },
                _getsSecondLevelNodeName: function(secondLevelNode) {
                    if(secondLevelNode.description) return secondLevelNode.description
                    else return secondLevelNode.name
                }
            });
        })()
    </script>
</dom-module>