<style>
.tagBox {
    margin: 10px auto 30px auto;
    padding: 5px 10px 5px 10px;;
    border: 1px solid #ccc;
}

.tagBox legend {
    font-size: 1.2em;
    font-weight: bold;
    color:#6c0404;
}
</style>
<div id='addTagDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="addTagDialogCaption" class="modal-title" style="color: #6c0404; font-weight: bold;">
                    <g:message code="addTagDialogCaption"/>
                </h4>
            </div>
            <div class="modal-body">
                <div id="tagFieldsDiv" class="tagBox" style="display:none;">
                    <fieldset id="tagFieldsBox">
                        <legend id="tagFieldsLegend" style="border: none;"><g:message code="tagsLbl"/></legend>
                        <div id="tagFields" style=""></div>
                    </fieldset>
                </div>

                <div id="selectedTagDiv" style="border: 1px solid #ccc; padding: 5px; margin:0px 0px 10px 0px; display: none;">
                    <label><g:message code="selectedTagsLbl"/></label>
                    <div id="selectedTagList" class="" style=""></div>
                </div>

                <div id="tagSearchPanel" class="form-group form-inline text-center"
                     style="margin:15px auto 0px auto;display: inline-block; width: 100%;">
                    <input id="tagSearchInput" type="text" class="form-control" style="width:220px; display: inline;"
                           placeholder="<g:message code="tagLbl"/>">
                    <button type="button" onclick="processTagSearch()" class="btn btn-danger" style="display: inline;">
                        <g:message code="tagSearchLbl" /></button>
                </div>

                <div id="tagDialogSearchMessage" class="" style=""></div>
                <div id="tagList" class="" style="margin: 10px 0px 0px 0px;"></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-accept-vs" data-dismiss="modal" onclick="closeAddTagDialog();">
                    <g:message code="acceptLbl"/></button>
                <button type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal" style="">
                    <g:message code="closeLbl"/>
                </button>
            </div>
        </div>
    </div>
</div>
<div id="newTagTemplate" style="display:none;">
    <div class="form-group" style='margin: 10px 0px 5px 10px;display:inline'>
        <button data-tagId='{0}' onclick="addTag(this)" type="button" class="btn btn-default"
                style="margin:7px 10px 0px 0px;">
            {1}  <i class="fa fa-plus-circle"></i>
        </button>
    </div>
</div>
<div id="tagTemplate" style="display:none;">
    <button data-tagId='{0}' onclick="removeTag(this)" type="button" class="btn btn-default"
            style="margin:7px 10px 0px 0px;">
        {1}  <i class="fa fa-minus-circle"></i>
    </button>
</div>
<asset:script>

    var addTagDialogClientCallback = null
    var dynatableTagDialog
    var tagMap = {}
    var newTagTemplate = document.getElementById('newTagTemplate').innerHTML
    var tagTemplate = document.getElementById('tagTemplate').innerHTML

    function closeAddTagDialog() {
        if(addTagDialogClientCallback != null) addTagDialogClientCallback()
        $('#addTagDialog').modal('hide')
    }

    function showAddTagDialog(callback) {
        console.log("showAddTagDialog");
        $('#addTagDialog').modal('show')
        addTagDialogClientCallback = callback
    }

    function removeTag(tagButton) {
        var tagDataId = $(tagButton).attr('data-tagId')
        delete selectedTagsMap[tagDataId];
        $(tagButton).parent().fadeOut(400, function() { $(tagButton).parent().remove(); });
        if(Object.keys(selectedTagsMap).length == 0){
            document.getElementById("selectedTagDiv").style.display= 'none'
        }
    }

    function addTag(newTagButton) {
        var tagDataId = $(newTagButton).attr('data-tagId')
        selectedTagsMap[tagDataId] = tagMap[tagDataId]
        document.getElementById("selectedTagList").innerHTML = ''
        $(newTagButton).parent().fadeOut(400, function() { $(newTagButton).parent().remove(); });

        Object.keys(selectedTagsMap).forEach(function(entry) {
            var tagDiv = document.createElement("div");
            tagDiv.classList.add('form-group');
            tagDiv.setAttribute("style","margin: 10px 0px 5px 10px;display:inline;");
            tagDiv.innerHTML = tagTemplate.format(entry, tagMap[entry]);
            document.querySelector("#selectedTagList").appendChild(tagDiv);
        });
        document.getElementById("selectedTagDiv").style.display= 'block'
    }

    function processTagSearch() {
        userMap = {}
        var tagTextToSearch = document.getElementById("tagSearchInput").value
        if(tagTextToSearch.trim() == "") return
        document.getElementById('tagList').innerHTML=''
        dynatableTagDialog.settings.dataset.ajaxUrl = "${createLink(controller: 'balanceTagVS', action: 'index')}?tag=" + tagTextToSearch
        dynatableTagDialog.paginationPage.set(1);
        dynatableTagDialog.process();
    }

    $(function() {
        $("#tagSearchInput").bind('keypress', function(e) {if (e.which == 13) {
            processTagSearch();
            e.preventDefault();
        }});

        $('#tagList').dynatable({
            features: {
                paginate: false,
                search: false,
                recordCount: false,
                perPageSelect: false
            },
            inputs: dynatableInputs,
            params: dynatableParams,
            dataset: {
                ajax: true,
                ajaxOnLoad: false,
                perPageDefault: 50,
                records: []
            },
            writers: {
                _rowWriter: tagDialogTagRowWriter
            }
        });

        dynatableTagDialog = $('#tagList').data('dynatable');

        dynatableTagDialog.settings.params.records = 'tagRecords'
        dynatableTagDialog.settings.params.queryRecordCount = 'numTotalTags'
    })


    function tagDialogTagRowWriter(rowIndex, jsonTagData, columns, cellWriter) {
        tagMap[jsonTagData.id] = jsonTagData.name
        return newTagTemplate.format(jsonTagData.id, jsonTagData.name);
    }

</asset:script>