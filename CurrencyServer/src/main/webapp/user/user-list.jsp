<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-pager/vs-pager.html" rel="import"/>
<link href="./user-subscription-card.vsp" rel="import"/>

<dom-module name="user-list">
    <template>
        <div style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{userListDto.resultList}}" as="subscription">
                    <user-subscription-card subscription="{{subscription}}"></user-subscription-card>
                </template>
            </div>
            <vs-pager on-pager-change="pagerChange" max="{{userListDto.max}}"
                      next="${msg.nextLbl}" previous="${msg.previousLbl}"
                      first="${msg.firstLbl}" last="${msg.lastLbl}"
                      offset="{{userListDto.offset}}" total="{{userListDto.totalCount}}"></vs-pager>
        </div>
    </template>
    <script>
        Polymer({
            is:'user-list',
            properties: {
                url:{type:String, observer:'getHTTP'},
                userListDto:{type:Object, observer:'userListDtoChanged'},
                groupId:{type:Number}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                document.querySelector("#voting_system_page").addEventListener('refresh-user-list',
                        function() {  this.refreshList() }.bind(this))
            },
            loadGroupUsers:function(groupId) {
                this.groupId = groupId
                console.log(this.tagName + " - loadGroupUsers - groupId: " + this.groupId)
                this.url = vs.contextURL + "/rest/group/id/" + this.groupId + "/listUsers"
            },
            refreshList: function() {
                getHTTP(this.url)
            },
            urlChanged:function() {
                console.log(this.tagName + " - urlChanged: " + this.url)
            },
            userListDtoChanged:function() {
                console.log(this.tagName + " - ready - userListDto.size: " + this.userListDto.resultList.length)
            },
            pagerChange: function(e) {
                var newURL = setURLParameter(this.url, "offset",  e.detail.offset)
                this.url = setURLParameter(newURL, "max", e.detail.max)
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.userListDto = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>