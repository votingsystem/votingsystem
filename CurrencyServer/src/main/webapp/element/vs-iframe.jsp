<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="vs-iframe">
    <template>
        <div class="flex">
            <iframe src="{{url}}" width="100%" frameBorder="0" style="height: 1000px; overflow:hidden;" scrolling="no"></iframe>
        </div>
    </template>
    <script>
        Polymer({
            is:'vs-iframe',
            properties: {
                url: {type:String, value:contextURL + "/app/index.xhtml"}
            },
            ready: function() { }
        });
    </script>
</dom-module>
