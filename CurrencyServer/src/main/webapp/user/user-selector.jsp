<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="user-selector">
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
        <div class="pagevs vertical layout center" style="max-width: 100%; margin:7px;">
            <div hidden={{!contactSelector}} class="horizontal layout center center-justified"
                 style="margin:10px 0 20px 0; cursor: pointer; border-bottom: 1px solid #888; color: #888;width: 100%;">
                <div id="searchUserDiv" on-click="setSearchView"
                     style="font-weight: bold; font-size: 1.1em; margin:0 40px 0 0;">${msg.userSearchLbl}</div>
                <div id="contactDiv" on-click="setContactsView" style="font-weight: bold; font-size: 1.1em; cursor: pointer;">${msg.contactsLbl}</div>
            </div>

            <div hidden="{{!modeSearch}}" class="horizontal layout" style="width: 100%;">
                <div class="layout horizontal center center-justified flex" style="margin:0px auto 0px auto;">
                    <input id="inputSearch" on-keyup="searchInputKeyPress" type="text" style="width:200px;" class="form-control"
                           placeholder="${msg.userSearchLbl}">
                    <div class="buttonvs" on-click="processSearch" style="margin:0 0 0 5px;">
                        <i class="fa fa-search"></i> ${msg.searchLbl}
                    </div>
                </div>
            </div>
            <div hidden="{{modeSearch}}" class="horizontal layout center center-justified" style="width: 100%;">
                <div class="flex" style="font-size: 1.5em; font-weight: bold; color: #6c0404; text-align: center;">${msg.storeContactsLbl}</div>
            </div>
            <div class="horizontal layout wrap around-justified">
                <template is="dom-repeat" items="{{userListDto.resultList}}" as="user">
                    <div class="card" style="display: block;">
                        <div class="horizontal layout" on-click="showUserDetails">
                            <div class="layout flex vertical center-justified">
                                <div class="name">{{user.firstName}}</div>
                                <div class="name" style="margin: 5px 0 0 0;">{{user.lastName}}</div>
                            </div>
                            <div>
                                <div hidden="{{isContactButtonHidden(user)}}">
                                    <div class="buttonvs" on-click="toggleContact" style="font-size: 0.7em; margin:0 0 0 0;">
                                        <span>{{selectorModeMsg}}</span>
                                    </div>
                                </div>
                                <div class="nif flex horizontal layout center center-justified">{{user.nif}}</div>
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
            is:'user-selector',
            properties: {
                url:{type:String, observer:'getHTTP'},
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
                this.modeSearch = !this.contactSelector
                console.log(this.tagName + " - contactSelector: " + this.contactSelector)
                if(typeof(Storage) === "undefined") {
                    alert("Browser without localStorage support")
                } else {
                    this.contacts = localStorage.contacts
                }
            },
            modeSearchChanged:function(e) {
                console.log(this.tagName + " modeSearchChanged - modeSearch: " + this.modeSearch)
                if(this.modeSearch === true) {
                    this.userListDto = {resultList:[]}
                    this.$.searchUserDiv.style.borderBottom = '3px solid #b3b'
                    this.$.contactDiv.style.borderBottom = ''
                } else {
                    this.$.searchUserDiv.style.borderBottom = ''
                    this.$.contactDiv.style.borderBottom = '3px solid #b3b'
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
                        new CustomEvent('user-clicked', { 'detail': e.model.user }));
            },
            reset: function() {
                this.userListDto = {resultList:[]}
            },
            userListDtoChanged: function() {
                if(!this.userListDto.resultList) {
                    console.log(this.tagName + " - userListDtoChanged - empty list")
                    return
                }
                console.log(this.tagName + " - userListDtoChanged: ", this.userListDto)
                this.userListEmpty = (this.userListDto.resultList.length === 0)
            },
            contactsArrayChanged: function(e) {
                this.contacts = JSON.stringify(this.contactsArray)
                localStorage.contacts = this.contacts
            },
            toggleContact: function(e) {
                e.stopPropagation()
                var user = e.model.user
                if(this.modeSearch === true) {
                    console.log("toggleContact - addContact")
                    if(!this.isContact(user)) {
                        this.userListDto = {resultList:[]}
                        if(!this.contactsArray) this.contactsArray = []
                        this.contactsArray.push(user)
                        for(userIdx in this.userListDto.resultList) {
                            if(user.id === this.userListDto.resultList[userIdx].id) {
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
                    for(userIdx in this.contactsArray) {
                        if(this.contactsArray[userIdx].id !== user.id) result.push(this.contactsArray[userIdx])
                    }
                    this.contacts = JSON.stringify(result)
                }
            },
            isContact:function (user){
                var isInArray = false
                for(userIdx in this.contactsArray) {
                    if(this.contactsArray[userIdx].id === user.id) return true
                }
                console.log(this.tagName + " - isContact: " + isInArray)
                return isInArray
            },
            isContactButtonHidden:function (user){
                if(this.modeSearch && this.contactSelector) {
                    if(this.isContact(user)) return true
                    else return false
                } else if(this.contactSelector) return false
                else return true
            },
            searchInputKeyPress:function (e){
                if(e.keyCode === 13) this.processSearch();
            },
            processSearch:function() {
                this.textToSearch = this.$.inputSearch.value.trim()
                if(this.textToSearch === "") return
                this.url = vs.contextURL + "/rest/user/search?searchText=" + this.textToSearch
            },
            setContactsView:function() {
                this.modeSearch = false
                if(this.contactsArray && this.contactsArray.length > 0) this.userListEmpty = false
            },
            setSearchView:function() {
                this.modeSearch = true
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                if(!targetURL) return
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                vs.getHTTPJSON(targetURL, function(responseText){
                    this.userListDto = toJSON(responseText)
                }.bind(this))
            }
        });
    </script>
</dom-module>