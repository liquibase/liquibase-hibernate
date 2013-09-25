package liquibase.ext.hibernate.database;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.Properties;

/**
 * This class provides the information required to locate a hibernate
 * configuration based on an initial 'database URL'.
 */
public class ConfigLocator {
    private String url;
    private ConfigType type;
    private String path;
    private Properties properties;

    public ConfigLocator(String url) {
	this.url = url;
	type = ConfigType.forUrl(url);
	if (type == null)
	    throw new IllegalStateException("Unsupported URL format: " + url);

	// Trim the prefix off the URL for the path
	path = url.substring(type.getPrefix().length() + 1);

	// Check if there is a parameter/query string value.
	properties = new Properties();

	int queryIndex = path.indexOf('?');
	if (queryIndex >= 0) {
	    // Convert the query string into properties
	    loadProperties(path.substring(queryIndex + 1));
	    // Remove the query string
	    path = path.substring(0, queryIndex);
	}
    }

    private void loadProperties(String query) {
	query = query.replaceAll("&", System.getProperty("line.separator"));
	try {
	    query = URLDecoder.decode(query, "UTF-8");
	    properties.load(new StringReader(query));
	} catch (IOException ioe) {
	    throw new IllegalStateException("Failed to read properties from url", ioe);
	}
    }

    /**
     * Returns the original URL used to create the locator.
     * 
     * @return The original URL.
     */
    public String getUrl() {
	return url;
    }

    /**
     * The {@link ConfigType} for this locator.
     * 
     * @return The type.
     */
    public ConfigType getType() {
	return type;
    }

    /**
     * The path to the file provided by the URL.
     * 
     * @return The file path.
     */
    public String getPath() {
	return path;
    }

    /**
     * The set of properties provided by the URL. Eg:
     * <p/>
     * <code>hibernate:/path/to/hibernate.cfg.xml?foo=bar</code>
     * <p/>
     * This will have a property called 'foo' with a value of 'bar'.
     * 
     * @return The property set.
     */
    public Properties getProperties() {
	return properties;
    }

    /**
     * @param obj
     *            the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj argument;
     *         {@code false} otherwise.
     * @see #hashCode()
     * @see java.util.HashMap
     */
    @Override
    public boolean equals(Object obj) {
	return ((ConfigLocator) obj).url.equals(url);
    }

    /**
     * Returns a hash code value for the object. This method is supported for
     * the benefit of hash tables such as those provided by
     * {@link java.util.HashMap}.
     * <p/>
     * The general contract of {@code hashCode} is:
     * <ul>
     * <li>Whenever it is invoked on the same object more than once during an
     * execution of a Java application, the {@code hashCode} method must
     * consistently return the same integer, provided no information used in
     * {@code equals} comparisons on the object is modified. This integer need
     * not remain consistent from one execution of an application to another
     * execution of the same application.
     * <li>If two objects are equal according to the {@code equals(Object)}
     * method, then calling the {@code hashCode} method on each of the two
     * objects must produce the same integer result.
     * <li>It is <em>not</em> required that if two objects are unequal according
     * to the {@link Object#equals(Object)} method, then calling the
     * {@code hashCode} method on each of the two objects must produce distinct
     * integer results. However, the programmer should be aware that producing
     * distinct integer results for unequal objects may improve the performance
     * of hash tables.
     * </ul>
     * <p/>
     * As much as is reasonably practical, the hashCode method defined by class
     * {@code Object} does return distinct integers for distinct objects. (This
     * is typically implemented by converting the internal address of the object
     * into an integer, but this implementation technique is not required by the
     * Java<font size="-2"><sup>TM</sup></font> programming language.)
     * 
     * @return a hash code value for this object.
     * @see Object#equals(Object)
     * @see System#identityHashCode
     */
    @Override
    public int hashCode() {
	return 7 + url.hashCode();
    }

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that "textually represents" this
     * object. The result should be a concise but informative representation
     * that is easy for a person to read. It is recommended that all subclasses
     * override this method.
     * <p/>
     * The {@code toString} method for class {@code Object} returns a string
     * consisting of the name of the class of which the object is an instance,
     * the at-sign character `{@code @}', and the unsigned hexadecimal
     * representation of the hash code of the object. In other words, this
     * method returns a string equal to the value of: <blockquote>
     * 
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre>
     * 
     * </blockquote>
     * 
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
	return url;
    }
}
