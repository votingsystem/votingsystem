<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="feeds-page">
    <template>
        <div style="font-size: 1.2em; margin: 30px auto; text-align: center;">
            <a target="_blank" href="${contextURL}/rest/subscription/elections">
                <i class="fa fa-rss-square" style="margin:3px 0 0 10px; color: #f0ad4e;"></i> ${msg.subscribeToVotingFeedsLbl}
            </a>
        </div>
    </template>
    <script>
        Polymer({is:'feeds-page',
            ready: function() {
                console.log(this.tagName + " - ready - ")
            },});
    </script>
</dom-module>
