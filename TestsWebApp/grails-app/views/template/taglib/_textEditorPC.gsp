<r:require modules="textEditorPC"/>
<div id='${attrs.id}' style="${attrs.style}"></div>
<div id="${attrs.id}EditorContents" class="editorContents"  style="display: none;"></div>
<r:script>
var editorConfig = {
        toolbar: [[ 'Bold', 'Italic', '-', 'NumberedList', '-', 'Link', 'Unlink' ],
			[ 'FontSize', 'TextColor', 'BGColor' ]]}

var editor, ${attrs.id}Content = '';

CKEDITOR.on('instanceReady',function( ev ) {
	var height = $("#${attrs.id}").height() - 75
	$("#contentDiv").fadeIn(500)
	editor.resize( '100%', height, true )
});

//showEditor and hideEditor are to avoid blocking the editor whith DOM changes
function showEditor_${attrs.id}() {
	if (editor) return;
	$("#${attrs.id}EditorContents").hide()
	editor = CKEDITOR.appendTo( '${attrs.id}', editorConfig, ${attrs.id}Content);
	$("#${attrs.id}").fadeIn()
	editor.focus();
}

//showEditor and hideEditor are to avoid blocking the editor whith DOM changes
function getEditor_${attrs.id}Data() {
	if(!editor) return ${attrs.id}Content.trim();
    $("#${attrs.id}").hide()
	var editorWidth = $("#${attrs.id}").width() - 20 //css padding
	var editorHeight = $("#${attrs.id}").height() - 20//css padding
    ${attrs.id}Content = editor.getData()
	var editorContent = editor.getData()
	if(editorContent.length > 800) editorContent = editorContent.substring(0,800) + "...";
	document.getElementById('${attrs.id}EditorContents').innerHTML = editorContent;
	$("#${attrs.id}EditorContents").width(editorWidth).height(editorHeight);
	$("#${attrs.id}EditorContents").fadeIn(300)
	editor.destroy();
	editor = null;
	return editorContent.trim()
}

</r:script>
