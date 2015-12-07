<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="uservs-viewer">
    <template>
        <iron-ajax auto id="ajax" url="{{url}}" last-response="{{uservsDto}}" handle-as="json" content-type="application/json"></iron-ajax>
    </template>
    <script>
        Polymer({
            is:'uservs-viewer',
            properties: {
                groupvsViewer: {type:Object},
                uservsViewer: {type:Object},
                uservsDto: {type:Object, observer:'uservsDtoChanged'},
                url: {type:String}
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
            }
        });
    </script>
</dom-module>
