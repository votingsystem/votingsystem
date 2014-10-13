<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">

<polymer-element name="innerpage-signal" attributes="title innerPage">
    <template></template>
    <script>
        Polymer('innerpage-signal', {
            title:null,
            innerPage:null,
            ready: function() {
                this.fire('core-signal', {name: "innerpage", data: {title:this.title, innerPage:this.innerPage}});
            }
        });
    </script>
</polymer-element>
