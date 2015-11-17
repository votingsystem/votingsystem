<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="groupvs-card">
    <link rel="import" type="css" href="groupvs-card.css">
    <template>
        <div id="content" on-tap="_showGroupDetails" class$="{{_groupvsClass(groupvs.state)}}">
                <div id="el" on-tap="nav">
                    <div class='groupvsSubject'>[[groupvs.name]]</div>
                </div>
                <div style="font-size: 0.7em; color: #606060; font-style: italic; margin: 0 0 0 10px;">
                    {{groupvs.representative.firstName}} {{groupvs.representative.lastName}}
                </div>
                <div hidden="{{!_isItemCanceled(groupvs)}}" style="position: relative;">
                    <div class='groupvsMessageCancelled'>${msg.groupvsCancelledLbl}</div>
                </div>

                <div class='numTotalUsersDiv text-right'><span>{{groupvs.numActiveUsers}}</span> ${msg.usersLbl}</div>

                <div id="el-tags" class="horizontal layout center meta">
                    <i class="fa fa-tag"></i>
                    <span class="horizontal layout wrap flex">
                        <template is="dom-repeat" items="[[groupvs.tags]]" as="tag">
                            <div on-tap="_tagClicked" style="cursor: pointer;color: #606060;text-decoration: underline;">[[tag.name]]</div>
                        </template>
                      </span>
                </div>
        </div>
    </template>
<script>
    Polymer({
        is:'groupvs-card',
        properties: {
            groupvs:{type:Object, observer:'_groupvsChanged'},
            caption:{type:String}
        },
        ready: function() {
            this.opened = false
        },
        _groupvsChanged:function() {
        },
        _isItemCanceled:function(item) {
            return item.state === 'CANCELED'
        },
        _tagClicked: function(e) {
            console.log(this.tagName + "._tagClicked: " + e.currentTarget.name)
        },
        _showGroupDetails :  function() {
            console.log(this.tagName + " - showGroupDetails")
            app.groupvs = this.groupvs;
            page(contextURL + "/rest/groupVS/id/" + app.groupvs.id)
        },
        _groupvsClass:function(state) {
            switch (state) {
                case 'ACTIVE': return "groupvsDiv groupvs groupvsActive"
                case 'PENDING': return "groupvsDiv groupvs groupvsPending"
                case 'CANCELED': return "groupvsDiv groupvs groupvsFinished"
            }
        }
    });
</script>
</dom-module>
