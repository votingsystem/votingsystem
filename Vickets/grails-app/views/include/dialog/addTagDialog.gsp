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
                <div id="tagDialogSelectedTagDiv" style="border-bottom: 1px solid #ccc; padding: 5px;
                        margin:0px 0px 10px 0px; display: none;">
                    <label><g:message code="selectedTagsLbl"/></label>
                    <div id="tagDialogSelectedTagList" class="" style=""></div>
                </div>

                <div id="tagSearchPanel" class="form-group form-inline text-center"
                     style="margin:15px auto 0px auto;display: inline-block; width: 100%;">
                    <input id="tagSearchInput" type="text" class="form-control" style="width:220px; display: inline;"
                           placeholder="<g:message code="tagLbl"/>">
                    <button type="button" onclick="processTagSearch()" class="btn btn-default" style="display: inline;">
                        <g:message code="tagSearchLbl" /></button>
                </div>

                <div id="tagDialogSearchMessage" class="" style=""></div>
                <div id="tagDialogTagListSearch" class="" style="margin: 10px 0px 0px 0px;"></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default btn-cancel-vs"
                        onclick="refreshTagCallback(selectedTagsMapTagDialog);" data-dismiss="modal" style="">
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

    var refreshTagCallback
    var tagMap = {}
    var selectedTagsMapTagDialog
    var newTagTemplate = document.getElementById('newTagTemplate').innerHTML
    var tagTemplate = document.getElementById('tagTemplate').innerHTML
    var maxNumTags

    function showAddTagDialog(maxNumberTags, selectedTags, callback) {
        console.log("showAddTagDialog");
        maxNumTags = maxNumberTags
        refreshTagCallback = callback
        document.getElementById("tagSearchInput").value= ''
        document.getElementById("tagDialogTagListSearch").innerHTML = ''
        document.getElementById("tagDialogSelectedTagList").innerHTML = ''
        document.getElementById("tagDialogSelectedTagDiv").style.display= 'none'
        selectedTagsMapTagDialog = selectedTags
        refreshTagsTagDialog()
        $('#addTagDialog').modal('show')
    }

    function removeTag(tagButton) {
        var tagDataId = $(tagButton).attr('data-tagId')
        delete selectedTagsMapTagDialog[tagDataId];
        $(tagButton).parent().fadeOut(400, function() { $(tagButton).parent().remove(); });
        if(Object.keys(selectedTagsMapTagDialog).length == 0){
            document.getElementById("tagDialogSelectedTagDiv").style.display= 'none'
        }
        refreshTagCallback(selectedTagsMapTagDialog)
    }

    function refreshTagsTagDialog() {
        document.getElementById("tagDialogSelectedTagList").innerHTML = ''
        Object.keys(selectedTagsMapTagDialog).forEach(function(entry) {
            var tagDiv = document.createElement("div");
            tagDiv.classList.add('form-group');
            tagDiv.setAttribute("style","margin: 10px 0px 5px 10px;display:inline;");
            tagDiv.innerHTML = tagTemplate.format(entry, tagMap[entry]);
            document.querySelector("#tagDialogSelectedTagList").appendChild(tagDiv);
        });
        if(maxNumTags != null && Object.keys(selectedTagsMapTagDialog).length > maxNumTags) {
            removeTag(document.getElementById("tagDialogSelectedTagList").getElementsByTagName('button')[0])
        }
        if(Object.keys(selectedTagsMapTagDialog).length > 0)
            document.getElementById("tagDialogSelectedTagDiv").style.display= 'block'
    }

    function addTag(newTagButton) {
        $(newTagButton).parent().fadeOut(400, function() { $(newTagButton).parent().remove(); });
        var tagDataId = $(newTagButton).attr('data-tagId')
        selectedTagsMapTagDialog[tagDataId] = tagMap[tagDataId]
        refreshTagsTagDialog()
        refreshTagCallback(selectedTagsMapTagDialog)
    }

    function processTagSearch() {
        userMap = {}
        var tagTextToSearch = document.getElementById("tagSearchInput").value
        if(tagTextToSearch.trim() == "") return
        document.getElementById("tagDialogTagListSearch").innerHTML = ''
        getHTTPRequest("${createLink(controller: 'vicketTagVS', action: 'index')}?tag=" + tagTextToSearch);
    }

    function getHTTPRequest (tagetURL) {
        console.log("getHTTPRequest - tagetURL: " + tagetURL)
        var request = new XMLHttpRequest();
        request.open('GET', tagetURL, true);
        request.onload = function (e) {
            if (request.readyState === 4) {
                if (request.status === 200) {// Check if the get was successful.
                    var responseJSON = toJSON(request.responseText)
                    responseJSON.tagRecords.forEach(function (entry) {
                        var tagDiv = document.createElement("div");
                        tagDiv.classList.add('form-group');
                        tagDiv.setAttribute("style","margin: 10px 0px 5px 10px;display:inline;");
                        tagDiv.innerHTML = newTagTemplate.format(entry.id, entry.name);
                        tagMap[entry.id] = entry.name;
                        document.querySelector("#tagDialogTagListSearch").appendChild(tagDiv);
                    });
                } else console.error(request.statusText);
            } else console.error(request.statusText);
        };
        request.send(null);
    }

    $(function() {
        $("#tagSearchInput").bind('keypress', function(e) {if (e.which == 13) {
            processTagSearch();
            e.preventDefault();
        }});
    })

</asset:script>