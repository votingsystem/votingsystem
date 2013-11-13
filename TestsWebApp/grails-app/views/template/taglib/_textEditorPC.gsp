<r:require modules="textEditorPC"/>
<div id='${attrs.id}' style="${attrs.style}"></div>
<div id="${attrs.id}EditorContents" class="editorContents"  style="display: none"></div>
<r:script>
var editorConfig = {
        toolbar: [[ 'Bold', 'Italic', '-', 'NumberedList', 'BulletedList', '-', 'Link', 'Unlink' ],
			[ 'FontSize', 'TextColor', 'BGColor' ]]}

var editor, ${attrs.id}Content = '';

	

CKEDITOR.on('instanceReady',function( ev ) {
	var height = $("#${attrs.id}").height() - 75
	$("#contentDiv").fadeIn(500)
	console.log("======= height: " + height)
	editor.resize( '100%', height, true )
});

//showEditor and hideEditor are to avoid blocking the editor whith DOM changes
function showEditor_${attrs.id}() {
	if (editor) return;
	// Create a new editor inside the <div id="editor">, setting its value to htmlEditorContent
	$("#${attrs.id}EditorContents").hide()
	editor = CKEDITOR.appendTo( '${attrs.id}', editorConfig, ${attrs.id}Content );
	editor.focus();
}

//showEditor and hideEditor are to avoid blocking the editor whith DOM changes
function hideEditor_${attrs.id}() {
	if(!editor) return;
	var editorWidth = $("#${attrs.id}").width() - 20 //css padding
	var editorHeight = $("#${attrs.id}").height() - 20//css padding
	document.getElementById('${attrs.id}EditorContents').innerHTML = ${attrs.id}Content = editor.getData();
	$("#${attrs.id}EditorContents").width(editorWidth).height(editorHeight);
	$("#${attrs.id}EditorContents").fadeIn(300)
	editor.destroy();
	editor = null;
}

</r:script>
