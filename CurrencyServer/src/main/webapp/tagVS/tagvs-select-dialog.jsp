<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-i18n/vs-i18n.html" rel="import">

<dom-module name="tagvs-select-dialog">
<template>
    <div id="modalDialog" class="modalDialog">
        <vs-i18n id="i18nVS"></vs-i18n>
        <div style="max-width: 600px;">
            <div style="margin:0 0 10px 0;">
                <div class="flex" style="font-size: 1.3em; font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;">${msg.addTagLbl}</div>
                </div>
                <div style="position: absolute; top: 0px; right: 0px;">
                    <i class="fa fa-times closeIcon" on-click="close"></i>
                </div>
            </div>

            <div hidden="[[tagBoxHidden]]" class="layout vertical wrap" style="border: 1px solid #ccc;
                    padding:10px; margin:0px 0px 10px 0px; display: block;">
                <div style="font-weight: bold; margin:0px 0px 5px 0px;"><span>[[messages.selectedTagsLbl]]</span></div>
                <div class="flex horizontal wrap layout">
                    <template is="dom-repeat" items="[[selectedTagList]]">
                        <div><a class="btn btn-default" on-click="removeTag" style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;cursor: pointer;">
                            <i class="fa fa-minus"></i> <span>[[item.name]]</span></a></div>
                    </template>
                </div>
            </div>

            <div>
                <div class="flex horizontal wrap layout center center-justified">
                    <template is="dom-repeat" items="[[resultListDto.resultList]]">
                        <a class="btn btn-default" on-click="selectTag" style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;cursor: pointer;">
                            <i class="fa fa-plus" style="color: #6c0404;"></i> <span>[[item.name]]</span></a>
                    </template>
                </div>
            </div>

            <div class="layout horizontal" style="margin:20px 0 0 0;">
                <div class="flex"></div>
                <button on-click="processTags" style="font-size: 1.1em;">
                    <i class="fa fa-check" style="color: #388746;"></i> <span>[[messages.acceptLbl]]</span>
                </button>
            </div>
        </div>
    </div>
</template>
<script>
    Polymer({
        is:'tagvs-select-dialog',
        properties: {
            url:{type:String, observer:'getHTTP'},
            numTags:{type:Number, value:1},
            caption:{type:String, value:null},
            tagBoxHidden:{type:Boolean, value:true},
            resultListDto:{type:Object},
            messages:{type:Object},
            tagList:{type:Array, value:[]}
        },
        ready: function() {
            this.messages = this.$.i18nVS.getMessages()
            console.log(this.tagName + " - ready")
        },
        close:function() {
            this.$.modalDialog.style.opacity = 0
            this.$.modalDialog.style['pointer-events'] = 'none'
            this.selectedTagList = []
            this.searchString = null
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
                    this.tagList.push(this.selectedTagList[0])
                    this.selectedTagList.splice(0, 1)
                }
                this.selectedTagList.push(selectedTag)
                for(tagIdx in this.tagList) {
                    if(selectedTag.id == this.tagList[tagIdx].id) this.tagList.splice(tagIdx, 1)
                }
            }
            this.selectedTagList = this.selectedTagList.slice(0) //to make changes visible to template
            this.tagBoxHidden = (this.selectedTagList.length === 0)
        },
        removeTag: function(e) {
            var selectedTag = e.model.item
            for(tagIdx in this.selectedTagList) {
                if(selectedTag.id === this.selectedTagList[tagIdx].id) {
                    this.selectedTagList.splice(tagIdx, 1)
                    this.tagList.push(selectedTag)
                }
            }
            this.selectedTagList = this.selectedTagList.slice(0); //hack to notify array changes
            this.tagList = this.tagList.slice(0); //hack to notify array changes
            this.tagBoxHidden = (this.selectedTagList.length === 0)
        },
        reset: function() {
            this.selectedTagList = []
            this.tagList = []
        },
        show: function (numTags, selectedTags) {
            if(numTags != null) this.numTags = numTags
            if(selectedTags == null) this.selectedTagList = []
            else this.selectedTagList = selectedTags
            this.tagBoxHidden = (this.selectedTagList.length === 0)
            this.url = vs.contextURL + "/rest/tagVS/list"
            this.$.modalDialog.style.opacity = 1
            this.$.modalDialog.style['pointer-events'] = 'auto'
        },
        processTags: function() {
            this.fire('tag-selected', this.selectedTagList);
            this.close()
        },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                this.resultListDto = toJSON(rawData.response)
            }.bind(this));
        }
    });
</script>
</dom-module>
