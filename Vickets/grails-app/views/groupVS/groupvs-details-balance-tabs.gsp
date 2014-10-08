<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/balance/balance-list']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/transactionVS/transactionvs-table']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/uservs-list']"/>">
<link rel="import" href="${resource(dir: '/bower_components/paper-tabs', file: 'paper-tabs.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="groupvs-details-balance-tabs" attributes="groupvs">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style shim-shadowdom>
            .tabContent {
                padding: 10px 20px 10px 20px;
                margin:0px auto 0px auto;
                width:auto;
            }
            paper-tabs.transparent-teal {
                background-color: transparent;
                color:#ba0011;
                box-shadow: none;
                cursor: pointer;
            }
            paper-tabs.transparent-teal::shadow #selectionBar { background-color: #ba0011; }
            paper-tabs.transparent-teal paper-tab::shadow #ink { color: #ba0011; }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{groupvsWithDetails}}" handleAs="json" method="get"
                   contentType="json"></core-ajax>
        <div  style="margin:0px auto 0px auto;">
            <paper-tabs  style="margin:0px auto 0px auto; border-bottom: 1px solid #ba0011;" class="transparent-teal center" valueattr="name"
                         selected="{{selectedTab}}"  on-core-select="{{tabSelected}}" noink>
                <paper-tab name="balanceList" style="width:400px; padding:2px;"><g:message code="balanceListLbl"/></paper-tab>
                <paper-tab name="transactionsTo" style="padding:2px;"><g:message code="incomesLbl"/></paper-tab>
                <paper-tab name="transactionsFrom" style="padding:2px;"><g:message code="expensesLbl"/></paper-tab>
                <paper-tab name="userList" style="padding:2px;"><g:message code="usersLbl"/></paper-tab>
            </paper-tabs>
            <div id="balanceList" class="tabContent" style="display:{{selectedTab == 'balanceList'?'block':'none'}}">
                <balance-list id="balanceList"></balance-list>
            </div>
            <div id="transactionsTo" class="tabContent" style="display:{{selectedTab == 'transactionsTo'?'block':'none'}}">
                <transactionvs-table id="transactionToTable"></transactionvs-table>
            </div>
            <div id="transactionsFrom" class="tabContent" style="display:{{selectedTab == 'transactionsFrom'?'block':'none'}}">
                <transactionvs-table id="transactionFromTable" isUserVSTable="true"></transactionvs-table>
            </div>
            <div id="userList" class="tabContent" style="display:{{selectedTab == 'userList'?'block':'none'}}">
                <uservs-list id="userList" menuType="${params.menu}"></uservs-list>
            </div>
        </div>
    </template>


    <script>
        Polymer('groupvs-details-balance-tabs', {
            selectedTab:'balanceList',
            groupvs: null,
            ready:function() {
                console.log(this.tagName + " - ready")
            },
            groupvsChanged:function() {
                console.log(this.tagName + " - groupvsChanged")
                if(this.groupvs != null && this.groupvs.userVS != null) {
                    if(this.groupvs.transactionFromList == null) {
                        this.$.ajax.url = "${createLink(controller: 'groupVS')}/" + this.groupvs.userVS.id
                    }
                    this.$.transactionFromTable.transactionList = this.groupvs.transactionFromList
                    this.$.transactionToTable.transactionList = this.groupvs.transactionToList
                    this.$.balanceList.url = "${createLink(controller:'userVSAccount', action:'balance')}?id=" + this.groupvs.userVS.id
                    this.$.userList.userURLPrefix = "${createLink(controller: 'groupVS')}/" + this.groupvs.userVS.id + "/user"
                    this.$.userList.url = "${createLink(controller: 'groupVS', action: 'listUsers')}/" + this.groupvs.userVS.id
                }
            },
            groupvsWithDetailsChanged:function() {
                this.$.transactionFromTable.transactionList = this.groupvsWithDetails.transactionFromList
                this.$.transactionToTable.transactionList = this.groupvsWithDetails.transactionToList
            }
        });
    </script>
</polymer-element>