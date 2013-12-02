<g:if test="${'mobile'.equals(attrs.type)}">
    <r:require modules="textEditorMobile"/>
</g:if>
<g:else><r:require modules="textEditorPC"/></g:else>
<div id='${attrs.id}' style="${attrs.style}"></div>
<div id="${attrs.id}EditorContents" class="editorContents"  style="display: none;"></div>
<r:script>
<g:if test="${'mobile'.equals(attrs.type)}">
    var editorConfig = {
        toolbar: [[ 'Bold', 'Italic', '-', 'NumberedList', 'BulletedList', '-', 'Link', 'Unlink' ],
			[ 'FontSize', 'TextColor', 'BGColor' ]]}
</g:if>
<g:else>
    var editorConfig = { toolbar: [[ 'Bold', 'Italic', '-', 'NumberedList', 'BulletedList', '-', 'Link', 'Unlink' ],
                [ 'FontSize', 'TextColor', 'BGColor' ]]}
</g:else>

var editor, ${attrs.id}Content = '';
	

CKEDITOR.on('instanceReady',function( ev ) {
	var height = $("#${attrs.id}").height() - 75
	$("#contentDiv").fadeIn(500)
	editor.resize( '100%', height, true )
});

var editorData
//showEditor and hideEditor are to avoid blocking the editor whith DOM changes
function showEditor_${attrs.id}() {
	if (editor) return;
	// Create a new editor inside the <div id="editor">, setting its value to editorDivContent
	$("#${attrs.id}EditorContents").hide()
	editor = CKEDITOR.appendTo( '${attrs.id}', editorConfig, editorData);
	$("#${attrs.id}").fadeIn()
	editor.focus();
}

//showEditor and hideEditor are to avoid blocking the editor whith DOM changes
function hideEditor_${attrs.id}() {
	if(!editor) return;
    $("#${attrs.id}").hide()
	var editorWidth = $("#${attrs.id}").width() - 20 //css padding
	var editorHeight = $("#${attrs.id}").height() - 20//css padding
	editorData = editor.getData()
	document.getElementById('${attrs.id}EditorContents').innerHTML = ${attrs.id}Content = editor.getData().substring(0,100) + "...";
	$("#${attrs.id}EditorContents").width(editorWidth).height(editorHeight);
	$("#${attrs.id}EditorContents").fadeIn(300)
	editor.destroy();
	editor = null;
}

</r:script>
