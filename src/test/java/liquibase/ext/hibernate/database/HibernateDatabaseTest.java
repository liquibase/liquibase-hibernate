package liquibase.ext.hibernate.database;

import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.example.auction.AuctionItem;
import com.example.auction.Watcher;

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
	public void testHibernateUrlSimple() throws DatabaseException {
		conn = new JdbcConnection(new HibernateConnection(
				"hibernate:Hibernate.cfg.xml"));
		db.setConnection(conn);
		Assert.assertNotNull(db.getConfiguration().getClassMapping(
				AuctionItem.class.getName()));
		Assert.assertNotNull(db.getConfiguration().getClassMapping(
				Watcher.class.getName()));
	}

	@Test
	public void testSpringUrlSimple() throws DatabaseException {
		conn = new JdbcConnection(new HibernateConnection(
				"spring:spring.ctx.xml?bean=sessionFactory"));
		db.setConnection(conn);
		Assert.assertNotNull(db.getConfiguration().getClassMapping(
				AuctionItem.class.getName()));
		Assert.assertNotNull(db.getConfiguration().getClassMapping(
				Watcher.class.getName()));
	}

}
