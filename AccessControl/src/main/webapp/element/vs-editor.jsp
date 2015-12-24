<%@ page contentType="text/html; charset=UTF-8" %>

<!--
based on https://github.com/JaySunSyn/polymer-tinymce

localization ->https://www.tinymce.com/docs/configure/localization/
-->

<link href="../resources/tinymce.html" rel="import"/>

<dom-module name="vs-editor">
    <template>
        <textarea id="polymerTinymceTextarea" name="content" style="width:100%" class="te"></textarea>
    </template>
    <script>
        Polymer({
            is:'vs-editor',
            properties:{
                tinytoolbar:{ type:String, value:"undo | bullist" },
                tinyplugins:{ type:Array, value:["advlist autolink lists link image charmap preview anchor fullscreen"] },
                templates: { type: Array, value: function () { return [] } },
                height: { type: Number, value: 168 },
                baseUrl: { type: String, value: '' }
            },
            ready: function() {
                console.log(this.tagName + "ready")
            },
            attached: function(){
                this.async(function () {
                    this.initEditor();
                }.bind(this),100);
            },
            initEditor:function () {
                if (this.baseUrl !== '') {
                    tinymce.baseURL = this.baseUrl;
                }
                tinymce.init({
                    selector: "#polymerTinymceTextarea",
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
            },
            execCommand:function(command){
                tinyMCE.activeEditor.execCommand(command);
            },
            getContent:function(){
                return tinyMCE.activeEditor.getContent();
            },
            setContent:function(content){
                if (tinyMCE && tinyMCE.activeEditor){
                    tinyMCE.activeEditor.setContent(content);
                }
            }
        });
    </script>
</dom-module>