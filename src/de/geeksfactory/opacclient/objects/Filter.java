package de.geeksfactory.opacclient.objects;

import java.util.List;

/**
 * Object representing a filter which can be applied to a list of search results
 * 
 * @since 2.0.6
 * 
 * @author Raphael Michel
 */
public class Filter {

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

		/**
		 * @return The label of the option.
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * @param label
		 *            The label of the option.
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
		 * @param identifier
		 *            The internal identifier for the option
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
		 * @param results_expected
		 *            The results expected to be seen when this option is
		 *            applied
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
		 * @param is_applied
		 *            Whether this filter is already applied with this option.
		 */
		public void setApplied(boolean is_applied) {
			this.is_applied = is_applied;
		}

		/**
		 * @param label
		 *            The label of the option.
		 * @param identifier
		 *            The internal identifier for the option
		 */
		public Option(String label, String identifier) {
			super();
			this.label = label;
			this.identifier = identifier;
		}

		public Option() {
			super();
		}
	}

	private String label;
	private String identifier;
	private List<Option> options;

	/**
	 * @return This fitler's label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label
	 *            This fitler's label
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
	 * @param options
	 *            The Options for this filter
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
	 * @param identifier
	 *            The internal identifier for the filter
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * @param label
	 *            This fitler's label
	 * @param options
	 *            The Options for this filter
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

}
