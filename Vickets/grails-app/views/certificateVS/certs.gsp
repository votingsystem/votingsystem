<%@ page import="org.votingsystem.model.CertificateVS" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <asset:javascript src="jquery.dynatable.js"/>
    <asset:stylesheet src="jquery.dynatable.css"/>
    <style type="text/css" media="screen">
        .certDiv {
            display:inline;
            width:580px;
            height: 300px;
            padding: 10px;
            background-color: #f2f2f2;
            margin: 10px 5px 5px 10px;
            -moz-border-radius: 5px; border-radius: 5px;
            cursor: pointer;
            max-height:165px;
            overflow:hidden;
            float:left;
        }
    </style>
</head>
<body>
<div class="pageContenDiv" style="max-width: 1300px; margin: 0px auto 0px auto;">
    <div class="row">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="certsPageTitle"/></li>
        </ol>
    </div>
    <div style="display: table;width:90%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div id="certTypeSelectDiv" style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="certTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;" class="form-control">
                <option value="&type=USER&state=OK"> - <g:message code="certUserStateOKLbl"/> - </option>
                <option value="&type=CERTIFICATE_AUTHORITY&state=OK"> - <g:message code="certAuthorityStateOKLbl"/> - </option>
                <option value="&type=USER&state=CANCELLED"> - <g:message code="certUserStateCancelledLbl"/> - </option>
                <option value="&type=CERTIFICATE_AUTHORITY&state=CANCELLED"> - <g:message code="certAuthorityStateCancelledLbl"/> - </option>
            </select>
        </div>
    </div>

    <h3><div id="pageHeader" class="pageHeader text-center"><g:message code="trustedCertsPageTitle"/></div></h3>

    <div id="certList" class="row container" style="width:1300px;"><ul></ul></div>

    <div style="margin:0px auto 0px auto;">
        <g:each in="${certsMap.certList}">
        </g:each>
    </div>
</div>
<div id="certificateVSTemplate" style="display:none;">
    <div>
        <li class="certDiv" style="">
            <a class="targetURL" href="{0}"></a>
            <div>
                <div style="display: inline;">
                    <div class='groupvsSubjectDiv' style="display: inline;"><span style="font-weight: bold;">
                        <g:message code="serialNumberLbl"/>: </span>{1}</div>
                    <div id="certStateDiv" style="display: inline; margin:0px 0px 0px 20px; font-size: 1.2em; font-weight: bold; float: right;">{2}</div>
                </div>
            </div>
            <div class='groupvsSubjectDiv'><span style="font-weight: bold;"><g:message code="subjectLbl"/>: </span>{3}</div>
            <div class=''><span style="font-weight: bold;"><g:message code="issuerLbl"/>: </span><a href="{9}">{4}</a></div>
            <div class=''><span style="font-weight: bold;"><g:message code="signatureAlgotithmLbl"/>: </span>{5}</div>
            <div>
                <div class='' style="display: inline;">
                    <span style="font-weight: bold;"><g:message code="noBeforeLbl"/>: </span>{6}</div>
                <div class='' style="display: inline; margin:0px 0px 0px 20px;">
                    <span style="font-weight: bold;"><g:message code="noAfterLbl"/>: </span>{7}</div>
            </div>
            <div>
                    <div class="text-center" style="font-weight: bold; display: {8};
                    margin:0px auto 0px auto;color: #6c0404; float:right; text-decoration: underline;"><g:message code="rootCertLbl"/></div>

            </div>
        </li>
    </div>
</div>
</body>
</html>
<asset:script>
    var dynatable
    var certType = getParameterByName('type')
    var certState = getParameterByName('state')
    var optionValue = "&type=" + ((certType == "")? "USER":certType) + "&state=" + ((certState == "")?"OK":certState)
    setPageHeader(optionValue)

    $(function() {
        $('#certList').dynatable({
            features: dynatableFeatures,
            inputs: dynatableInputs,
            params: dynatableParams,
            table: {
                bodyRowSelector: 'li'
            },
            dataset: {
                ajax: true,
                ajaxUrl: updateMenuLink("<g:createLink controller="certificateVS" action="certs"/>?" + optionValue),
                ajaxOnLoad: false,
                perPageDefault: 50,
                records: []
            },
            writers: {
                _rowWriter: certificateVSWriter
            }
        });

        $('#certTypeSelectDiv > select > option[value="' + optionValue + '"]').attr("selected", "selected")

        dynatable = $('#certList').data('dynatable');
        dynatable.settings.params.records = 'certList'
        dynatable.settings.params.queryRecordCount = 'queryRecordCount'
        dynatable.settings.params.totalRecordCount = 'numTotalCerts'


        <g:if test="${CertificateVS.Type.USER.toString().equals(certsMap.type)}">
            document.getElementById("pageHeader").innerHTML = "<g:message code="userCertsPageTitle"/>"
        </g:if><g:elseif test="${CertificateVS.Type.CERTIFICATE_AUTHORITY.toString().equals(certsMap.type)}">
            document.getElementById("pageHeader").innerHTML = "<g:message code="trustedCertsPageTitle"/>"
        </g:elseif><g:else>
            document.getElementById("pageHeader").innerHTML = "${certsMap?.type?.toString()}"
        </g:else>

        var newCertificateVSTemplate = $('#certificateVSTemplate').html()
        function certificateVSWriter(rowIndex, jsonAjaxData, columns, cellWriter) {

            var targetURL = "${createLink( controller:'certificateVS', action:'cert')}/" + jsonAjaxData.serialNumber
            var issuerTargetURL = "#"
            if(jsonAjaxData.issuerSerialNumber != null) issuerTargetURL =
                    "${createLink( controller:'certificateVS', action:'cert')}/" + jsonAjaxData.issuerSerialNumber

            var stateLbl
            if("${CertificateVS.State.OK.toString()}" == jsonAjaxData.state) stateLbl = "<g:message code="certOKLbl"/>"
            else if("${CertificateVS.State.CANCELLED.toString()}" == jsonAjaxData.state) stateLbl = "<g:message code="certCancelledLbl"/>"
            else stateLbl = jsonAjaxData.state

            var displayValue = "none"
            if(jsonAjaxData.isRoot) displayValue = "inline"

            var newCertificateVSHTML = newCertificateVSTemplate.format(targetURL, jsonAjaxData.serialNumber, stateLbl,
                    jsonAjaxData.subjectDN, jsonAjaxData.issuerDN, jsonAjaxData.sigAlgName, jsonAjaxData.notBefore,
                    jsonAjaxData.notAfter, displayValue, issuerTargetURL);
            return newCertificateVSHTML
        }

        $('#certList').bind('dynatable:afterUpdate',  function() {
            updateMenuLinks()
            $('.certDiv').click(function() {
                window.location.href = $(this).find('.targetURL').attr('href')
            }
        )})

        $('#certTypeSelect').on('change', function (e) {
            var optionSelected = $(this).val()
            console.log("transactionvs selected: " + optionSelected)
            var targetURL = updateMenuLink("${createLink(controller: 'certificateVS', action: 'certs')}")
            if("" != optionSelected) {
                history.pushState(null, null, targetURL);
                targetURL = targetURL + optionSelected
            }
            dynatable.settings.dataset.ajaxUrl= targetURL
            history.pushState(null, null, targetURL);
            setPageHeader(targetURL)
            dynatable.paginationPage.set(1);
            dynatable.process();
        });
    })

    function setPageHeader(urlParams) {
        if(urlParams.indexOf("${CertificateVS.Type.USER}") > -1)
            document.getElementById("pageHeader").innerHTML = "<g:message code="userCertsPageTitle"/>"
        else document.getElementById("pageHeader").innerHTML = "<g:message code="trustedCertsPageTitle"/>"
    }

</asset:script>