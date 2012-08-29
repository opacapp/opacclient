package de.geeksfactory.opacclient;

public class AccountUnsupportedException extends Exception {
	private String html;

	public String getHtml() {
		return html;
	}

	public AccountUnsupportedException(String html) {
		super();
		this.html = html;
	}
}
