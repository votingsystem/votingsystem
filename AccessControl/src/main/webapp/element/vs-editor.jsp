<%@ page contentType="text/html; charset=UTF-8" %>

<!--
based on https://github.com/JaySunSyn/polymer-tinymce

localization ->https://www.tinymce.com/docs/configure/localization/
-->

<link href="../resources/tinymce.html" rel="import"/>

<dom-module name="vs-editor">
    <template>
        <div id="editorContainer"></div>
    </template>
    <script>
        Polymer({
            is:'vs-editor',
            properties:{
                tinytoolbar:{ type:String, value:"undo | bullist" },
                tinyplugins:{ type:Array, value:["advlist autolink lists link image charmap preview anchor fullscreen"] },
                templates: { type: Array, value: function () { return [] } },
                height: { type: Number, value: 168 },
                baseUrl: { type: String, value: '' },
                contentMap: { type: Object, value: {} }
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.textareaId = this.id + Math.random().toString(36).substring(7);
            },
            detached: function(){
                this.$.editorContainer.style.display = 'none'
                this.contentMap[this.textareaId] = this.getContent()
                tinymce.EditorManager.execCommand('mceRemoveEditor',true, this.textareaId);
            },
            attached: function(){
                this.$.editorContainer.innerHTML = '<textarea id="' + this.textareaId + '" name="content" style="width:100%" class="te"></textarea>'
                this.async(function () {
                    this.initEditor();
                }.bind(this),100);
            },
            initEditor:function () {
                if (this.baseUrl !== '') {
                    tinymce.baseURL = this.baseUrl;
                }
                tinymce.init({
                    selector: "#" + this.textareaId,
                    language: '${spa.language()}',
                    templates: this.templates,
                    plugins: this.tinyplugins,
                    toolbar: this.tinytoolbar,
                    height: this.height,
                    setup: function (ed) {
                        ed.on('init', function(args) {
                            this.fire('tiny-init');
                        });
                        ed.on('focus', function(e) {
                            this.fire('tiny-focus');
                        });
                        ed.on('NodeChange', function(e) {
                            this.fire('tiny-node');
                        });
                    }
                });
                this.setContent(this.contentMap[this.textareaId] || "")
            },
            execCommand:function(command){
                tinyMCE.activeEditor.execCommand(command);
            },
            getContent:function(){
                return tinyMCE.activeEditor.getContent();
            },
            setContentText:function(content){
                tinyMCE.activeEditor.setContent(content);
                this.$.editorContainer.style.display = 'block'
            },
            setContent:function(content){
                this.contentMap[this.textareaId] = content
                if (tinyMCE && tinyMCE.activeEditor){
                    this.setContentText(content)
                } else {
                    this.async(function () {
                        this.setContent(content)
                    }.bind(this),100);
                }
            }
        });
    </script>
</dom-module>