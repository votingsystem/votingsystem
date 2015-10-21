<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-i18n/vs-i18n.html" rel="import">

<dom-module name="tagvs-select-dialog">
<template>
    <paper-dialog id="xDialog" with-backdrop no-cancel-on-outside-click title="{{caption}}" style="max-width: 600px; min-height: 400px;">
        <vs-i18n id="i18nVS"></vs-i18n>
        <div style="max-width: 600px; height: 100%;">
            <div style="margin:0 0 10px 0;">
                <div class="flex" style="font-size: 1.3em; font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;">${msg.addTagLbl}</div>
                </div>
                <div style="position: absolute; top: 0px; right: 0px;">
                    <i class="fa fa-times closeIcon" on-click="close"></i>
                </div>
            </div>

            <div hidden="{{tagBoxHidden}}" class="layout vertical wrap" style="border: 1px solid #ccc;
                    padding:10px; margin:0px 0px 10px 0px; display: block;">
                <div style="font-weight: bold; margin:0px 0px 5px 0px;"><span>{{messages.selectedTagsLbl}}</span></div>
                <div class="flex horizontal wrap layout">
                    <template is="dom-repeat" items="{{selectedTagList}}">
                        <div><a class="btn btn-default" on-click="removeTag" style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                            <i class="fa fa-minus"></i> <span>{{item.name}}</span></a></div>
                    </template>
                </div>
            </div>

            <div class="vertical layout flex">
                <div class="layout horizontal center center-justified flex">
                    <input id="tagSearchInput" class="form-control" required autofocus
                           title="{{messages.tagLbl}}" placeholder="{{messages.tagLbl}}"/>
                    <button on-click="searchTag" style="font-size: 1.1em;margin: 0px 0px 0px 5px; width: 160px;">
                        <i class="fa fa-search"></i> <span>{{messages.tagSearchLbl}}</span>
                    </button>
                </div>
            </div>

            <div hidden="{{!searchString}}">
                <div style="text-align:center;margin:10px 0 0 10px; font-size: 1.1em;">
                    <span>{{messages.searchResultLbl}}</span> <b><span>{{searchString}}</span></b>
                </div>
            </div>
            <div>
                <div class="flex horizontal wrap layout center center-justified">
                    <template is="dom-repeat" items="[[resultListDto.resultList]]">
                        <a class="btn btn-default" on-click="selectTag" style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                            <i class="fa fa-plus" style="color: #6c0404;"></i> <span>{{item.name}}</span></a>
                    </template>
                </div>
            </div>

            <div class="layout horizontal" style="margin:20px 0 0 0;">
                <div class="flex"></div>
                <button on-click="processTags" style="font-size: 1.1em;">
                    <i class="fa fa-check" style="color: #388746;"></i> <span>{{messages.acceptLbl}}</span>
                </button>
            </div>
        </div>
        <iron-ajax id="ajax" auto url="{{url}}" last-response="{{resultListDto}}" handle-as="json" method="get"
                   content-type="application/json"></iron-ajax>
    </paper-dialog>
</template>
<script>
    Polymer({
        is:'tagvs-select-dialog',
        properties: {
            searchString:{type:String, value:null},
            numTags:{type:Number, value:1},
            caption:{type:String, value:null},
            tagBoxHidden:{type:Boolean, value:true},
            serviceUrl:{type:String, value:restURL + "/tagVS"},
            resultListDto:{type:Object},
            messages:{type:Object},
            searchResultTagList:{type:Array, value:[]}
        },
        ready: function() {
            this.messages = this.$.i18nVS.getMessages()
            console.log(this.tagName + " - ready")
            this.$.tagSearchInput.onkeypress = function(event){
                if (event.keyCode == 13) this.searchTag()
            }.bind(this)
        },
        close:function() {
            this.$.xDialog.opened = false
            this.$.tagSearchInput.value = ''
            this.selectedTagList = []
            this.searchString = null
            this.$.ajax.url = ""
        },
        searchTag: function() {
            if(this.$.tagSearchInput.validity.valid) {
                this.$.ajax.url = this.serviceUrl + "?tag=" + this.$.tagSearchInput.value
                console.log(this.tagName + " - searchTag - this.$.ajax.url: " + this.$.ajax.url)
                this.searchString = this.$.tagSearchInput.value
            }
        },
        selectTag: function(e) {
            var selectedTag = e.model.item
            var isNewTag = true
            for(tagIdx in this.selectedTagList) {
                if(selectedTag.id == this.selectedTagList[tagIdx].id) isNewTag = false
            }
            console.log("selectTag: " + selectedTag.id + " - isNewTag: " + isNewTag + " - numTags: " + this.numTags +
                    " - num. tags selected: " + this.selectedTagList.length + " - selectedTagList: " + this.selectedTagList)
            if(isNewTag) {
                if(this.selectedTagList.length == this.numTags ) {
                    this.searchResultTagList.push(this.selectedTagList[0])
                    this.selectedTagList.splice(0, 1)
                }
                this.selectedTagList.push(selectedTag)
                for(tagIdx in this.searchResultTagList) {
                    if(selectedTag.id == this.searchResultTagList[tagIdx].id) this.searchResultTagList.splice(tagIdx, 1)
                }
            }
            this.selectedTagList = this.selectedTagList.slice(0) //to make changes visible to template
            this.tagBoxHidden = (this.selectedTagList.length === 0)
        },
        removeTag: function(e) {
            var selectedTag = e.model.item
            for(tagIdx in this.selectedTagList) {
                if(selectedTag.id == this.selectedTagList[tagIdx].id) {
                    this.selectedTagList.splice(tagIdx, 1)
                    this.searchResultTagList.push(selectedTag)
                }
            }
            this.selectedTagList = this.selectedTagList.slice(0); //hack to notify array changes
            this.searchResultTagList = this.searchResultTagList.slice(0); //hack to notify array changes
            this.tagBoxHidden = (this.selectedTagList.length === 0)
        },
        reset: function() {
            this.selectedTagList = []
            this.searchResultTagList = []
        },
        show: function (numTags, selectedTags) {
            if(numTags != null) this.numTags = numTags
            if(selectedTags == null) this.selectedTagList = []
            else this.selectedTagList = selectedTags
            this.tagBoxHidden = (this.selectedTagList.length === 0)
            this.$.xDialog.title = this.caption
            this.$.xDialog.opened = true
        },
        processTags: function() {
            this.fire('tag-selected', this.selectedTagList);
            this.close()
        }
    });
</script>
</dom-module>
