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
        .tableHeadervs {
            margin: 0px 0px 0px 0px;
            color:#6c0404;
            border-bottom: 1px solid #6c0404;
            background: white;
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
            cursor: pointer;
            width: 100%;
        }

        .rowvs div {
            text-align:center;
        }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{userList}}" handleAs="json" method="get"
                   contentType="json"></core-ajax>
        <!--JavaFX Webkit gives problems with tables and templates -->
        <div style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
            <div layout horizontal center class="tableHeadervs">
                <div style="width: 110px;"><g:message code="nifLbl"/></div>
                <!--<div style="width:200px;">IBAN</div>-->
                <div flex style="width:150px;"><g:message code="nameLbl"/></div>
                <div style="width:80px;"><g:message code="stateLbl"/></div>
                <div style="width:170px;"><g:message code="lastUpdateLbl"/></div>
            </div>
            <div>
                <template repeat="{{uservs in userList.userVSList}}">
                    <div class="rowvs" layout horizontal center center justified>
                        <div class="nifColumn" style="width: 110px;">
                            <a on-click="{{userSelected}}"> {{uservs.uservs.NIF}}</a>
                        </div>
                        <!--<div style="width:200px;">{{uservs.uservs.IBAN}}</div>-->
                        <div flex style="width:150px;">{{uservs.uservs.name}}</div>
                        <div style="width:80px;">{{uservs.state | userState}}</div>
                        <div style="width:170px;">{{uservs.lastUpdated}}</div>
                    </div>
                </template>
            </div>
        </div>
    </template>
    <script>
        Polymer('user-list', {
            ready: function() {console.log(this.tagName + " - ready") },
            userSelected: function(e) {
                console.log(this.tagName + " - userSelected -userId: " + e.target.templateInstance.model.uservs.uservs.id)
                this.fire('core-signal', {name: "uservs-selected", data: e.target.templateInstance.model.uservs.uservs.id});
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