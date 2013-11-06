<r:require module="textEditorPC"/>    
<div id='${attrs.id}'></div>
<div id="editorContents" class="editorContents"  style="display: none"></div>
<r:script>
var editorConfig = {
        toolbar: [[ 'Bold', 'Italic', '-', 'NumberedList', 'BulletedList', '-', 'Link', 'Unlink' ],
			[ 'FontSize', 'TextColor', 'BGColor' ]]}

var editor, htmlEditorContent = '';

CKEDITOR.on('instanceReady',function( ev ) {
	$("#contentDiv").fadeIn(500)
});

//showEditor and hideEditor are to avoid blocking the editor whith DOM changes
function showEditor() {
	if (editor) return;
	// Create a new editor inside the <div id="editor">, setting its value to htmlEditorContent
	$("#editorContents").hide()
	editor = CKEDITOR.appendTo( '${attrs.id}', editorConfig, htmlEditorContent );
	editor.focus();
}

//showEditor and hideEditor are to avoid blocking the editor whith DOM changes
function hideEditor() {
	if(!editor) return;
	var editorWidth = $("#${attrs.id}").width() - 20 //css padding
	var editorHeight = $("#${attrs.id}").height() - 20//css padding
	document.getElementById('editorContents').innerHTML = htmlEditorContent = editor.getData();
	$("#editorContents").width(editorWidth).height(editorHeight);
	$("#editorContents").fadeIn(300)
	editor.destroy();
	editor = null;
}

</r:script>
