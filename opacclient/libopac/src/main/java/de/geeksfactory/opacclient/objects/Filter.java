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

/**
 * Object representing a filter which can be applied to a list of search
 * results. This is currently NOT implemented or used in the open source version
 * and is more like a bridge between the "Community Edition" and the
 * "Plus Edition" of the App.
 *
 * @author Raphael Michel
 * @since 2.0.6
 */
public class Filter {

    private String label;
    private String identifier;
    private List<Option> options;
    /**
     * @param label   This fitler's label
     * @param options The Options for this filter
     */
    public Filter(String label, List<Option> options) {
        super();
        this.label = label;
        this.options = options;
    }

    /**
     */
    public Filter() {
        super();
    }

    /**
     * @return This fitler's label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label This fitler's label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return The Options for this filter
     */
    public List<Option> getOptions() {
        return options;
    }

    /**
     * @param options The Options for this filter
     */
    public void setOptions(List<Option> options) {
        this.options = options;
    }

    /**
     * @return The internal identifier for the filter
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier The internal identifier for the filter
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Filter other = (Filter) obj;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Filter{" +
                "label='" + label + '\'' +
                ", identifier='" + identifier + '\'' +
                ", options=" + options +
                '}';
    }

    public class Option {
        /**
         * Object representing an option which can be applied
         *
         * @since 2.0.6
         */

        private String label;
        private String identifier;
        private int results_expected;
        private boolean is_applied;
        private boolean is_loadnext;

        /**
         * @param label      The label of the option.
         * @param identifier The internal identifier for the option
         */
        public Option(String label, String identifier) {
            super();
            this.label = label;
            this.identifier = identifier;
        }

        public Option() {
            super();
        }

        /**
         * @return The label of the option.
         */
        public String getLabel() {
            return label;
        }

        /**
         * @param label The label of the option.
         */
        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * @return The internal identifier for the option
         */
        public String getIdentifier() {
            return identifier;
        }

        /**
         * @param identifier The internal identifier for the option
         */
        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        /**
         * @return The results expected to be seen when this option is applied
         */
        public int getResults_expected() {
            return results_expected;
        }

        /**
         * @param results_expected The results expected to be seen when this option is
         *                         applied
         */
        public void setResults_expected(int results_expected) {
            this.results_expected = results_expected;
        }

        /**
         * @return Whether this filter is already applied with this option.
         */
        public boolean isApplied() {
            return is_applied;
        }

        /**
         * @param is_applied Whether this filter is already applied with this option.
         */
        public void setApplied(boolean is_applied) {
            this.is_applied = is_applied;
        }

        /**
         * @return the is_loadnext
         */
        public boolean getLoadnext() {
            return is_loadnext;
        }

        /**
         * @param is_loadnext the is_loadnext to set
         */
        public void setLoadnext(boolean is_loadnext) {
            this.is_loadnext = is_loadnext;
        }

        @Override
        public String toString() {
            return "Option{" +
                    "label='" + label + '\'' +
                    ", identifier='" + identifier + '\'' +
                    ", results_expected=" + results_expected +
                    ", is_applied=" + is_applied +
                    ", is_loadnext=" + is_loadnext +
                    '}';
        }
    }

}
