<div id='confirmOptionDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="resultCaption" class="modal-title" style="color: #870000; font-weight: bold;">
                    <g:message code="confirmOptionDialogCaption"/>
                </h4>
            </div>
            <div class="modal-body">
                <p style="text-align: center;">
                    <g:message code="confirmOptionDialogMsg"/>:<br>
                    <b><span id="optionSelectedDialogMsg"></span></b>
                </p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-accept-vs" data-dismiss="modal" onclick="sendVote()">
                    <g:message code="acceptLbl"/></button>
                <button type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal"
                        onclick="proccessCancelRepresentativeSelection()">
                    <g:message code="cancelLbl"/>
                </button>
            </div>
        </div>
    </div>
</div>
<r:script>
</r:script>