package de.geeksfactory.opacclient.objects;

public class Detail {
	private String desc;
	private String content;

	public Detail(String desc, String content) {
		super();
		this.desc = desc;
		this.content = content;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
