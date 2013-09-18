package liquibase.ext.hibernate.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link ConfigLocator} class.
 */
public class ConfigLocatorTest {
    private final String FILE_PATH = "/path/to/file.ext";

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testHibernateUrlSimple() {
	ConfigLocator locator = new ConfigLocator("hibernate:" + FILE_PATH);
	assertEquals(ConfigType.HIBERNATE, locator.getType());
	assertEquals(FILE_PATH, locator.getPath());
	assertEquals(0, locator.getProperties().size());
    }

    @Test
    public void testHibernateUrlWithProperties() {
	ConfigLocator locator = new ConfigLocator("hibernate:" + FILE_PATH + "?foo=bar&name=John+Doe");
	assertEquals(ConfigType.HIBERNATE, locator.getType());
	assertEquals(FILE_PATH, locator.getPath());
	assertEquals(2, locator.getProperties().size());
	assertEquals("bar", locator.getProperties().getProperty("foo", null));
	assertEquals("John Doe", locator.getProperties().getProperty("name", null));
    }

    @Test
    public void testEjb3UrlSimple() {
	ConfigLocator locator = new ConfigLocator("persistence:" + FILE_PATH);
	assertEquals(ConfigType.EJB3, locator.getType());
	assertEquals(FILE_PATH, locator.getPath());
	assertEquals(0, locator.getProperties().size());
    }

    @Test
    public void testEjb3UrlWithProperties() {
	ConfigLocator locator = new ConfigLocator("persistence:" + FILE_PATH + "?foo=bar&name=John+Doe");
	assertEquals(ConfigType.EJB3, locator.getType());
	assertEquals(FILE_PATH, locator.getPath());
	assertEquals(2, locator.getProperties().size());
	assertEquals("bar", locator.getProperties().getProperty("foo", null));
	assertEquals("John Doe", locator.getProperties().getProperty("name", null));
    }

    @Test
    public void testSpringUrlSimple() {
	ConfigLocator locator = new ConfigLocator("spring:" + FILE_PATH);
	assertEquals(ConfigType.SPRING, locator.getType());
	assertEquals(FILE_PATH, locator.getPath());
	assertEquals(0, locator.getProperties().size());
    }

    @Test
    public void testSpringUrlWithProperties() {
	ConfigLocator locator = new ConfigLocator("spring:" + FILE_PATH + "?foo=bar&name=John+Doe");
	assertEquals(ConfigType.SPRING, locator.getType());
	assertEquals(FILE_PATH, locator.getPath());
	assertEquals(2, locator.getProperties().size());
	assertEquals("bar", locator.getProperties().getProperty("foo", null));
	assertEquals("John Doe", locator.getProperties().getProperty("name", null));
    }

    @Test(expected = IllegalStateException.class)
    public void testBadUrl() {
	new ConfigLocator("bad:" + FILE_PATH);
    }

    @Test
    public void testEquals() {
	ConfigLocator locator = new ConfigLocator("hibernate:" + FILE_PATH);
	assertTrue(locator.equals(new ConfigLocator("hibernate:" + FILE_PATH)));
	assertFalse(locator.equals(new ConfigLocator("spring:" + FILE_PATH)));
    }
}
