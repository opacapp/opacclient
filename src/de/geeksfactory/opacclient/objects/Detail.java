/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.objects;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Object representing a detail of a media item
 * 
 * @author Raphael Michel
 */
public class Detail implements Parcelable {
	private String desc;
	private String content;

	/**
	 * Create a new detail
	 * 
	 * @param desc
	 *            Description
	 * @param content
	 *            Content
	 */
	public Detail(String desc, String content) {
		super();
		this.desc = desc;
		this.content = content;
	}

	/**
	 * Get this detail's description
	 * 
	 * @return the description
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * Set this detail's description. Description in this context means
	 * something like "Title", "Summary".
	 * 
	 * @param desc
	 *            the description
	 */
	public void setDesc(String desc) {
		this.desc = desc;
	}

	/**
	 * Get this detail's content.
	 * 
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Set this detail's content. If the description is "Title", this should
	 * contain the actual title, like "Harry Potter"
	 * 
	 * @param content
	 *            the content
	 */
	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "Detail [desc=" + desc + ", content=" + content + "]";
	}

    protected Detail(Parcel in) {
        desc = in.readString();
        content = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(desc);
        dest.writeString(content);
    }

    public static final Parcelable.Creator<Detail> CREATOR = new Parcelable.Creator<Detail>() {
        @Override
        public Detail createFromParcel(Parcel in) {
            return new Detail(in);
        }

        @Override
        public Detail[] newArray(int size) {
            return new Detail[size];
        }
    };
}