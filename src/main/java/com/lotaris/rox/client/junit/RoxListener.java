package com.lotaris.rox.client.junit;

import com.lotaris.rox.annotations.RoxableTest;
import com.lotaris.rox.annotations.RoxableTestClass;
import com.lotaris.rox.common.config.RoxRuntimeException;
import com.lotaris.rox.common.model.v1.ModelFactory;
import com.lotaris.rox.common.model.v1.Payload;
import com.lotaris.rox.common.model.v1.Test;
import com.lotaris.rox.core.connector.Connector;
import com.lotaris.rox.core.storage.FileStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The test unit listener is used to send the result to the ROX server
 *
 * @author Laurent Prévost, laurent.prevost@lotaris.com
 */
public class RoxListener extends AbstractRoxListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(RoxListener.class);
	/**
	 * Store the list of the tests executed
	 */
	private List<Test> results = new ArrayList<Test>();
	/**
	 * Store the test that fail to handle correctly the difference between test failures and test
	 * success in the testFinished method.
	 */
	private Set<String> testFailures = new HashSet<String>();

	public RoxListener() {
	}

	public RoxListener(String category) {
		super(category);
	}

	@Override
	public void testRunFinished(Result result) throws Exception {

		// Ensure there is nothing to do when ROX is disabled
		if (configuration.isDisabled()) {
			return;
		}

		if (!results.isEmpty()) {

			try {
				publishTestPayload();
			} catch (RoxRuntimeException rre) {
				LOGGER.warn("Could not publish or save test payload", rre);
			}
		}
	}

	private void publishTestPayload() throws IOException {

		if (configuration.isPublish() || configuration.isSave()) {

			long runEndedDate = System.currentTimeMillis();

			Payload payload = ModelFactory.createPayload(
					ModelFactory.createTestRun(
					configuration.getProjectApiId(),
					configuration.getProjectVersion(),
					runEndedDate,
					runEndedDate - runStartedDate,
					configuration.getGroup(),
					configuration.getUid(getCategory(null, null), configuration.getProjectApiId(), configuration.getProjectVersion()),
					results));

			if (configuration.isSave()) {
				new FileStore(configuration).save(payload);
			}

			if (configuration.isPublish()) {
				new Connector(configuration).send(payload);
			}
		}
	}

	@Override
	public void testStarted(Description description) throws Exception {
		super.testStarted(description);

		// Ensure there is nothing to do when ROX is disabled
		if (configuration.isDisabled()) {
			return;
		}

		RoxableTest annotation = getMethodAnnotation(description);

		if (annotation != null) {
			if (annotation.key() != null || !annotation.key().isEmpty()) {
				testStartDates.put(annotation.key(), System.currentTimeMillis());
			} else {
				LOGGER.warn("@{} annotation is present but missconfigured. The key is missing", RoxableTest.class.getSimpleName());
			}
		} else {
			LOGGER.warn("@{} annotation is missing on method name : {}.{}", RoxableTest.class.getSimpleName(), description.getClassName(), description.getMethodName());
		}
	}

	@Override
	public void testFinished(Description description) throws Exception {
		super.testFinished(description);

		// Ensure there is nothing to do when ROX is disabled
		if (configuration.isDisabled()) {
			return;
		}

		RoxableTest methodAnnotation = getMethodAnnotation(description);

		if (methodAnnotation != null && !methodAnnotation.key().isEmpty() && !testFailures.contains(methodAnnotation.key())) {
			// Create a test result
			Test testResult = createTest(description, methodAnnotation, getClassAnnotation(description), true, null);

			results.add(testResult);
		}
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		super.testFailure(failure);

		// Ensure there is nothing to do when ROX is disabled
		if (configuration.isDisabled()) {
			return;
		}

		RoxableTest methodAnnotation = getMethodAnnotation(failure.getDescription());
		RoxableTestClass cAnnotation = getClassAnnotation(failure.getDescription());

		if (methodAnnotation != null && !methodAnnotation.key().isEmpty()) {
			testFailures.add(methodAnnotation.key());

			// Create the test result
			Test testResult = createTest(failure.getDescription(), methodAnnotation, cAnnotation, false, createAndlogStackTrace(failure));

			results.add(testResult);
		}
	}
}