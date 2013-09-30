package liquibase.ext.hibernate.database;

import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;

import org.hibernate.dialect.HSQLDialect;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.liquibase.test.Item;

import com.example.auction.AuctionItem;
import com.example.auction.Watcher;

/**
 * Tests the {@link HibernateDatabase} class.
 */
public class HibernateDatabaseTest {
    private static final String CUSTOMCONFIG_CLASS   = "org.liquibase.test.CustomConfigurationFactoryImpl";

    private static final String CUSTOMCONFIG_TO_TEST = "customconfig:" + CUSTOMCONFIG_CLASS;

    private DatabaseConnection  conn;

    private HibernateDatabase   db;

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
        conn = new JdbcConnection(new HibernateConnection("hibernate:Hibernate.cfg.xml"));
        db.setConnection(conn);
        Assert.assertNotNull(db.getConfiguration().getClassMapping(AuctionItem.class.getName()));
        Assert.assertNotNull(db.getConfiguration().getClassMapping(Watcher.class.getName()));
    }

    @Test
    public void testSpringUrlSimple() throws DatabaseException {
        conn = new JdbcConnection(new HibernateConnection("spring:spring.ctx.xml?bean=sessionFactory"));
        db.setConnection(conn);
        Assert.assertNotNull(db.getConfiguration().getClassMapping(AuctionItem.class.getName()));
        Assert.assertNotNull(db.getConfiguration().getClassMapping(Watcher.class.getName()));
    }

    @Test
    public void testCustomConfigMustHaveItemClassMapping() throws DatabaseException {
        conn = new JdbcConnection(new HibernateConnection(CUSTOMCONFIG_TO_TEST));
        db.setConnection(conn);
        Assert.assertNotNull(db.getConfiguration().getClassMapping(Item.class.getName()));
    }
    
    @Test
    public void testSpringPackageScanningMustHaveItemClassMapping() throws DatabaseException {
        conn = new JdbcConnection(new HibernateConnection("spring-package-scanning:org.liquibase.test?dialect="+HSQLDialect.class.getName()));
        db.setConnection(conn);
        Assert.assertNotNull(db.getConfiguration().getClassMapping(Item.class.getName()));
    }

}
