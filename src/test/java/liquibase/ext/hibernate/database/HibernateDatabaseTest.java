package liquibase.ext.hibernate.database;

import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link HibernateDatabase} class.
 */
public class HibernateDatabaseTest {
	private DatabaseConnection conn;
	private HibernateDatabase db;

	@Before
	public void setUp() throws Exception {
		db = new HibernateDatabase();
	}

	@After
	public void tearDown() throws Exception {
		db.close();
	}

	@Test
	public void testHibernateUrlSimple() {
		conn = new JdbcConnection(new HibernateConnection(
				"hibernate:hibernate/Hibernate.cfg.xml"));
		db.setConnection(conn);

		// FIXME
		// Configuration config = db.createConfiguration();
	}

	@Test
	public void testSpringUrlSimple() {
		conn = new JdbcConnection(
				new HibernateConnection(
						"spring:src/test/resources/hibernate/spring.ctx.xml?bean=sessionFactory"));
		db.setConnection(conn);
		// FIXME
		// Configuration config = db.createConfiguration();
	}

}
