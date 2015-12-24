<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../element/vs-editor.vsp" rel="import"/>

<dom-module name="eventvs-editor">
    <template>
        <div style="max-width: 1000px;">
            <vs-editor id="editor"></vs-editor>
        </div>
    </template>
    <script>
        Polymer({
            is:'eventvs-editor',
            properties:{

            },
            ready: function() {
                console.log(this.tagName + "ready")
            }
        });
    </script>
</dom-module>