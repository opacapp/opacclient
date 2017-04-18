/**
 * Copyright (C) 2016 by Johan von Forstner under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.objects;

/**
 * Represents a volume of a series of media ({@link DetailedItem}) available in a library.
 *
 * This is to be used, if a search result is not a real item but more like a "meta item" for a
 * collection, for example a "Harry Potter" item containing a collection of all seven Harry Potter
 * books as child items.
 */
public class Volume {
    private String id;
    private String title;

    /**
     * @return Unique media identifier of a child item. Required.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id Unique media identifier of a child item. Required.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Title of a child item. Required.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title Title of a child item. Required.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "Volume{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
