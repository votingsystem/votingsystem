<div class="modal fade" id="clientToolAdvertDialog" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
    <div id="clientToolAdvertDialogDiv" class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="myModalLabel"><g:message code="clientToolDialogCaption"/></h4>
            </div>
            <div class="modal-body" style="font-size: 1.1em;">
                <g:message code="clientToolDialogMsg"/>
            </div>
            <div class="modal-footer">
                <button id="clientToolAdvertDialogCancelButton" type="submit" class="btn btn-accept-vs" onclick="disposeClientToolAdvertDialog()">
                    <g:message code="acceptLbl"/>
                </button>
            </div>
        </div>
    </div>
</div>
<asset:script>
    function disposeClientToolAdvertDialog() {
        $('#clientToolAdvertDialog').modal('hide')
    }

</asset:script>