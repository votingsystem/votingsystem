<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="tools-page">
    <template>
        <div class="pagevs vertical layout center center-justified">
            <a class="buttonvs" style="width: 280px;font-size: 1.1em;" data-route="publish_election" href="/publish_election">
                <i class="fa fa-envelope"></i> ${msg.publishVoteLbl}
            </a>
            <a class="buttonvs" style="width: 280px; margin: 10px 0 0 0; font-size: 1.1em;" data-route="edit_representative" href="/representative/edit">
                <i class="fa fa-hand-o-right"></i> ${msg.newRepresentativeLbl}
            </a>
            <a class="buttonvs" style="width: 280px;margin: 10px 0 10px 0; font-size: 1.1em;" href="${contextURL}/tools/NativeClient.zip">
                <i class="fa fa-download"></i> ${msg.validationToolMsg}
            </a>
            <a href="http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk">
                ${msg.javaRequirementsMsg}
            </a>
            <div class="flex"></div>
        </div>
    </template>
    <script>
        Polymer({is:'tools-page'});
    </script>
</dom-module>
