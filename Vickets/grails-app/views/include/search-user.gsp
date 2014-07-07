<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="search-user" attributes="url">
    <template>
        <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get" on-core-response="{{responseDataReceived}}"
                   contentType="json"></core-ajax>
        <div layout vertical center style="max-width: 800px; overflow:auto;">
            <table class="table white_headers_table" id="uservs_table" style="">
                <thead>
                <tr style="color: #ff0000;">
                    <th style="width: 60px;"><g:message code="nifLbl"/></th>
                    <th style=""><g:message code="nameLbl"/></th>
                </tr>
                </thead>
                <tbody>
                <template repeat="{{uservs in responseData.userVSList}}">
                    <tr on-click="{{showUserDetails}}">
                        <td class="text-center">{{uservs.nif}}</td>
                        <td class="text-center">{{uservs.name}}</td>
                    </tr>
                </template>
                </tbody>
            </table>
        </div>
    </template>
    <script>
        Polymer('search-user', {
            ready: function() { this.url = this.url || '';},
                showUserDetails:function(e) {
                    this.fire('user-clicked', e.target.templateInstance.model.uservs);
                },
                emptyTable: function() {
                    var tableRows = this.$.uservs_table.getElementsByTagName('tbody');
                    var numRows = tableRows.length;
                    while(numRows) this.$.uservs_table.removeChild(tableRows[--numRows]);
                    this.$.uservs_table.style.visibility = 'hidden'
                },
                responseDataReceived: function() {
                    console.log("search-user - responseDataReceived - num. users: " + this.responseData.userVSList.length)
                }

            });
    </script>
</polymer-element>