package com.lotaris.rox.client.junit;

import com.lotaris.rox.core.filters.FilterUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * The integration test framework listener is used to send the result to the server
 *
 * @author Laurent Prevost, laurent.prevost@lotaris.com
 */
public class RoxFilter extends Filter {
	/**
	 * Define the filters to apply
	 */
	private List<String> filters;
	
	/**
	 * Default constructor aims to facilitate the reflection
	 */
	public RoxFilter() {
		filters = new ArrayList<>();
	};
	
	public RoxFilter(String[] filters) {
		this.filters = Arrays.asList(filters);
	}
	
	/**
	 * @param filter Add a filter
	 */
	public void addFilter(String filter) {
		if (filters != null && filter != null && !filter.isEmpty() && !filters.contains(filter)) {
			filters.add(filter);
		}
	}
	
	@Override
	public boolean shouldRun(Description description) {
		if (!description.isTest()) {
			return true;
		}
		
		// Delegate the filtering to filter utils
		else {
			return FilterUtils.isRunnable(description.getTestClass(), description.getMethodName(), filters.toArray(new String[filters.size()]));
		}
	}

	@Override
	public String describe() {
		return "";
	}
}