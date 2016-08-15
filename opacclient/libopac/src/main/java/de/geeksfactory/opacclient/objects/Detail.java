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

/**
 * Object representing a detail of a media item
 *
 * @author Raphael Michel
 */
public class Detail {
    private String desc;
    private String content;
    private boolean html;

    /**
     * Create a new detail
     *
     * @param desc    Description
     * @param content Content
     */
    public Detail(String desc, String content) {
        super();
        this.desc = desc;
        this.content = content;
    }

    /**
     * Create a new detail
     *
     * @param desc    Description
     * @param content Content
     */
    public Detail(String desc, String content, boolean html) {
        super();
        this.desc = desc;
        this.content = content;
        this.html = html;
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
     * Set this detail's description. Description in this context means something like "Title",
     * "Summary".
     *
     * @param desc the description
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
     * Set this detail's content. If the description is "Title", this should contain the actual
     * title, like "Harry Potter"
     *
     * @param content the content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns whether the content is to be treated as HTML or plain text.
     */
    public boolean isHtml() {
        return html;
    }

    /**
     * Set whether the content is to be treated as HTML or plain text.
     */
    public void setHtml(boolean html) {
        this.html = html;
    }

    @Override
    public String toString() {
        return "Detail [desc=" + desc + ", content=" + content + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Detail detail = (Detail) o;

        if (html != detail.html) return false;
        if (!desc.equals(detail.desc)) return false;
        return content.equals(detail.content);

    }

    @Override
    public int hashCode() {
        int result = desc.hashCode();
        result = 31 * result + content.hashCode();
        result = 31 * result + (html ? 1 : 0);
        return result;
    }
}
