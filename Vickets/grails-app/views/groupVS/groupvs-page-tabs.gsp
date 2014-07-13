<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/include/balance-list.gsp']"/>">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/include/user-list.gsp']"/>">


<polymer-element name="group-page-tabs">
    <template>
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
        }
        paper-tabs.transparent-teal::shadow #selectionBar { background-color: #ba0011; }
        paper-tabs.transparent-teal paper-tab::shadow #ink { color: #ba0011; }
        </style>
        <div  style="width: 1000px; margin:0px auto 0px auto;">
            <paper-tabs  style="width: 1000px;margin:0px auto 0px auto;" class="transparent-teal center" valueattr="name"
                         selected="{{selectedTab}}"  on-core-select="{{tabSelected}}" noink>
                <paper-tab name="balanceList" style="width: 400px"><g:message code="balanceListLbl"/></paper-tab>
                <paper-tab name="transactionsTo"><g:message code="incomeLbl"/></paper-tab>
                <paper-tab name="transactionsFrom"><g:message code="expensesLbl"/></paper-tab>
                <paper-tab name="userList"><g:message code="usersLbl"/></paper-tab>
            </paper-tabs>
            <div id="balanceList" class="tabContent" style="display:{{selectedTab == 'balanceList'?'block':'none'}}">
                <balance-list url="${createLink(controller:'userVSAccount', action:'balance')}?id=${groupvsMap.id}"></balance-list>
            </div>
            <div id="transactionsTo" class="tabContent" style="display:{{selectedTab == 'transactionsTo'?'block':'none'}}">
                <div id="transactionTo_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
                    <table class="table white_headers_table" id="transactionTo_table" style="">
                        <thead>
                        <tr style="color: #ff0000;">
                            <th style="width: 290px;"><g:message code="typeLbl"/></th>
                            <th style="max-width:80px;"><g:message code="amountLbl"/></th>
                            <th style="width:180px;"><g:message code="dateLbl"/></th>
                            <th style="min-width:300px;"><g:message code="subjectLbl"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${groupvsMap?.transactionToList}">
                            <g:set var="transactionURL" value="${createLink(uri:'/transaction', absolute:true)}/${it.id}" scope="page" />
                            <% def transactionToDate = formatDate(date:it.dateCreated, formatName:'webViewDateFormat')%>
                            <tr>
                                <td class="text-center">${transactionVSService.getTransactionTypeDescription(it.type, request.locale)}</td>
                                <td class="text-right">${it.amount} ${it.currency}</td>
                                <td class="text-center">${transactionToDate}</td>
                                <td class="text-center">
                                    <a href="#" onclick="openWindow('${transactionURL}')">${it.subject}</a>
                                </td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
            </div>
            <div id="transactionsFrom" class="tabContent" style="display:{{selectedTab == 'transactionsFrom'?'block':'none'}}">
                <div id="transactionFrom_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
                    <table class="table white_headers_table" id="transactionFrom_table" style="">
                        <thead>
                        <tr style="color: #ff0000;">
                            <th style="width: 290px;"><g:message code="typeLbl"/></th>
                            <th style="width:80px;"><g:message code="amountLbl"/></th>
                            <th style="width:180px;"><g:message code="dateLbl"/></th>
                            <th style="min-width:300px;"><g:message code="subjectLbl"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${groupvsMap?.transactionFromList}">
                            <g:set var="transactionURL" value="${createLink(uri:'/transaction', absolute:true)}/${it.id}" scope="page" />
                            <% def transactionDate = formatDate(date:it.dateCreated, formatName:'webViewDateFormat')%>
                            <tr>
                                <td class="text-center">${transactionVSService.getTransactionTypeDescription(it.type, request.locale)}</td>
                                <td class="text-right">${it.amount} ${it.currency}</td>
                                <td class="text-center">${transactionDate}</td>
                                <td class="text-center">
                                    <a href="#" onclick="openWindow('${transactionURL}')">${it.subject}</a>
                                </td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
            </div>
            <div id="userList" class="tabContent" style="display:{{selectedTab == 'userList'?'block':'none'}}">
                <user-list url="${createLink(controller: 'groupVS', action: 'listUsers')}/${groupvsMap?.id}"
                           userURLPrefix="${createLink(controller: 'groupVS')}/${groupvsMap?.id}/user"
                           menuType="${params.menu}"></user-list>
            </div>
        </div>
    </template>


    <script>
        Polymer('group-page-tabs', {
            selectedTab:'balanceList'
        });
    </script>
</polymer-element>