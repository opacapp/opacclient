package de.geeksfactory.opacclient.utils;

import org.json.JSONObject;

import java.util.Iterator;

/**
 * This {@link Iterator} takes a {@link JSONObject} and iterates over its keys. In
 * contrast to {@link JSONObject#keys()}, this Iterator will always return Strings instead of
 * generic Objects, so it prevents unchecked casts. If the value returned from {@link
 * JSONObject#keys()}'s {@link Iterator#next()} method is no String (which probably cannot happen),
 * the {@link #next()} method will throw a {@link java.lang.IllegalArgumentException}.
 */
public class JsonKeyIterator implements Iterator<String> {
    private Iterator jsonIter;

    public JsonKeyIterator(JSONObject object) {
        jsonIter = object.keys();
    }

    public boolean hasNext() {
        return jsonIter.hasNext();
    }

    public String next() {
        Object next = jsonIter.next();
        if (next instanceof String) {
            return (String) next;
        } else {
            throw new IllegalArgumentException("A non-String key was found inside the JSONObject");
        }
    }

    public void remove() {
        jsonIter.remove();
    }
}
