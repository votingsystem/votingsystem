<!DOCTYPE html>
<html>
<head>
    <title>Validator</title>
    <meta name="layout" content="main" />
    <script type="text/javascript" src="${resource(dir: 'bower_components/bootstrapValidator/dist/js', file: 'bootstrapValidator.min.js')}"></script>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrapValidator/dist/css', file: 'bootstrapValidator.min.css')}" type="text/css"/>
    <style>
    textarea { }
    input[id="subject"] { }
    </style>
</head>
<body>
    <div style="max-width: 800px;">
        <form id="depositDialogForm"  method="post"  class="">
            <div class="modal-body">
                <div class="row">
                    <div class="col-md-8">
                        <div class="form-group" style="display: inline-block;">
                            <label style="display: inline;"><g:message code="amountLbl"/>: </label>
                            <input type="number" id="amount" min="1" value="0" class="form-control"
                                   style="width:120px;margin:0px 0px 0px 0px;display: inline;" name="amount">
                            <label style="display: inline;">EUR</label>
                        </div>
                    </div>
                </div>
                <div class="form-group" style="margin:15px 0px 15px 0px;">
                    <label><g:message code="subjectLbl"/></label>
                    <textarea id="subject" class="form-control" rows="2" name="subject"></textarea>
                </div>


                <div class="form-group">
                    <label>amountText</label>
                    <input type="text" id="amountText" class="form-control" name="amountText" />
                </div>


                <label id="selectReceptorlblDiv" style=""></label>
                <div id="receptorPanelDiv">
                    <div id="fieldsDiv" class="fieldsBox" style="display:none;">
                        <fieldset id="fieldsBox">
                            <legend id="fieldsLegend" style="border: none;"><g:message code="receptorLbl"/></legend>
                            <div id="fields" style=""></div>
                        </fieldset>
                    </div>

                    <div id="searchPanel" class="form-group form-inline text-center"
                         style="margin:15px auto 0px auto;display: inline-block; width: 100%;">
                        <input id="userSearchInput" type="text" class="form-control" style="width:220px; display: inline;">
                        <button type="button" onclick="processSearch()" class="btn btn-danger" style="display: inline;">
                            <g:message code="userSearchLbl" /></button>
                    </div>

                    <div id="uservs_tableDiv" style="margin: 20px auto 0px auto; max-width: 800px; overflow:auto; visibility: hidden;">
                        <table class="table white_headers_table" id="uservs_table" style="">
                            <thead>
                            <tr style="color: #ff0000;">
                                <th data-dynatable-column="uservsNIF" style="width: 60px;"><g:message code="nifLbl"/></th>
                                <th data-dynatable-column="uservsName" style=""><g:message code="nameLbl"/></th>
                                <!--<th data-dynatable-no-sort="true"><g:message code="voucherLbl"/></th>-->
                            </tr>
                            </thead>
                        </table>
                    </div>
                </div>


            </div>
            <div class="modal-footer">
                <div class="form-group">
                    <div class="col-lg-5 col-lg-offset-3">
                        <button type="submit" class="btn btn-primary">Validate</button>
                    </div>
                </div>
                <button id="advancedSearchCancelButton" type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal" style="">
                    <g:message code="closeLbl"/>
                </button>
            </div>
        </form>
    </div>


<button type="button" class="btn btn-default btn-lg">
    <span class="glyphicon glyphicon-star"></span> Star
</button>

<form id="accountForm" method="post" class="form-horizontal">
    <div class="tab-content">
        <div class="tab-pane active" id="info-tab">
            <div class="form-group">
                <label class="col-lg-3 control-label">Full name</label>
                <div class="col-lg-5">
                    <input type="text" class="form-control" name="fullName" />
                </div>
            </div>
            <div class="form-group">
                <label class="col-lg-3 control-label">Company</label>
                <div class="col-lg-5">
                    <input type="text" class="form-control" name="company" />
                </div>
            </div>
            <div class="form-group">
                <label class="col-lg-3 control-label">Job title</label>
                <div class="col-lg-5">
                    <input type="text" class="form-control" name="jobTitle" />
                </div>
            </div>
        </div>


    </div>

    <div class="form-group">
        <div class="col-lg-5 col-lg-offset-3">
            <button type="submit" class="btn btn-primary">Validate</button>
        </div>
    </div>
</form>
</body>
</html>
<asset:script>

    function submitForm(form) {
        var result = document.getElementById("testTime").getValidatedTime()
        console.log("result: " + result)
        return false
    }

    $(document).ready(function(){









        $('#accountForm').bootstrapValidator({
            // Only disabled elements are excluded
            // The invisible elements belonging to inactive tabs must be validated
            excluded: [':disabled'],
            feedbackIcons: {
                valid: 'glyphicon glyphicon-ok',
                invalid: 'glyphicon glyphicon-remove',
                validating: 'glyphicon glyphicon-refresh'
            },
            fields: {
                fullName: {
                    validators: {
                        notEmpty: {
                            message: 'The full name is required'
                        }
                    }
                },
                company: {
                    validators: {
                        notEmpty: {
                            message: 'The company name is required'
                        }
                    }
                },
                address: {
                    validators: {
                        notEmpty: {
                            message: 'The address is required'
                        }
                    }
                },
                city: {
                    validators: {
                        notEmpty: {
                            message: 'The city is required'
                        }
                    }
                }
            }
        });
    });

</asset:script>