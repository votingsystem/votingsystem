<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="group-card">
    <style>
        :host {
            display: block;
            max-width: 350px;
            margin: 10px;
            cursor: pointer;
            transition: box-shadow 0.1s;
        }
        :host(:hover) {
            @apply(--shadow-elevation-2dp);
        }
        #content {
            background: white;
            border-radius: 3px;
            border: 1px solid #e5e5e5;
        }
        .meta {
            @apply(--paper-font-caption);
            border-top: 1px solid;
            border-bottom: 1px solid;
            border-color: var(--divider-color);
            padding: 10px 16px;
        }
        .meta + .meta {
            border-top: 0;
        }
        .meta:last-child {
            border-bottom: 0;
        }
        .groupMessageCancelled {
            position:absolute;
            opacity:0.4;
            left:20%;
            top:-10px;
            font-size: 1.2em;
            font-weight: bold;
            color:#91140a;
            text-transform:uppercase;
            transform:rotate(17deg);
            -ms-transform:rotate(17deg);
            -webkit-transform:rotate(17deg);
            -moz-transform: rotate(17deg);
        }
        .groupSubject {
            white-space:nowrap;
            overflow:hidden;
            text-overflow: ellipsis;
            font-weight:bold;
            color: #f2f2f2;
            text-align: center;
            position: relative;
            padding: 5px 10px 5px 10px;
            margin: 0 auto 0 auto;
            text-decoration: underline;
        }
        .numTotalUsersDiv {
            bottom: 0px;
            padding: 3px 0 0 10px;
            margin: 0px 20px 0px 0px;
            text-transform:uppercase;
            font-size: 0.8em;
        }
        .groupActive .groupSubject { color:#388746; }
        .groupActive .numTotalUsersDiv { color:#388746; }
        .groupPending .groupSubject{ color:#fba131; }
        .groupPending .numTotalUsersDiv { color:#fba131; }
        .groupFinished .groupSubject{ color:#cc1606; }
        .groupFinished .numTotalUsersDiv { color:#cc1606; }
    </style>
    <template>
        <div id="content" on-tap="_showGroupDetails" class$="{{_groupClass(group.state)}}">
                <div id="el" on-tap="nav">
                    <div class='groupSubject'>[[group.name]]</div>
                </div>
                <div style="font-size: 0.7em; color: #606060; font-style: italic; margin: 0 0 0 10px;">
                    {{group.representative.firstName}} {{group.representative.lastName}}
                </div>
                <div hidden="{{!_isItemCanceled(group)}}" style="position: relative;">
                    <div class='groupMessageCancelled'>${msg.groupCancelledLbl}</div>
                </div>

                <div class='numTotalUsersDiv text-right'><span>{{group.numActiveUsers}}</span> ${msg.usersLbl}</div>

                <div id="tagsDiv" class="horizontal layout center meta" style="font-size: 0.8em; padding: 3px 5px; color: #888;">
                    <i class="fa fa-tag"></i>
                    <span class="horizontal layout wrap flex">
                        <template is="dom-repeat" items="[[group.tags]]" as="tag">
                            <div style="color: #606060;text-decoration: underline;">[[tag.name]]</div>
                        </template>
                      </span>
                </div>
        </div>
    </template>
<script>
    Polymer({
        is:'group-card',
        properties: {
            group:{type:Object, observer:'_groupChanged'},
            caption:{type:String}
        },
        ready: function() { },
        _groupChanged:function() {
            if(this.group.tags.length === 0) this.$.tagsDiv.style.display = 'none'
        },
        _isItemCanceled:function(item) {
            return item.state === 'CANCELED'
        },
        _showGroupDetails :  function() {
            console.log(this.tagName + " - showGroupDetails")
            vs.group = this.group;
            page(vs.contextURL + "/rest/group/id/" + vs.group.id)
        },
        _groupClass:function(state) {
            switch (state) {
                case 'ACTIVE': return "groupDiv group groupActive"
                case 'PENDING': return "groupDiv group groupPending"
                case 'CANCELED': return "groupDiv group groupFinished"
            }
        }
    });
</script>
</dom-module>
