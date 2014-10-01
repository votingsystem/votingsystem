<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-i18n', file: 'votingsystem-i18n.html')}">


<polymer-element name="tagvs-select-dialog" attributes="caption serviceURL">
<template>
    <votingsystem-dialog id="xDialog" class="selectTagDialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
        <style no-shim>
            .selectTagDialog {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 13px;
                -webkit-user-select: none;
                -moz-user-select: none;
                overflow: auto;
                background: white;
                padding:10px 30px 30px 30px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 400px;
            }
        </style>
        <g:include view="/include/styles.gsp"/>
        <votingsystem-i18n id="i18nVS"></votingsystem-i18n>
        <div id="container" style="padding:10px;">
            <div layout vertical>

            <div>
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">{{caption}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>
            </div>

            <div>
                <div layout vertical wrap style="border: 1px solid #ccc; padding:10px; margin:0px 0px 10px 0px;
                display:{{(selectedTagList == null || selectedTagList.length == 0) ? 'none':'block'}}">
                    <div style="font-weight: bold; margin:0px 0px 5px 0px;">{{messages.selectedTagsLbl}}</div>
                    <div flex horizontal wrap layout style="">
                        <template repeat="{{tag in selectedTagList}}">
                            <a class="btn btn-default" on-click="{{removeTag}}" style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                                {{tag.name}} <i class="fa fa-minus"></i></a>
                        </template>
                    </div>
                </div>
            </div>

            <div>
                <div flex layout horizontal center center-justified>
                    <input id="tagSearchInput" class="form-control" required autofocus
                           title="{{messages.tagLbl}}" placeholder="{{messages.tagLbl}}"/>
                    <votingsystem-button on-click="{{searchTag}}" style="margin: 0px 0px 0px 5px;">
                        <i class="fa fa-search" style="margin:0 5px 0 2px;"></i>  {{messages.tagSearchLbl}}
                    </votingsystem-button>
                </div>
            </div>

            <div style="display: {{searchString!= null ? 'block':'none'}}">
                <div style="text-align:center;margin:10px 0 0 10px;">
                    {{messages.searchResultLbl}} <b>'{{searchString}}'</b>
                </div>
            </div>

            <div>
                <div flex horizontal wrap layout style="">
                    <template repeat="{{tag in searchedTagList}}">
                        <a class="btn btn-default" on-click="{{selectTag}}" style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                            {{tag.name}} <i class="fa fa-plus"></i></a>
                    </template>
                </div>
            </div>

            <div layout horizontal style="margin:20px 0 0 0;">
                <div flex></div>
                <votingsystem-button on-click="{{processTags}}">
                    <i class="fa fa-check" style="margin:0 5px 0 2px;"></i> {{messages.acceptLbl}}
                </votingsystem-button>
            </div>
            </div>
        </div>
        <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get"
                   contentType="json"></core-ajax>
    </votingsystem-dialog>
</template>
<script>
    Polymer('tagvs-select-dialog', {
        caption: null,
        isShowingTags: false,
        maxNumberTags:3,
        serviceURL: null,
        searchString:null,
        ready: function() {
            this.messages = this.$.i18nVS.getMessages()
            this.selectedTagList = []
            this.searchedTagList =[]
            this.$.tagSearchInput.onkeypress = function(event){
                if (event.keyCode == 13) this.searchTag()
            }.bind(this)
        },

        close:function() {
            this.$.xDialog.opened = false
            this.$.tagSearchInput.value = ''
            this.searchedTagList = []
            this.selectedTagList = []
            this.$.tagSearchInput.value = ""
            this.isShowingTags = false
            this.searchString = null
            this.$.ajax.url = ""
        },
        responseDataChanged:function () {
            if(this.responseData != null) this.searchedTagList = this.responseData.tagRecords
            else this.searchedTagList = []
        },

        searchTag: function() {
            if(this.$.tagSearchInput.validity.valid) {
                this.$.ajax.url = this.serviceURL + "?tag=" + this.$.tagSearchInput.value
                console.log(this.tagName + " - searchTag - this.$.ajax.url: " + this.$.ajax.url)
                this.searchString = this.$.tagSearchInput.value
            }
        },

        selectTag: function(e) {
            var selectedTag = e.target.templateInstance.model.tag
            var isNewTag = true
            for(tagIdx in this.selectedTagList) {
                if(selectedTag.id == this.selectedTagList[tagIdx].id) isNewTag = false
            }
            console.log("selectTag: " + selectedTag.id + " - isNewTag: " + isNewTag + " - maxNumTags: " + this.maxNumTags +
                    " - num. tags selected: " + this.selectedTagList.length)
            if(isNewTag) {
                if(this.selectedTagList.length == this.maxNumTags ) {
                    this.searchedTagList.push(this.selectedTagList[0])
                    this.selectedTagList.splice(0, 1)
                }
                this.selectedTagList.push(selectedTag)
                for(tagIdx in this.searchedTagList) {
                    if(selectedTag.id == this.searchedTagList[tagIdx].id) this.searchedTagList.splice(tagIdx, 1)
                }
            }
        },

        removeTag: function(e) {
            var selectedTag = e.target.templateInstance.model.tag
            for(tagIdx in this.selectedTagList) {
                if(selectedTag.id == this.selectedTagList[tagIdx].id) {
                    this.selectedTagList.splice(tagIdx, 1)
                    this.searchedTagList.push(selectedTag)
                }
            }
        },

        reset: function() {
            this.selectedTagList = []
            this.searchedTagList = []
        },

        show: function (maxNumberTags, selectedTags) {
            this.maxNumTags = maxNumberTags
            if(selectedTags == null) this.selectedTagList = []
            else this.selectedTagList = selectedTags
            this.isShowingTags = true
            this.$.xDialog.opened = true
        },

        processTags: function() {
            this.fire('tag-selected', this.selectedTagList);
            this.close()
        }

    });
</script>
</polymer-element>