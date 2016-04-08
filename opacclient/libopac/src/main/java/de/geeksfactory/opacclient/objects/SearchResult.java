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

import java.util.List;

import de.geeksfactory.opacclient.searchfields.SearchQuery;

/**
 * Object representing a search result
 *
 * @author Raphael Michel
 */
public class SearchResult implements CoverHolder {
    private MediaType type;
    private int nr;
    private String id;
    private String innerhtml;
    private Status status;
    private byte[] coverBitmap;
    private String cover;
    private int page;
    private List<SearchQuery> childQuery;

    /**
     * Create a new SearchResult object
     *
     * @param type      media type (like "BOOK")
     * @param nr        Position in result list
     * @param innerhtml HTML to display
     */
    public SearchResult(MediaType type, int nr, String innerhtml) {
        this.type = type;
        this.nr = nr;
        this.innerhtml = innerhtml;
    }

    /**
     * Create an empty object
     */
    public SearchResult() {
        this.type = MediaType.NONE;
        this.nr = 0;
        this.innerhtml = "";
    }

    /**
     * Get the unique identifier of this object
     *
     * @return ID or <code>null</code> if unknown
     */
    public String getId() {
        return id;
    }

    /**
     * Set the unique identifier of this object
     *
     * @param id unique identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get this item's media type.
     *
     * @return Media type or <code>null</code> if unknown
     */
    public MediaType getType() {
        return type;
    }

    /**
     * Set this item's media type.
     *
     * @param type Media type
     */
    public void setType(MediaType type) {
        this.type = type;
    }

    /**
     * Get this item's position in result list
     *
     * @return position
     */
    public int getNr() {
        return nr;
    }

    /**
     * Set this item's position in result list
     *
     * @param nr position
     */
    public void setNr(int nr) {
        this.nr = nr;
    }

    /**
     * Get HTML describing the item to the user in a result list.
     *
     * @return position
     */
    public String getInnerhtml() {
        return innerhtml;
    }

    /**
     * Set HTML describing the item to the user in a result list. Only "simple" HTML like
     * {@code <b>}, {@code <i>}, etc. can be used.
     *
     * @param innerhtml simple HTML code
     */
    public void setInnerhtml(String innerhtml) {
        this.innerhtml = innerhtml;
    }

    /**
     * Get item status (if known)
     *
     * @return Status or <code>null</code> if not set.
     * @since 2.0.7
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Set item status (if known)
     *
     * @since 2.0.7
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Get the page this result was found on
     */
    public int getPage() {
        return page;
    }

    /**
     * Set the page this result was found on
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * Get cover image bitmap
     */
    @Override
    public byte[] getCoverBitmap() {
        return coverBitmap;
    }

    /**
     * Set cover image bitmap
     */
    @Override
    public void setCoverBitmap(byte[] coverBitmap) {
        this.coverBitmap = coverBitmap;
    }

    /**
     * Get cover image URL
     */
    @Override
    public String getCover() {
        return cover;
    }

    /**
     * Set cover image URL
     */
    @Override
    public void setCover(String cover) {
        this.cover = cover;
    }

    /**
     * Get the child query (see setChildQuery for details)
     */
    public List<SearchQuery> getChildQuery() {
        return childQuery;
    }

    /**
     * Set the child query. If this is set, clicking the item in the UI will not
     * open a detail page, but start another search.
     */
    public void setChildQuery(
            List<SearchQuery> childQuery) {
        this.childQuery = childQuery;
    }

    @Override
    public String toString() {
        return "SearchResult [id= " + id + ", type=" + type + ", nr=" + nr
                + ", innerhtml=" + innerhtml + "]";
    }

    /**
     * Supported media types.
     *
     * @since 2.0.3
     */
    public enum MediaType {
        NONE, BOOK, CD, CD_SOFTWARE, CD_MUSIC, DVD, MOVIE, AUDIOBOOK, PACKAGE,
        GAME_CONSOLE, EBOOK, SCORE_MUSIC, PACKAGE_BOOKS, UNKNOWN, NEWSPAPER,
        BOARDGAME, SCHOOL_VERSION, MAP, BLURAY, AUDIO_CASSETTE, ART, MAGAZINE,
        GAME_CONSOLE_WII, GAME_CONSOLE_NINTENDO, GAME_CONSOLE_PLAYSTATION,
        GAME_CONSOLE_XBOX, LP_RECORD, MP3, URL, EVIDEO, EDOC, EAUDIO
    }

    /**
     * Media status, simplified like a traffic light, e.g. red for "lent out, no reservation
     * possible", yellow for "reservation needed" or green for "available".
     *
     * @since 2.0.7
     */
    public enum Status {
        UNKNOWN, RED, YELLOW, GREEN
    }

}
