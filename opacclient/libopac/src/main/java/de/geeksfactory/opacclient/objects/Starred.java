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
 * Object representing a bookmarked item. Not part of the API you are interested
 * in if you want to implement a library system.
 *
 * @author Raphael Michel
 */
public class Starred {
    private int id;
    private String mnr;
    private String title;

    @Override
    public String toString() {
        return "Starred [id=" + id + ", mnr=" + mnr + ", title=" + title + "]";
    }

    /**
     * Get this item's ID in bookmark database
     */
    public int getId() {
        return id;
    }

    /**
     * Set this item's ID in bookmark database
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get this item's unique identifier
     */
    public String getMNr() {
        return mnr;
    }

    /**
     * Set this item's unique identifier
     */
    public void setMNr(String mnr) {
        this.mnr = mnr;
    }

    /**
     * Get this item's title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set this item's title
     */
    public void setTitle(String title) {
        this.title = title;
    }
}
