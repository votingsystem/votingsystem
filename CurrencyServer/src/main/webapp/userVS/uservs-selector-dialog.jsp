<%@ page contentType="text/html; charset=UTF-8" %>

<link href="./uservs-selector.vsp" rel="import"/>

<dom-module name="uservs-selector-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">{{msg.userSearchLbl}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div>
                    <uservs-selector id="userVSSelector" groupvs-id="{{groupId}}" contact-selector="false"></uservs-selector>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'uservs-selector-dialog',
            properties :{
                groupvsId:{type:Object}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                document.querySelector("#voting_system_page").addEventListener('user-clicked', function(e) {
                    this.$.modalDialog.style.opacity = 0
                    this.$.modalDialog.style['pointer-events'] = 'none'
                }.bind(this))
            },
            show: function(groupVSId) {
                console.log(this.tagName + " - show - groupVSId: " + groupVSId)
                this.$.userVSSelector.groupVSId = groupVSId
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            close: function() {
                this.$.userVSSelector.reset()
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            }
        });
    </script>
</dom-module>