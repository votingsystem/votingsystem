<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="contact-page">
    <template>
        <div style="font-size: 1.2em; margin: 30px auto; text-align: center;">
            <a  href="mailto:${config.emailAdmin}" style="font-weight: bold;">
                <i class="fa fa-envelope-o" style="margin:3px 0 0 10px; color: #f0ad4e;"></i> ${msg.emailLbl}
            </a>
        </div>
    </template>
    <script>
        Polymer({is:'contact-page'});
    </script>
</dom-module>
