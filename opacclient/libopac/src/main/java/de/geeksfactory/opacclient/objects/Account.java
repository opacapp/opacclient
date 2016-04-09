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
 * Object representing a library account
 *
 * @author Raphael Michel
 */
public class Account {
    private long id;
    private String library;

    private String label;
    private String name;
    private String password;
    private long cached;
    private boolean password_known_valid = false;

    @Override
    public String toString() {
        return "Account [id=" + id + ", library=" + library + ", label="
                + label + ", name=" + name + ", cached=" + cached + ", " +
                "passwordValid=" + password_known_valid + "]";
    }

    /**
     * Get ID this account is stored with in <code>AccountDataStore</code>
     *
     * @return Account ID
     */
    public long getId() {
        return id;
    }

    /**
     * Set ID this account is stored with in <code>AccountDataStore</code>
     *
     * @param id Account ID
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Get library identifier this Account belongs to.
     *
     * @return Library identifier (see {@link Library#getIdent()})
     */
    public String getLibrary() {
        return library;
    }

    /**
     * Set library identifier this Account belongs to.
     *
     * @param library Library identifier (see {@link Library#getIdent()})
     */
    public void setLibrary(String library) {
        this.library = library;
    }

    /**
     * Get user-configured Account label.
     *
     * @return Label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set user-configured Account label.
     *
     * @param label Label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Get user name / identification
     *
     * @return User name or ID
     */
    public String getName() {
        return name;
    }

    /**
     * Set user name / identification
     *
     * @param name User name or ID
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set user password
     *
     * @return Password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Get user password
     *
     * @param password Password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get date of last data update
     *
     * @return Timestamp in milliseconds
     */
    public long getCached() {
        return cached;
    }

    /**
     * Set date of last data update
     *
     * @param cached Timestamp in milliseconds (use
     *               <code>System.currentTimeMillis</code>)
     */
    public void setCached(long cached) {
        this.cached = cached;
    }

    public boolean isPasswordKnownValid() {
        return password_known_valid;
    }

    public void setPasswordKnownValid(boolean password_known_valid) {
        this.password_known_valid = password_known_valid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        return id == account.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
