<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/iron-localstorage/iron-localstorage.html" rel="import"/>

<dom-module name="uservs-selector">
    <template>
        <style>
            .card {
                width: 290px; vertical-align: top;
                background-color: #f9f9f9; box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
                border: 1px solid rgba(0, 0, 0, 0.24); margin: 10px; color: #667; cursor: pointer; padding: 5px;
            }
            .nif {font-size: 0.7em; color:#888;margin:5px 0 0 0; }
            .name {color: #000; font-size: 0.9em;}
        </style>
        <iron-localstorage id="localstorage" name="contacts-localstorage" value="{{contacts}}"></iron-localstorage>
        <iron-ajax id="ajax" url="{{url}}" last-response="{{userListDto}}" handle-as="json" content-type="application/json"></iron-ajax>
        <div class="vertical layout center">
            <div hidden={{!contactSelector}} class="horizontal layout center center-justified" style="margin:-10px 0 20px 0;">
                <div id="searchDiv" class="linkVS" on-click="setSearchView"
                     style="font-weight: bold; font-size: 1.1em; margin:0 40px 0 0;">${msg.userSearchLbl}</div>
                <div id="contactDiv" class="linkVS" on-click="setContactsView" style="font-weight: bold; font-size: 1.1em;">${msg.contactsLbl}</div>
            </div>

            <div hidden="{{!modeSearch}}" class="horizontal layout" style="width: 100%;">
                <div class="layout horizontal center center-justified flex" style="margin:0px auto 0px auto;">
                    <input id="inputSearch" type="text" style="width:200px;" class="form-control"
                           placeholder="${msg.userSearchLbl}" on-keypress="searchInputKeyPress">
                    <button on-click="processSearch" style="margin:0 0 0 5px;">
                        <i class="fa fa-search"></i> ${msg.searchLbl}
                    </button>
                </div>
            </div>
            <div hidden="{{modeSearch}}" class="horizontal layout center center-justified" style="width: 100%;">
                <div class="flex" style="font-size: 1.5em; font-weight: bold; color: #6c0404; text-align: center;">${msg.storeContactsLbl}</div>
            </div>
            <div class="horizontal layout wrap around-justified">
                <template is="dom-repeat" items="{{userListDto.resultList}}" as="uservs">
                    <div class="card" style="display: block;">
                        <div class="horizontal layout" on-click="showUserDetails">
                            <div class="layout flex vertical center-justified">
                                <div class="name">{{uservs.name}}</div>
                                <div class="name" style="margin: 5px 0 0 0;">{{uservs.lastName}}</div>
                            </div>
                            <div>
                                <div hidden="{{isContactButtonHidden(uservs)}}">
                                    <button on-click="toggleContact" style="font-size: 0.7em; margin:0 0 0 0;">
                                        <span>{{selectorModeMsg}}</span>
                                    </button>
                                </div>
                                <div class="nif flex horizontal layout center center-justified">{{uservs.nif}}</div>
                            </div>
                        </div>
                    </div>
                </template>
            </div>
            <div hidden="{{!userListEmpty}}"  class="center" style="margin:30px 0 30px 0; font-weight: bold;">${msg.emptyUserSearchResultMsg}</div>
        </div>
    </template>
    <script>
        Polymer({
            is:'uservs-selector',
            properties: {
                groupvsId:{type:Number},
                userListDto:{type:Object, value:{resultList:[]}, observer:'userListDtoChanged'},
                contactSelector:{type:Boolean, value:true},
                modeSearch:{type:Boolean, value:true, observer:'modeSearchChanged'},
                userListEmpty:{type:Boolean, value:false},
                selectorModeMsg:{type:String},
                textToSearch:{type:String},
                contactsArray:{type:Array, observer:'contactsArrayChanged'},
                contacts:{type:String, observer:'loadContacts'}
            },
            ready: function() {
                this.url = this.url || ''
                this.modeSearch = !this.contactSelector
                console.log(this.tagName + " - contactSelector: " + this.contactSelector)
            },
            modeSearchChanged:function(e) {
                console.log(this.tagName + " modeSearchChanged - modeSearch: " + this.modeSearch)
                if(this.modeSearch === true) {
                    this.userListDto = {resultList:[]}
                    this.$.searchDiv.style.borderBottom = '3px solid'
                    this.$.contactDiv.style.borderBottom = ''
                } else {
                    this.$.searchDiv.style.borderBottom = ''
                    this.$.contactDiv.style.borderBottom = '3px solid'
                    this.userListDto = {resultList:toJSON(this.contacts)}
                }
                this.userListEmpty = false
                this.selectorModeMsg = this.modeSearch ? '${msg.addContactLbl}':'${msg.removeContactLbl}'
            },
            loadContacts:function(e) {
                console.log(this.tagName + " - loadContacts")
                this.contactsArray = toJSON(this.contacts)
                if(this.contactSelector && !this.modeSearch) this.userListDto = {resultList:toJSON(this.contactsArray)}
            },
            showUserDetails:function(e) {
                document.querySelector("#voting_system_page").dispatchEvent(
                        new CustomEvent('user-clicked', { 'detail': e.model.uservs }));
            },
            reset: function() {
                this.userListDto = {resultList:[]}
            },
            userListDtoChanged: function() {
                if(!this.userListDto.resultList) {
                    console.log(this.tagName + " - userListDtoChanged - null userListDto.resultList")
                    return
                }
                console.log(this.tagName + " - userListDtoChanged - num. users: " + JSON.stringify(this.userListDto))
                this.userListEmpty = (this.userListDto.resultList.length === 0)
            },
            contactsArrayChanged: function(e) {
                this.contacts = JSON.stringify(this.contactsArray)
            },
            toggleContact: function(e) {
                e.stopPropagation()
                var uservs = e.model.uservs
                if(this.modeSearch === true) {
                    console.log("toggleContact - addContact")
                    if(!this.isContact(uservs)) {
                        this.userListDto = {resultList:[]}
                        this.contactsArray.push(uservs)
                        for(userIdx in this.userListDto.resultList) {
                            if(uservs.id === this.userListDto.resultList[userIdx].id) {
                                this.userListDto.resultList.splice(userIdx, 1)
                                console.log("==== deleted: " + userIdx)
                            }
                        }
                        this.userListDto = toJSON(JSON.stringify(this.userListDto)); //notify changes
                        this.contactsArray =  this.contactsArray.slice(0) //notify changes
                    }
                    e.target.style.display = 'none'
                } else {
                    console.log("toggleContact - removeContact")
                    var result = []
                    for(uservsIdx in this.contactsArray) {
                        if(this.contactsArray[uservsIdx].id !== uservs.id) result.push(this.contactsArray[uservsIdx])
                    }
                    this.contacts = JSON.stringify(result)
                }
            },
            isContact:function (uservs){
                var isInArray = false
                for(uservsIdx in this.contactsArray) {
                    if(this.contactsArray[uservsIdx].id === uservs.id) return true
                }
                console.log(this.tagName + " - isContact: " + isInArray)
                return isInArray
            },
            isContactButtonHidden:function (uservs){
                if(this.modeSearch && this.contactSelector) {
                    if(this.isContact(uservs)) return true
                    else return false
                } else if(this.contactSelector) return false
                else return true
            },
            searchInputKeyPress:function (e){
                var chCode = ('charCode' in e) ? e.charCode : e.keyCode;
                if (chCode == 13) this.processSearch()
            },
            processSearch:function() {
                this.textToSearch = this.$.inputSearch.value.trim()
                if(this.textToSearch === "") return
                if(this.groupVSId) this.url = contextURL + "/rest/groupVS/id/" + this.groupVSId + "/searchUsers?searchText=" + this.textToSearch
                else this.url = contextURL + "/rest/userVS/search?searchText=" + this.textToSearch
                console.log(this.tagName + " - processSearch - url: " + this.url)
                this.$.ajax.generateRequest()
            },
            setContactsView:function() {
                this.modeSearch = false
                if(this.contactsArray.length > 0) this.userListEmpty = false
            },
            setSearchView:function() {
                this.modeSearch = true
            }
        });
    </script>
</dom-module>