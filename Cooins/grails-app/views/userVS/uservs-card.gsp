<polymer-element name="uservs-card">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style no-shim>
        .card {
            position: relative;
            display: inline-block;
            width: 280px;
            vertical-align: top;
            background-color: #f9f9f9;
            box-shadow: 0 2px 2px 0 rgba(0, 0, 0, 0.24);
            -moz-border-radius: 3px; border-radius: 4px;
            border: 1px solid rgba(0, 0, 0, 0.24);
            margin: 5px;
            color: #667;
            cursor: pointer;
        }
        .nifColumn {text-decoration:underline;}
        .date {margin:3px 10px 0 0; color: #0000ff; font-size: 0.7em;}
        .userVSname {color: #888; margin: 3px 3px 5px 3px; white-space: nowrap; overflow: hidden;
            text-overflow: ellipsis; font-size: 0.8em;}
        .stateInfo {color:#621; text-transform: uppercase; text-align: right; margin: 0 0 0 5px; font-size: 0.8em;
            font-weight: bold;text-align: left;}
        </style>
        <div class="card" on-click="{{userSelected}}">
            <template if="{{isNifVisible}}">
                <div class="nifColumn">{{uservs.uservs.NIF}}</div>
            </template>
            <!--<div style="width:200px;">{{uservs.uservs.IBAN}}</div>-->
            <div flex horizontal layout center-justified class="userVSname">{{uservs.uservs.name}}</div>
            <div horizontal layout>
                <div flex class="stateInfo">{{uservs.state | userState}}</div>
                <div class="date">{{uservs.dateCreated}}</div>
            </div>
        </div>
    </template>
    <script>
        Polymer('uservs-card', {
            isNifVisible:false,
            publish: {
                uservs: {value: {}}
            },
            ready: function() { console.log(this.tagName + " - ready") },
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
            },
            userSelected: function(e) {
                console.log(this.tagName + " - userSelected - userId: " + e.target.templateInstance.model.uservs.uservs.id)
                this.fire('core-signal', {name: "uservs-selected", data: e.target.templateInstance.model.uservs.uservs.id});
            }
        });
    </script>
</polymer-element>
