<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="search-user" attributes="url">
    <template>
        <core-ajax id="ajax" auto url="{{url}}" response="{{userList}}" handleAs="json" method="get"
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
                <template repeat="{{uservs in userList.userVSList}}">
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
                    this.fire('user-clicked', {userId: e.target.templateInstance.model.uservs.id});
                }
            });
    </script>
</polymer-element>