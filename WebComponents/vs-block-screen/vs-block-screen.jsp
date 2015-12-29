<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="vs-block-screen">
    <template>
        <div id="modalDialog" class="modalDialog"></div>
    </template>
    <script>
        Polymer({
            is:'vs-block-screen',
            block(isBlocked) {
                console.log(this.tagName + " --- screenBlocked: " + isBlocked)
                if(isBlocked) {
                    this.$.modalDialog.style.opacity = 1
                    this.$.modalDialog.style['pointer-events'] = 'auto'
                } else {
                    this.$.modalDialog.style.opacity = 0
                    this.$.modalDialog.style['pointer-events'] = 'none'
                }
            }
        });
    </script>
</dom-module>
