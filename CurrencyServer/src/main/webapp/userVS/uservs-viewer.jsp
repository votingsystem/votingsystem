<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="uservs-viewer">
    <template>
    </template>
    <script>
        Polymer({
            is:'uservs-viewer',
            properties: {
                url:{type:String, observer:'getHTTP'},
                groupvsViewer: {type:Object},
                uservsViewer: {type:Object},
                uservsDto: {type:Object, observer:'uservsDtoChanged'},
            },
            ready: function() { console.log(this.tagName + " - ready")},
            uservsDtoChanged: function() {
                console.log(this.tagName + " - TODO uservsDtoChanged - type: " + this.uservsDto.type)
                if(this.uservsDto.type === 'GROUP') {
                    Polymer.Base.importHref(contextURL + '/groupVS/groupvs-details.vsp', function(e) {
                        if(!groupvsViewer) groupvsViewer = document.createElement('groupvs-details');
                        groupvsViewer.groupvs = this.uservsDto
                        vs.loadMainContent(groupvsViewer, "${msg.groupVSLbl}")
                    });
                } else {
                    Polymer.Base.importHref(contextURL + '/userVS/uservs-data.vsp', function(e) {
                        if(!uservsViewer) uservsViewer = document.createElement('uservs-data');
                        uservsViewer.uservs = this.uservsDto
                        vs.loadMainContent(uservsViewer)
                    });
                }
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.uservsDto = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>
