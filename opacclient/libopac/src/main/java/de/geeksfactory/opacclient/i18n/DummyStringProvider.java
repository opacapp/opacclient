/**
 * Copyright (C) 2013 by Johan von Forstner under the MIT license:
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
package de.geeksfactory.opacclient.i18n;

import de.geeksfactory.opacclient.objects.SearchResult;

/**
 * This is the simplest possible implementation of the {@link StringProvider} interface. For any
 * given string identifier, it just returns the identifier itself. Arguments (if any) will be
 * appended to the identifier separated by a space.
 */
public class DummyStringProvider implements StringProvider {
    @Override
    public String getString(String identifier) {
        return identifier;
    }

    @Override
    public String getFormattedString(String identifier, Object... args) {
        StringBuilder builder = new StringBuilder();
        builder.append(identifier);
        for (Object arg : args) {
            builder.append(" ");
            builder.append(arg.toString());
        }
        return builder.toString();
    }

    @Override
    public String getQuantityString(String identifier, int count, Object... args) {
        StringBuilder builder = new StringBuilder();
        builder.append(identifier)
               .append(" ")
               .append(count);
        for (Object arg : args) {
            builder.append(" ");
            builder.append(arg.toString());
        }
        return builder.toString();
    }

    @Override
    public String getMediaTypeName(SearchResult.MediaType mediaType) {
        return mediaType.toString();
    }
}
