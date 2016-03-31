<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="contact-page">
    <template>
        <div class="pagevs vertical layout center center-justified">
            <a class="buttonvs" style="width: 280px;font-size: 1.1em;" href="mailto:${config.emailAdmin}">
                <i class="fa fa-envelope"></i> ${msg.emailLbl}
            </a>
            <div class="flex"></div>
        </div>
    </template>
    <script>
        Polymer({is:'contact-page'});
    </script>
</dom-module>
