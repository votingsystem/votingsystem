<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="user-viewer">
    <template>
    </template>
    <script>
        Polymer({
            is:'user-viewer',
            properties: {
                url:{type:String, observer:'getHTTP'},
                groupViewer: {type:Object},
                userViewer: {type:Object},
                userDto: {type:Object, observer:'userDtoChanged'},
            },
            ready: function() { console.log(this.tagName + " - ready")},
            userDtoChanged: function() {
                console.log(this.tagName + " - TODO userDtoChanged - type: " + this.userDto.type)
                if(this.userDto.type === 'GROUP') {
                    Polymer.Base.importHref(vs.contextURL + '/group/group-details.vsp', function(e) {
                        if(!this.groupViewer) this.groupViewer = document.createElement('group-details');
                        this.groupViewer.group = this.userDto
                        vs.loadMainContent(this.groupViewer, "${msg.groupLbl}")
                    }.bind(this));
                } else {
                    Polymer.Base.importHref(vs.contextURL + '/user/user-data.vsp', function(e) {
                        if(!this.userViewer) this.userViewer = document.createElement('user-data');
                        this.userViewer.user = this.userDto
                        vs.loadMainContent(this.userViewer)
                    }.bind(this));
                }
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.userDto = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>
