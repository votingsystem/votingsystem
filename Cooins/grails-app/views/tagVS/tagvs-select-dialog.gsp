<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="core-ajax" file="core-ajax.html"/>
<vs:webresource dir="paper-input" file="paper-input.html"/>
<vs:webresource dir="paper-button" file="paper-button.html"/>
<vs:webresource dir="core-icon-button" file="core-icon-button.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog-transition.html"/>
<link rel="import" href="${resource(dir: '/bower_components/vs-i18n', file: 'vs-i18n.html')}">


<polymer-element name="tagvs-select-dialog" attributes="caption serviceURL">
<template>
    <paper-dialog id="xDialog" autoCloseDisabled layered backdrop title="{{caption}}" class="selectTagDialog"
                  on-core-overlay-open="{{onCoreOverlayOpen}}">
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
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 400px;
            }
        </style>
        <g:include view="/include/styles.gsp"/>
        <vs-i18n id="i18nVS"></vs-i18n>
        <div layout vertical>
            <div layout horizontal center center-justified style="margin:0 0 10px 0;">
                <div flex style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;display:{{caption? 'block':'none'}}"><g:message code="addTagLbl"/></div>
                </div>
                <div style="position: absolute; top: 0px; right: 0px;">
                    <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                </div>
            </div>

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

            <div layout horizontal center center-justified>
                <input id="tagSearchInput" class="form-control" required autofocus
                       title="{{messages.tagLbl}}" placeholder="{{messages.tagLbl}}"/>
                <paper-button raised on-click="{{searchTag}}" style="margin: 0px 0px 0px 5px; width: 130px; font-size: 0.9em;">
                    <i class="fa fa-search"></i>  {{messages.tagSearchLbl}}
                </paper-button>
            </div>

            <div style="display: {{searchString!= null ? 'block':'none'}}">
                <div style="text-align:center;margin:10px 0 0 10px; font-size: 1.1em;">
                    {{messages.searchResultLbl}} <b>'{{searchString}}'</b>
                </div>
            </div>

            <div>
                <div flex horizontal wrap layout center center-justified>
                    <template repeat="{{tag in searchedTagList}}">
                        <a class="btn btn-default" on-click="{{selectTag}}" style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                            {{tag.name}} <i class="fa fa-plus" style="color: #6c0404;"></i></a>
                    </template>
                </div>
            </div>

            <div layout horizontal style="margin:20px 0 0 0;">
                <div flex></div>
                <paper-button raised on-click="{{processTags}}" style="font-size: 0.9em;">
                    <i class="fa fa-check"></i> {{messages.acceptLbl}}
                </paper-button>
            </div>
        </div>
        <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get"
                   contentType="json"></core-ajax>
    </paper-dialog>
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
            this.$.xDialog.title = this.caption
            this.$.xDialog.opened = true
        },

        processTags: function() {
            this.fire('tag-selected', this.selectedTagList);
            this.close()
        }

    });
</script>
</polymer-element>