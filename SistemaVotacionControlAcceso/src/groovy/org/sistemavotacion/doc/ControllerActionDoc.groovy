package org.sistemavotacion.doc;

public class ControllerActionDoc {
	
	private CommentDoc commentDoc;
	private String method;
	private String uri;
	private int lineNumber;
	
	public ControllerActionDoc(String method, String uri) {
		this.setMethod(method);
		this.setUri(uri);
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	String getUri() {
		return uri;
	}

	void setUri(String uri) {
		this.uri = uri;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	
	public void setCommentDoc(CommentDoc commentDoc) {
		this.commentDoc = commentDoc;
	}
	
	public CommentDoc setCommentDoc( ) {
		return commentDoc;
	}
}
