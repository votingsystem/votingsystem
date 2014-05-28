<script type="text/javascript" src="/TestWebApp/ckeditor/ckeditor.js"></script>
<div id='${attrs.id}' style="${attrs.style}"></div>
<asset:script>
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

function showEditor_${attrs.id}() {
	editor = CKEDITOR.appendTo( '${attrs.id}', editorConfig);
	$("#${attrs.id}").fadeIn()
	editor.focus();
}

function getEditor_${attrs.id}Data() {
	return editor.getData().trim()
}

$(function() {showEditor_${attrs.id}() })
</asset:script>