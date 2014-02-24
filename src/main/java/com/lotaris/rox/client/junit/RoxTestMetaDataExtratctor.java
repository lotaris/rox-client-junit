package com.lotaris.rox.client.junit;

import com.lotaris.rox.common.utils.MetaDataBuilder;
import org.junit.runner.Description;

/**
 * Extracts meta data from a description to populate the meta
 * data of a test result.
 * 
 * @author Laurent Prevost <laurent.prevost@lotaris.com>
 */
public interface RoxTestMetaDataExtratctor {
	/**
	 * Do some processing before a test is performed
	 * 
	 * @param description The description of the test
	 */
	void before(Description description);
	
	/**
	 * Do some processing after a test is performed
	 * 
	 * @param descrption The description of the test
	 */
	void after(Description descrption);
	
	/**
	 * Extract meta data from a test
	 * 
	 * @param description Description of the test
	 * @return The meta data to decorate the test result
	 */
	MetaDataBuilder extract(Description description);
}
