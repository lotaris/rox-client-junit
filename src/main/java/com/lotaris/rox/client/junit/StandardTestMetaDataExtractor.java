package com.lotaris.rox.client.junit;

import com.lotaris.rox.common.utils.MetaDataBuilder;
import org.junit.runner.Description;

/**
 * Simple implementation of a data extractor to get the method, class and package
 * names of a test.
 * 
 * @author Laurent Prevost <laurent.prevost@lotaris.com>
 */
public class StandardTestMetaDataExtractor implements RoxTestMetaDataExtratctor {

	@Override
	public void before(Description description) {}

	@Override
	public void after(Description descrption) {}

	@Override
	public MetaDataBuilder extract(Description description) {
		MetaDataBuilder data = new MetaDataBuilder();

		data
			.add("java.package", description.getTestClass().getPackage().getName())
			.add("java.class", description.getClassName())
			.add("java.method", description.getMethodName());

		return data;
	}
}
