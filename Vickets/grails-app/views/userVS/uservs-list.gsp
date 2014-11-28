<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-pager', file: 'vs-pager.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/uservs-card']"/>">

<polymer-element name="uservs-list" attributes="url menuType isNifVisible">
    <template>
        <core-signals on-core-signal-refresh-uservs-list="{{refreshUserList}}"></core-signals>
        <core-ajax id="ajax" auto url="{{url}}" response="{{userList}}" handleAs="json" method="get" contentType="json"></core-ajax>
        <div style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
            <div>
                <template repeat="{{uservs in userList.userVSList}}">
                    <uservs-card uservs="{{uservs}}"></uservs-card>
                </template>
                <vs-pager on-pager-change="{{pagerChange}}" max="{{userList.max}}"
                          next="<g:message code="nextLbl"/>" previous="<g:message code="previousLbl"/>"
                          first="<g:message code="firstLbl"/>" last="<g:message code="lastLbl"/>"
                          offset="{{userList.offset}}" total="{{userList.totalCount}}"></vs-pager>
            </div>
        </div>
    </template>
    <script>
        Polymer('uservs-list', {
            baseURL:null,
            ready: function() {console.log(this.tagName + " - ready") },
            refreshUserList: function(state) {
                this.$.ajax.go()
            },
            pagerChange: function(e) {
                var newURL = setURLParameter(this.$.ajax.url, "offset",  e.detail.offset)
                newURL = setURLParameter(newURL, "max", e.detail.max)
                console.log(this.tagName + " - pagerChange - newURL: " + newURL)
                this.$.ajax.url = newURL
            }
        });
    </script>
</polymer-element>