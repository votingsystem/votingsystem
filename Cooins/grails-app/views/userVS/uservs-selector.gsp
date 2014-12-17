<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="core-ajax" file="core-ajax.html"/>
<vs:webresource dir="paper-input" file="paper-input.html"/>
<vs:webresource dir="paper-button" file="paper-button.html"/>
<vs:webresource dir="core-localstorage" file="core-localstorage.html"/>

<polymer-element name="uservs-selector" attributes="url contactSelector">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .card {
                position: relative; display: inline-block; width: 280px; vertical-align: top;
                background-color: #f9f9f9; box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
                border: 1px solid rgba(0, 0, 0, 0.24); margin: 10px; color: #667; cursor: pointer; padding: 5px;
            }
            .nif {font-size: 0.7em; color:#888;margin:5px 0 0 0; }
            .name {color: #000; font-size: 0.9em;}
        </style>
        <core-ajax id="ajax" url="{{url}}" response="{{responseData}}" handleAs="json" method="get" on-core-response="{{responseDataReceived}}"
                   contentType="json"></core-ajax>
        <div vertical layout center>
            <template if="{{modeSearch}}">
                <div horizontal layout>
                    <div layout horizontal center center-justified style="margin:0px auto 0px auto;width: 100%;">
                        <input value="{{textToSearch}}" type="text" style="width:200px;" class="form-control"
                               placeholder="<g:message code="userSearchLbl"/>" on-keypress="{{searchInputKeyPress}}">
                        <paper-button raised on-click="{{processSearch}}" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-search"></i> <g:message code="userSearchLbl"/>
                        </paper-button>
                    </div>
                    <div layout horizontal center center-justified style="width: 200px;">
                        <paper-button raised on-click="{{toggleContactsView}}" style="margin: 0px 10px 0px 5px;" data-button="contacts">
                            <i class="fa fa-users"></i> <g:message code="contactsLbl"/>
                        </paper-button>
                    </div>
                </div>
            </template>
            <template if="{{!modeSearch}}">
                <div horizontal layout center center-justified style="width: 100%;">
                    <div flex style="font-size: 1.5em; font-weight: bold; color: #6c0404; text-align: center;"><g:message code="contactsLbl"/></div>
                    <div>
                        <paper-button raised on-click="{{toggleContactsView}}" style="font-size: 0.8em; margin:10px 0px 10px 10px;" data-button="userSearch">
                            <i class="fa fa-search"></i> <g:message code="searchLbl"/>
                        </paper-button>
                    </div>
                </div>
            </template>
            <div style="display: {{userVSList.length == 0? 'none':'block'}};">
                <div layout flex horizontal wrap around-justified>
                    <template repeat="{{uservs in userVSList}}">
                        <div horizontal layout  class="card" on-click="{{showUserDetails}}">
                            <div layout flex vertical center-justified>
                                <div class="name">{{uservs.name}}</div>
                                <div class="name" style="margin: 5px 0 0 0;">{{uservs.lastName}}</div>
                            </div>
                            <div>
                                <div style="display:{{uservs|isContactButtonVisible}}">
                                    <paper-button raised on-click="{{toggleContact}}" style="font-size: 0.7em; margin:0 0 0 0;">
                                        {{modeSearch? '<g:message code="addContactLbl"/>':'<g:message code="removeContactLbl"/>'}}
                                    </paper-button>
                                </div>
                                <div flex horizontal layout center center-justified class="nif">{{uservs.nif}}</div>
                            </div>
                        </div>
                    </template>
                </div>
            </div>
            <div class="center" id="emptySearchMsg" style="font-size: 1em; font-weight: bold;
                    display: {{responseData.userVSList.length == 0? 'block':'none'}};">
                <g:message code="emptyUserSearchResultMsg"/>
            </div>
            <core-localstorage id="localstorage" name="contacts-localstorage" value="{{contacts}}"
                   on-load="{{contactsChanged}}"></core-localstorage>
        </div>
    </template>
    <script>
        Polymer('uservs-selector', {
            contactsArray:[],
            modeSearch:false,
            ready: function() {
                this.url = this.url || '';
                this.userVSList = []
                this.contactSelector = (this.contactSelector != null)?true:false
                this.modeSearch = !this.contactSelector
                console.log(this.tagName + " - contactSelector: " + this.contactSelector)
            },
            modeSearchChanged:function(e) {
                console.log(this.tagName + " modeSearchChanged - modeSearch: " + this.modeSearch)
                if(this.modeSearch === true) {
                    this.textToSearch = ""
                    this.userVSList = []
                } else this.userVSList = toJSON(this.contacts)
            },
            contactsChanged:function(e) {
                console.log(this.tagName + " - contactsChanged")
                this.contactsArray = toJSON(this.contacts)
                if(this.contactSelector && !this.modeSearch) this.userVSList = this.contactsArray
            },
            showUserDetails:function(e) {
                this.fire("user-clicked", e.target.templateInstance.model.uservs)
                this.fire('core-signal', {name: "user-clicked", data: e.target.templateInstance.model.uservs});
            },
            reset: function() {
                this.userVSList = []
            },
            responseDataReceived: function() {
                console.log(this.tagName + " - responseDataReceived - num. users: " + this.responseData.userVSList.length)
                this.userVSList = this.responseData.userVSList
            },
            contactsArrayChanged: function(e) {
                this.contacts = JSON.stringify(this.contactsArray)
            },
            toggleContact: function(event, detail, target) {
                var uservs = target.templateInstance.model.uservs
                if(this.modeSearch == true) {
                    console.log("toggleContact - addContact")
                    if(!this.isContact(uservs)) this.contactsArray.push(uservs)
                    target.style.display = 'none'
                } else {
                    console.log("toggleContact - removeContact")
                    var result = []
                    for(uservsIdx in this.contactsArray) {
                        if(this.contactsArray[uservsIdx].id !== uservs.id) result.push(this.contactsArray[uservsIdx])
                    }
                    this.contacts = JSON.stringify(result)
                }
                event.stopPropagation()
            },
            isContact:function (uservs){
                var isInArray = false
                for(uservsIdx in this.contactsArray) {
                    if(this.contactsArray[uservsIdx].id === uservs.id) return true
                }
                return isInArray
            },
            isContactButtonVisible:function (uservs){
                if(this.modeSearch && this.contactSelector) {
                    if(this.isContact(uservs)) return "none"
                    else return "block"
                } else if(this.contactSelector) return "block"
                else return "none"
            },
            searchInputKeyPress:function (e){
                var chCode = ('charCode' in e) ? e.charCode : e.keyCode;
                if (chCode == 13) this.processSearch()
            },
            processSearch:function() {
                if(this.textToSearch.trim() === "") return
                this.url = "${createLink(controller: 'userVS', action: 'search')}?searchText=" + this.textToSearch
                this.$.ajax.go()
            },
            toggleContactsView:function(event, detail, target) {
                if("contacts" === target.attributes['data-button'].value) this.modeSearch = false
                else  this.modeSearch = true
            }
        });
    </script>
</polymer-element>