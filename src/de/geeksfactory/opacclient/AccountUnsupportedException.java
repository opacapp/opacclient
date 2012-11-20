package de.geeksfactory.opacclient;

public class AccountUnsupportedException extends Exception {
	private static final long serialVersionUID = 1562173357204776777L;

	private String html;

	public String getHtml() {
		return html;
	}

	public AccountUnsupportedException(String html) {
		super();
		this.html = html;
	}
}
