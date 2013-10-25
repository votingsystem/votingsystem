<script src="${resource(dir:'ckeditor',file:'ckeditor.js')}"></script>
<g:render template="/template/js/mobileUtils"/>
<script type="text/javascript">
// To show the editor the host page must invoke the method showEditor()

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
	editor = CKEDITOR.appendTo( 'editor', editorConfig, htmlEditorContent );
	editor.focus();
}

//showEditor and hideEditor are to avoid blocking the editor whith DOM changes
function hideEditor() {
	if(!editor) return;
	var editorWidth = $("#editor").width() - 20 //css padding
	var editorHeight = $("#editor").height() - 20//css padding
	document.getElementById('editorContents').innerHTML = htmlEditorContent = editor.getData();
	$("#editorContents").width(editorWidth).height(editorHeight);
	$("#editorContents").fadeIn(300)
	editor.destroy();
	editor = null;
}

</script>
