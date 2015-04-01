<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>

    <link href="${config.resourceURL}/core-overlay/core-overlay.html" rel="import"/>
    <link href="${config.resourceURL}/core-ajax/core-ajax.html" rel="import"/>
    <link rel="import" href="${config.resourceURL}/core-transition/core-transition-css.html">
    <link href="${config.webURL}/tagVS/tagvs-select-dialog.vsp" rel="import"/>
    <link href="${config.resourceURL}/paper-item/paper-item.html" rel="import"/>
    <style shim-shadowdom>
    </style>
</head>
<body>

    <button onclick="document.querySelector('#tagDialog').show()" style="margin:10px;">Tag dialog</button>

    <tagvs-select-dialog id="tagDialog" caption="${msg.addTagDialogCaption}"
                                    serviceURL="${config.restURL}/tagVS"></tagvs-select-dialog>

</body>
</html>
<script>
    var webAppMessage = new WebAppMessage( this.operation)
    webAppMessage.serviceURL = ${config.restURL}/transactionVS"
    webAppMessage.signedMessageSubject = "${msg.transactionvsFromGroupMsgSubject}"
    webAppMessage.signedContent = {operation:"TEST"}

    webAppMessage.setCallback(function () {
            console.log("====#### callback")
    })
    window[webAppMessage.objectId]()
</script>
