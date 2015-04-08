<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>

    <link href="${resourceURL}/core-overlay/core-overlay.html" rel="import"/>
    <link href="${resourceURL}/core-ajax/core-ajax.html" rel="import"/>
    <link rel="import" href="${resourceURL}/core-transition/core-transition-css.html">
    <link href="${elementURL}/tagVS/tagvs-select-dialog.vsp" rel="import"/>
    <link href="${resourceURL}/paper-item/paper-item.html" rel="import"/>
    <style shim-shadowdom>
    </style>
</head>
<body>

    <button onclick="document.querySelector('#tagDialog').show()" style="margin:10px;">Tag dialog</button>

    <tagvs-select-dialog id="tagDialog" caption="${msg.addTagDialogCaption}"
                                    serviceURL="${restURL}/tagVS"></tagvs-select-dialog>

</body>
</html>
<script>
    var webAppMessage = new WebAppMessage( this.operation)
    webAppMessage.serviceURL = ${restURL}/transactionVS"
    webAppMessage.signedMessageSubject = "${msg.transactionvsFromGroupMsgSubject}"
    webAppMessage.signedContent = {operation:"TEST"}

    webAppMessage.setCallback(function () {
            console.log("====#### callback")
    })
    window[webAppMessage.objectId]()
</script>
