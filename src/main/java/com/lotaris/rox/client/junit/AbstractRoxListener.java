package com.lotaris.rox.client.junit;

import com.lotaris.rox.annotations.RoxableTest;
import com.lotaris.rox.annotations.RoxableTestClass;
import com.lotaris.rox.annotations.TestFlag;
import com.lotaris.rox.common.config.Configuration;
import com.lotaris.rox.common.model.v1.ModelFactory;
import com.lotaris.rox.common.model.v1.Test;
import com.lotaris.rox.common.utils.Inflector;
import com.lotaris.rox.common.utils.MetaDataBuilder;
import com.lotaris.rox.utils.CollectionHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared code to create ROX related listeners
 *
 * @author Laurent Prévost, laurent.prevost@lotaris.com
 */
public abstract class AbstractRoxListener extends RunListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRoxListener.class);
	
	/**
	 * Rox configuration
	 */
	protected static final Configuration configuration = Configuration.getInstance();
	
	/**
	 * Default category when none is specified
	 */
	private static final String DEFAULT_CATEGORY	= "JUnit";
	private static final String DEFAULT_TAG				= "unit";
	
	/**
	 * Store the date when the run started
	 */
	protected Long runStartedDate;

	/**
	 * Store the start date of a test to measure the approximative execution
	 * time of a each test.
	 */
	protected Map<String, Long> testStartDates = new HashMap<>();

	/**
	 * Define if all the stack traces must be printed/written
	 */
	private boolean fullStackTraces = false;
	
	/**
	 * Define the category of the test that are running. Will
	 * be use donly if no other is specified.
	 */
	private String category;
	
	/**
	 * List of meta data extractors
	 */
	private final List<RoxTestMetaDataExtratctor> extractors = new ArrayList<>();
	
	/**
	 * Constructor
	 */
	public AbstractRoxListener() {
		extractors.add(new StandardTestMetaDataExtractor());
	}
	
	/**
	 * Constructor
	 * 
	 * @param category The test category
	 */
	public AbstractRoxListener(String category) {
		this();
		this.category = category;
	}

	@Override
	public void testStarted(Description description) throws Exception {
		super.testStarted(description);
		
		for (RoxTestMetaDataExtratctor extractor : extractors) {
			extractor.before(description);
		}
	}

	@Override
	public void testFinished(Description description) throws Exception {
		super.testFinished(description);

		for (RoxTestMetaDataExtratctor extractor : extractors) {
			extractor.after(description);
		}
	}
	
	/**
	 * @param fullStackTraces The full stack trace mode
	 */
	public void setFullStackTraces(Boolean fullStackTraces) {
		this.fullStackTraces = fullStackTraces;
	}
	
	/**
	 * Add an extractor to the list of extractors
	 * 
	 * @param extractor Extractor to add
	 */
	public void addExctractor(RoxTestMetaDataExtratctor extractor) {
		extractors.add(extractor);
	}
	
	@Override
	public void testRunStarted(Description description) throws Exception {
		super.testRunStarted(description);
		runStartedDate = System.currentTimeMillis();
	}
	
	/**
	 * Try to retrieve the {@link RoxableTest} annotation of the test method
	 * 
	 * @param description The representation of the test
	 * @return The annotation found, or null if not found
	 * @throws NoSuchMethodException 
	 */
	protected RoxableTest getMethodAnnotation(Description description) throws NoSuchMethodException {
		return description.getTestClass().getMethod(description.getMethodName()).getAnnotation(RoxableTest.class);
	}
	
	/**
	 * Try to retrieve the {@link RoxableTestClass} annotation of the test class
	 * 
	 * @param description The representation of the test
	 * @return The annotation found, or null if not found
	 */
	protected RoxableTestClass getClassAnnotation(Description description) {
		return description.getTestClass().getAnnotation(RoxableTestClass.class);
	}

	/**
	 * Create a test based on the different information gathered from class, method and description
	 * 
	 * @param description jUnit test description
	 * @param mAnnotation Method annotation
	 * @param cAnnotation Class annotation
	 * @param passed Test passing or not
	 * @param message Message associated to the test result
	 * @return The test created from all the data available
	 */
	protected Test createTest(Description description, RoxableTest mAnnotation, RoxableTestClass cAnnotation, boolean passed, String message) {
		MetaDataBuilder data = new MetaDataBuilder();
		
		for (RoxTestMetaDataExtratctor extractor : extractors) {
			data.add(extractor.extract(description));
		}
		
		return ModelFactory.createTest(
			!mAnnotation.key().isEmpty() ? mAnnotation.key() : getTechnicalName(description),
			getName(description, mAnnotation),
			getCategory(cAnnotation, mAnnotation),
			System.currentTimeMillis(),
			System.currentTimeMillis() - testStartDates.get(getTechnicalName(description)),
			message,
			passed,
			TestFlag.flagsValue(Arrays.asList(mAnnotation.flags())),
			getTags(mAnnotation, cAnnotation),
			getTickets(mAnnotation, cAnnotation),
			data.toMetaData()
		);
	}
	
	/**
	 * Retrieve a name from a test
	 * 
	 * @param description The description of the test
	 * @param mAnnotation The method annotation
	 * @return The name retrieved
	 */
	private String getName(Description description, RoxableTest mAnnotation) {
		if (mAnnotation == null || mAnnotation.name() == null || mAnnotation.name().isEmpty()) {
			return Inflector.getHumanName(description.getMethodName());
		}
		else {
			return mAnnotation.name();
		}
	} 
	
	/**
	 * Retrieve the category to apply to the test
	 * @param classAnnotation The roxable class annotation to get the override category
	 * @param methodAnnotation The roxable annotation to get the override category
	 * @return The category found
	 */
	protected String getCategory(RoxableTestClass classAnnotation, RoxableTest methodAnnotation) {
		if (methodAnnotation != null && methodAnnotation.category() != null && !methodAnnotation.category().isEmpty()) {
			return methodAnnotation.category();
		}
		else if (classAnnotation != null && classAnnotation.category() != null && !classAnnotation.category().isEmpty()) {
			return classAnnotation.category();
		}
		else if (configuration.getCategory() != null && !configuration.getCategory().isEmpty()) {
			return configuration.getCategory();
		}
		else if (category != null) {
			return category;
		}
		else {
			return DEFAULT_CATEGORY;
		}
	}
	
	/**
	 * Compute the list of tags associated for a test
	 * 
	 * @param methodAnnotation The method annotation to get info
	 * @param classAnnotation The class annotation to get info
	 * @return The tags associated to the test
	 */
	private Set<String> getTags(RoxableTest methodAnnotation, RoxableTestClass classAnnotation) {
		Set<String> tags = CollectionHelper.getTags(configuration.getTags(), methodAnnotation, classAnnotation);
		
		if (!tags.contains(DEFAULT_TAG)) {
			tags.add(DEFAULT_TAG);
		}
		
		return tags;
	}

	/**
	 * Compute the list of tickets associated for a test
	 * 
	 * @param methodAnnotation The method annotation to get info
	 * @param classAnnotation The class annotation to get info
	 * @return The tickets associated to the test
	 */
	private Set<String> getTickets(RoxableTest methodAnnotation, RoxableTestClass classAnnotation) {
		return CollectionHelper.getTickets(configuration.getTickets(), methodAnnotation, classAnnotation);
	}
	
	/**
	 * Build a stack trace string
	 * 
	 * @param failure The failure to get the exceptions and so on
	 * @return The stack trace stringified
	 */
	protected String createAndlogStackTrace(Failure failure) {
		StringBuilder sb = new StringBuilder();

		if (failure.getMessage() != null && !failure.getMessage().isEmpty()) {
			sb.append("Failure message: ").append(failure.getMessage());
		}
		
		if (failure.getException() != null) {
			sb.append("\n\n");
			sb.append(failure.getException().getClass().getCanonicalName()).append(": ").append(failure.getMessage()).append("\n");
		
			for (StackTraceElement ste : failure.getException().getStackTrace()) {
				sb.append("\tat ").append(ste.getClassName()).append(".").append(ste.getMethodName()).
					append("(").append(ste.getFileName()).append(":").append(ste.getLineNumber()).append(")\n");
				
				if (!fullStackTraces && ste.getClassName().equals(failure.getDescription().getClassName())) {
					sb.append("\t...\n");
					break;
				}
			}

			if (fullStackTraces && failure.getException().getCause() != null) {
				sb.append("Cause: ").append(failure.getException().getCause().getMessage()).append("\n");
				
				for (StackTraceElement ste : failure.getException().getCause().getStackTrace()) {
					sb.append("\tat ").append(ste.getClassName()).append(".").append(ste.getMethodName()).
						append("(").append(ste.getFileName()).append(":").append(ste.getLineNumber()).append(")\n");
				}
			}
			
			LOGGER.info("\n{}\n{}", failure.getTestHeader(), sb.toString());
		}
		
		return sb.toString();
	}
	
	/**
	 * Build the technical name
	 * 
	 * @param description The description to retrieve the unique name of a test
	 * @return The technical name
	 */
	protected String getTechnicalName(Description description) {
		return description.getClassName() + "." + description.getMethodName();
	}
}