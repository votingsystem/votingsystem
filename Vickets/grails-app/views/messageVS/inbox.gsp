<%@ page import="grails.converters.JSON" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <g:javascript library="jquery" plugin="jquery"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>
    <script type="text/javascript" src="${resource(dir: 'bower_components/bootstrap/dist/js', file: 'bootstrap.min.js')}"></script>
    <asset:stylesheet src="vickets.css"/>
</head>
<body>
<div class="pageContenDiv" style="max-width: 1000px; padding: 20px 30px 0px 30px;">

    <div style="display: table;width:90%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="messagevsStateSelect" style="margin:0px auto 0px auto;color:black; max-width: 300px;" class="form-control">
                <option value="PENDING"> - <g:message code="selectPendingMessaVSLbl"/> - </option>
                <option value="CONSUMED"> - <g:message code="selectConsumedMessaVSLbl"/> - </option>
            </select>
        </div>
    </div>

    <div id="adviceMessageDiv"><g:message code="messageVSPendingMsg"/></div>


    <div id="messagevs_tableDiv" style="margin: 20px auto 0px auto; max-width: 1200px; overflow:auto;">
        <table class="table white_headers_table" id="messagevs_table" style="">
            <thead>
            <tr style="color: #ff0000;">
                <th data-dynatable-column="dateCreated" style="width:270px;"><g:message code="dateLbl"/></th>
                <th data-dynatable-column="fromUser" style="width:270px;"><g:message code="fromUserLbl"/></th>
                <th data-dynatable-column="operation" style="width:270px;"></th>
            </tr>
            </thead>
        </table>
    </div>

</div>
<div id="messageVSTemplate" class="text-center"></div>
</body>
    <g:include view="/include/dialog/windowAlertModal.gsp"/>
</html>
<asset:script>

    //var messageVSList = ${raw(messageVSList)}

    var messageVSTemplate = document.getElementById("messageVSTemplate").innerHTML
    var messageVSMap = {}
    var dynatable
    var messageVSTable = document.getElementById("messagevs_table")


    function setMessageVSList(messageVSList) {
        for(var i = 0; i < messageVSList.length ; i++) {
            //fromUser:[id:messageVS.fromUserVS.id, name:messageVS.fromUserVS.getDefaultName()]
            //messageVSList.add([fromUser: fromUser, dateCreated:messageVS.dateCreated,
            //encryptedDataList:messageVSJSON.encryptedDataList]
            var messageVS = messageVSList[i]
            messageVSMap[messageVS.id] = messageVS

            //console.log(" - messageVS: " +  JSON.stringify(messageVSMap))
            var row = messageVSTable.insertRow(1);
            var cell1 = row.insertCell(0);
            var cell2 = row.insertCell(1);
            var cell3 = row.insertCell(2);
            cell1.innerHTML = '<tr><td title="" class="text-center">' + messageVS.dateCreated + '</td>';
            cell2.innerHTML = '<td title="" class="text-center">' + messageVS.fromUser.name + '</td>';
            cell3.innerHTML = '<td class="text-center"><button onclick="decryptMessageVS(' + messageVS.id + ')">' +
                '<g:message code="readMessageVSLbl"/></button></td></tr>';
        }

    }

    function updateMessageVSList(appMessage) {
        var appMessageJSON = toJSON(appMessage)
        if('PENDING' == appMessageJSON.state) {
            document.getElementById("adviceMessageDiv").innerHTML = '<g:message code="messageVSPendingMsg"/>'
        } else if('CONSUMED' == appMessageJSON.state) {
            document.getElementById("adviceMessageDiv").innerHTML = ''
        }

        var elmtTable = document.getElementById('messagevs_table');
        var tableRows = elmtTable.getElementsByTagName('tr');
        var rowCount = tableRows.length;
        for (var x=0; x < rowCount-1; x++) {
           document.getElementById("messagevs_table").deleteRow(1);
        }


        setMessageVSList(appMessageJSON.messageVSList)
    }


    $(function() {
        //setMessageVSList(messageVSList)

        $('#messagevsStateSelect').on('change', function (e) {
            var messagevsState = $(this).val()
            console.log("messagevsStateSelect: " + messagevsState)
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.MESSAGEVS_GET)
            webAppMessage.signedMessageSubject = "<g:message code="getMessageSubject"/>"
            webAppMessage.document = {operation:Operation.MESSAGEVS_GET, state:messagevsState}
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);

        });

    })

    function decryptMessageVS(messageVSId) {
        console.log("decryptMessageVS: " + messageVSId)
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.MESSAGEVS_DECRYPT)
        webAppMessage.signedMessageSubject = "<g:message code="decryptMessageSubject"/>"
        webAppMessage.callerCallback = 'showDecryptedMessageVS'
        webAppMessage.documentToDecrypt = messageVSMap[messageVSId]
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function showDecryptedMessageVS(messageVS) {
        var messageVSJSON = toJSON(messageVS)
        showWindowAlertModalMsg(messageVS.messageContent, '<g:message code="messageVSDecryptedCaption"/>')
    }

</asset:script>
<asset:deferredScripts/>