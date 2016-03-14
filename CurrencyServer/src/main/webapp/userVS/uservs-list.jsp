<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-pager/vs-pager.html" rel="import"/>
<link href="./uservs-subscription-card.vsp" rel="import"/>

<dom-module name="uservs-list">
    <template>
        <div style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{userListDto.resultList}}" as="subscription">
                    <uservs-subscription-card subscription="{{subscription}}"></uservs-subscription-card>
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
            is:'uservs-list',
            properties: {
                url:{type:String, observer:'getHTTP'},
                userListDto:{type:Object, observer:'userListDtoChanged'},
                groupvsId:{type:Number}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                document.querySelector("#voting_system_page").addEventListener('refresh-uservs-list',
                        function() {  this.refreshList() }.bind(this))
            },
            loadGroupUsers:function(groupvsId) {
                this.groupvsId = groupvsId
                console.log(this.tagName + " - loadGroupUsers - groupvsId: " + this.groupvsId)
                this.url = vs.contextURL + "/rest/groupVS/id/" + this.groupvsId + "/listUsers"
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