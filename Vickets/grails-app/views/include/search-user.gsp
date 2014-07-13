<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-input', file: 'votingsystem-input.html')}">

<polymer-element name="search-user" attributes="url">
    <template>
        <style>
            .header {
                margin: 10px 0px 0px 0px;
                color:#6c0404;
                border-bottom: 2px solid #ccc;
                background: white;
                box-shadow: 0px 2px 2px rgba(0, 0, 0, 0.24);
                font-weight: bold;
            }
            .rowvs {
                border-bottom: 1px solid #ccc; padding: 10px; cursor: pointer;
            }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get" on-core-response="{{responseDataReceived}}"
                   contentType="json"></core-ajax>
        <div layout vertical center style="">
            <div style="display: {{responseData == null || responseData.userVSList.length == 0? 'none':'block'}};">
                <div layout horizontal id="uservs_table" class="header">
                    <div class="center" style="width: 300px;"><g:message code="nifLbl"/></div>
                    <div class="center" style="width:300px"><g:message code="nameLbl"/></div>
                </div>
                <div>
                    <template repeat="{{uservs in responseData.userVSList}}">
                        <div layout horizontal center center justified on-click="{{showUserDetails}}" class="rowvs">
                            <div class="center" style="width:300px">{{uservs.nif}}</div>
                            <div class="center" style="width:300px">{{uservs.name}}</div>
                        </div>
                    </template>
                </div>
            </div>

            <div class="center" id="emptySearchMsg" style="font-size: 1em; font-weight: bold;
                    display: {{responseData.userVSList.length == 0? 'block':'none'}};">
                <g:message code="emptyUserSearchResultMsg"/>
            </div>
        </div>
    </template>
    <script>
        Polymer('search-user', {
            ready: function() { this.url = this.url || '';},
                showUserDetails:function(e) {
                    this.fire('user-clicked', e.target.templateInstance.model.uservs);
                },
                reset: function() {
                    this.responseData = {userVSList:[]}
                    this.$.uservs_table.style.visibility = 'hidden'
                },
                responseDataReceived: function() {
                    console.log("search-user - responseDataReceived - num. users: " + this.responseData.userVSList.length)

                }

            });
    </script>
</polymer-element>