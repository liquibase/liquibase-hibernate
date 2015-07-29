package liquibase.ext.hibernate.database;

import com.example.ejb3.auction.Bid;
import com.example.ejb3.auction.BuyNow;
import com.example.pojo.auction.AuctionItem;
import com.example.pojo.auction.Watcher;
import liquibase.CatalogAndSchema;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import org.hibernate.dialect.HSQLDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class HibernateSpringDatabaseTest {

    private DatabaseConnection conn;
    private HibernateSpringDatabase db;

    @Before
    public void setUp() throws Exception {
        db = new HibernateSpringDatabase();
    }

    @After
    public void tearDown() throws Exception {
        db.close();
    }

    @Test
    public void testSpringUrlSimple() throws DatabaseException {
        conn = new JdbcConnection(new HibernateConnection("hibernate:spring:spring.ctx.xml?bean=sessionFactory"));
        db.setConnection(conn);
        assertNotNull(db.getConfiguration().getClassMapping(AuctionItem.class.getName()));
        assertNotNull(db.getConfiguration().getClassMapping(Watcher.class.getName()));
    }


    @Test
    public void testSpringPackageScanningMustHaveItemClassMapping() throws DatabaseException {
        conn = new JdbcConnection(new HibernateConnection("hibernate:spring:com.example.ejb3.auction?dialect=" + HSQLDialect.class.getName()));
        db.setConnection(conn);
        assertNotNull(db.getConfiguration().getClassMapping(Bid.class.getName()));
        assertNotNull(db.getConfiguration().getClassMapping(BuyNow.class.getName()));
    }

    @Test
    public void simpleSpringUrl() throws Exception {
        String url = "hibernate:spring:spring.ctx.xml?bean=sessionFactory";
        Database database = CommandLineUtils.createDatabaseObject(this.getClass().getClassLoader(), url, null, null, null, null, null, false, false, null, null, null, null, null, null, null);

        assertNotNull(database);

        DatabaseSnapshot snapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(CatalogAndSchema.DEFAULT, database, new SnapshotControl(database));

        HibernateClassicDatabaseTest.assertPojoHibernateMapped(snapshot);
    }

    @Test
    public void simpleSpringScanningUrl() throws Exception {
        String url = "hibernate:spring:com.example.ejb3.auction?dialect=" + HSQLDialect.class.getName();
        Database database = CommandLineUtils.createDatabaseObject(this.getClass().getClassLoader(), url, null, null, null, null, null, false, false, null, null, null, null, null, null, null);

        assertNotNull(database);

        DatabaseSnapshot snapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(CatalogAndSchema.DEFAULT, database, new SnapshotControl(database));

        HibernateEjb3DatabaseTest.assertEjb3HibernateMapped(snapshot);
    }

}
