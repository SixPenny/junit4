package org.junit.runner;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>A <code>Description</code> describes a test which is to be run or has been run. <code>Descriptions</code> 
 * can be atomic (a single test) or compound (containing children tests). <code>Descriptions</code> are used
 * to provide feedback about the tests that are about to run (for example, the tree view
 * visible in many IDEs) or tests that have been run (for example, the failures view).</p>
 * 
 * <p><code>Descriptions</code> are implemented as a single class rather than a Composite because
 * they are entirely informational. They contain no logic aside from counting their tests.</p>
 * 
 * <p>In the past, we used the raw {@link junit.framework.TestCase}s and {@link junit.framework.TestSuite}s
 * to display the tree of tests. This was no longer viable in JUnit 4 because atomic tests no longer have 
 * a superclass below {@link Object}. We needed a way to pass a class and name together. Description 
 * emerged from this.</p>
 * 
 * @see org.junit.runner.Request
 * @see org.junit.runner.Runner
 * @since 4.0
 */
public class Description implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Pattern METHOD_AND_CLASS_NAME_PATTERN= Pattern
			.compile("(.*)\\((.*)\\)");

	/**
	 * Create a <code>Description</code> named <code>name</code>.
	 * Generally, you will add children to this <code>Description</code>.
	 * @param name the name of the <code>Description</code> 
	 * @param annotations meta-data about the test, for downstream interpreters
	 * @return a <code>Description</code> named <code>name</code>
	 */
	public static Description createSuiteDescription(String name, Annotation... annotations) {
		return new Description(name, annotations);
	}

	/**
	 * Create a <code>Description</code> named <code>name</code>.
	 * Generally, you will add children to this <code>Description</code>.
	 * @param name the name of the <code>Description</code>
	 * @param uniqueId an arbitrary object used to define uniqueness (in {@link #equals(Object)}
	 * @param annotations meta-data about the test, for downstream interpreters
	 * @return a <code>Description</code> named <code>name</code>
	 */
	public static Description createSuiteDescription(String name, Serializable uniqueId, Annotation... annotations) {
	    return new Description(name, uniqueId, annotations);
	}

	/**
	 * Create a <code>Description</code> of a single test named <code>name</code> in the 'class' named <code>className</code>.
	 * Generally, this will be a leaf <code>Description</code>.
	 * @param className the class name of the test
	 * @param name the name of the test (a method name for test annotated with {@link org.junit.Test})
	 * @param annotations meta-data about the test, for downstream interpreters
	 * @return a <code>Description</code> named <code>name</code>
	 */
	public static Description createTestDescription(String className, String name, Annotation... annotations) {
		return new Description(String.format("%s(%s)", name, className), annotations);
	}

	/**
	 * Create a <code>Description</code> of a single test named <code>name</code> in the class <code>clazz</code>.
	 * Generally, this will be a leaf <code>Description</code>.
	 * @param clazz the class of the test
	 * @param name the name of the test (a method name for test annotated with {@link org.junit.Test})
	 * @param annotations meta-data about the test, for downstream interpreters
	 * @return a <code>Description</code> named <code>name</code>
	 */
	public static Description createTestDescription(Class<?> clazz, String name, Annotation... annotations) {
		return createTestDescription(clazz.getName(), name, annotations);
	}

	/**
	 * Create a <code>Description</code> of a single test named <code>name</code> in the class <code>clazz</code>.
	 * Generally, this will be a leaf <code>Description</code>.
	 * (This remains for binary compatibility with clients of JUnit 4.3)
	 *
	 * @param name the name of the test (a method name for test annotated with {@link org.junit.Test})
	 * @return a <code>Description</code> named <code>name</code>
	 */
	public static Description createTestDescription(String className, String name, Object uniqueId) {
		return new Description(String.format("%s(%s)", name, className), uniqueId);
	}

	/**
	 * Create a <code>Description</code> named after <code>testClass</code>
	 * @param testClass A {@link Class} containing tests 
	 * @return a <code>Description</code> of <code>testClass</code>
	 */
	public static Description createSuiteDescription(Class<?> testClass) {
		return new Description(testClass.getName(), testClass.getAnnotations());
	}
	
	/**
	 * Describes a Runner which runs no tests
	 */
	public static final Description EMPTY= new Description("No Tests");
	
	/**
	 * Describes a step in the test-running mechanism that goes so wrong no
	 * other description can be used (for example, an exception thrown from a Runner's
	 * constructor
	 */
	public static final Description TEST_MECHANISM= new Description("Test mechanism");
	
	private final ArrayList<Description> fChildren= new ArrayList<Description>();
	private final String fDisplayName;
	private final Object fUniqueId;
	private final Annotation[] fAnnotations;

	private Description(String displayName, Annotation... annotations) {
		this(displayName, displayName, annotations);
	}

	private Description(String displayName, Object uniqueId, Annotation... annotations) {
		if ((displayName == null) || (displayName.length() == 0))
			throw new IllegalArgumentException(
					"The display name must not be empty.");
		if ((uniqueId == null))
			throw new IllegalArgumentException(
					"The unique id must not be null.");
		fDisplayName= displayName;
		fUniqueId= uniqueId;
		fAnnotations= annotations;
	}

	/**
	 * @return a user-understandable label
	 */
	public String getDisplayName() {
		return fDisplayName;
	}

	/**
	 * Add <code>Description</code> as a child of the receiver.
	 * @param description the soon-to-be child.
	 */
	public void addChild(Description description) {
		getChildren().add(description);
	}

	/**
	 * @return the receiver's children, if any
	 */
	public ArrayList<Description> getChildren() {
		return fChildren;
	}

	/**
	 * @return <code>true</code> if the receiver is a suite
	 */
	public boolean isSuite() {
		return !isTest();
	}

	/**
	 * @return <code>true</code> if the receiver is an atomic test
	 */
	public boolean isTest() {
		return getChildren().isEmpty();
	}

	/**
	 * @return the total number of atomic tests in the receiver
	 */
	public int testCount() {
		if (isTest())
			return 1;
		int result= 0;
		for (Description child : getChildren())
			result+= child.testCount();
		return result;
	}

	@Override
	public int hashCode() {
		return fUniqueId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Description))
			return false;
		Description d = (Description) obj;
		return fUniqueId.equals(d.fUniqueId);
	}
	
	@Override
	public String toString() {
		return getDisplayName();
	}

	/**
	 * @return true if this is a description of a Runner that runs no tests
	 */
	public boolean isEmpty() {
		return equals(EMPTY);
	}

	/**
	 * @return a copy of this description, with no children (on the assumption that some of the
	 * children will be added back)
	 */
	public Description childlessCopy() {
		return new Description(fDisplayName, fAnnotations);
	}

	/**
	 * @return the annotation of type annotationType that is attached to this description node,
	 * or null if none exists
	 */
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		for (Annotation each : fAnnotations)
			if (each.annotationType().equals(annotationType))
				return annotationType.cast(each);
		return null;
	}

	/**
	 * @return all of the annotations attached to this description node
	 */
	public Collection<Annotation> getAnnotations() {
		return Arrays.asList(fAnnotations);
	}

	/**
	 * @return If this describes a method invocation, 
	 * the class of the test instance.
	 */
	public Class<?> getTestClass() {
		String name= getClassName();
		if (name == null)
			return null;
		try {
			return Class.forName(name, false, getClass().getClassLoader());
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	/**
	 * @return If this describes a method invocation, 
	 * the name of the class of the test instance
	 */
	public String getClassName() {
		return methodAndClassNamePatternGroupOrDefault(2, toString());
	}
	
	/**
	 * @return If this describes a method invocation, 
	 * the name of the method (or null if not)
	 */
	public String getMethodName() {
		return methodAndClassNamePatternGroupOrDefault(1, null);
	}

	private String methodAndClassNamePatternGroupOrDefault(int group,
			String defaultString) {
		Matcher matcher= METHOD_AND_CLASS_NAME_PATTERN.matcher(toString());
		return matcher.matches() ? matcher.group(group) : defaultString;
	}
}