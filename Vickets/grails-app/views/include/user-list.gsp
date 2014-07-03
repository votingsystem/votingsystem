<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="user-list" attributes="url userURLPrefix menuType">
  <template>
      <style>
          .nifColumn {
              cursor: pointer;
              color: #0000ff;
              text-decoration:underline;
          }
      </style>
      <core-ajax id="ajax" auto url="{{url}}" response="{{userListJSON}}" handleAs="json" method="get"
                 contentType="json"></core-ajax>
      <div layout vertical center>
          <table class="table white_headers_table" id="uservs_table" style="">
              <thead>
              <tr style="color: #ff0000;">
                  <th data-dynatable-column="nif" style="width: 120px;"><g:message code="nifLbl"/></th>
                  <th data-dynatable-column="name" style="max-width:80px;">IBAN</th>
                  <th data-dynatable-column="name" style="max-width:80px;"><g:message code="nameLbl"/></th>
                  <th data-dynatable-column="state" style="max-width:60px;"><g:message code="stateLbl"/></th>
                  <th data-dynatable-column="lastUpdate" style="width:200px;"><g:message code="lastUpdateLbl"/></th>
              </tr>
              </thead>
              <tbody>
                  <template repeat="{{userJSON in userListJSON.userVSList}}">
                      <tr><td class="text-center"><div data-userId="{{userJSON.uservs.id}}" on-click="{{openWindow}}"
                              class="nifColumn">{{userJSON.uservs.NIF}}</div></td>
                          <td class="text-center">{{userJSON.uservs.IBAN}}</td>
                          <td class="text-center">{{userJSON.uservs.name}}</td>
                          <td class="text-center">{{userJSON.state | userState}}</td>
                          <td class="text-center">{{userJSON.lastUpdated}}</td></tr>
                  </template>
              </tbody>
          </table>
      </div>
  </template>
  <script>
    Polymer('user-list', {
        ready: function() { },
        openWindow: function(e) {
            var userURL = this.userURLPrefix + "/" + e.target.getAttribute("data-userId") + "?mode=details&menu=" + this.menuType
            openWindow(userURL)
        },
        userState: function(state) {
            var userState
            switch(state) {
                case 'ACTIVE':
                    userState = '<g:message code="activeUserLbl"/>'
                    break;
                case 'PENDING':
                    userState = '<g:message code="pendingUserLbl"/>'
                    break;
                case 'CANCELLED':
                    userState = '<g:message code="cancelledUserLbl"/>'
                    break;
                default:
                    userState = jsonSubscriptionData.state
            }
            return userState
        }
    });
  </script>
</polymer-element>