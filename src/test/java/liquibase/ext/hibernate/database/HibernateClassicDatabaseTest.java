package liquibase.ext.hibernate.database;

import liquibase.CatalogAndSchema;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.*;


public class HibernateClassicDatabaseTest {

    private static final String CUSTOMCONFIG_CLASS = "com.example.customconfig.CustomClassicConfigurationFactoryImpl";

    private DatabaseConnection conn;
    private HibernateClassicDatabase db;

    @Before
    public void setUp() throws Exception {
        db = new HibernateClassicDatabase();
    }

    @After
    public void tearDown() throws Exception {
        db.close();
    }

//    @Test
//    public void runMain() throws Exception {
//        Main.main(new String[]{
//                "--url=hibernate:classic:com/example/pojo/Hibernate.cfg.xml",
//                "--referenceUrl=jdbc:mysql://vagrant/lbcat", "--referenceUsername=lbuser",
//                "--referencePassword=lbuser",
//                "--logLevel=debug",
//                "diffChangeLog"
//        });
//    }

//    @Test
//    public void testHibernateUrlSimple() throws DatabaseException {
//        conn = new JdbcConnection(new HibernateConnection("hibernate:classic:com/example/pojo/Hibernate.cfg.xml"));
//        db.setConnection(conn);
//        assertNotNull(db.getConfiguration().getClassMapping(AuctionItem.class.getName()));
//        assertNotNull(db.getConfiguration().getClassMapping(Watcher.class.getName()));
//    }
//
//
//    @Test
//    public void testCustomConfigMustHaveItemClassMapping() throws DatabaseException {
//        conn = new JdbcConnection(new HibernateConnection("hibernate:classic:" + CUSTOMCONFIG_CLASS));
//        db.setConnection(conn);
//        assertNotNull(db.getConfiguration().getClassMapping(Item.class.getName()));
//    }

    @Test
    public void simpleHibernateUrl() throws Exception {
        String url = "hibernate:classic:com/example/pojo/Hibernate.cfg.xml";
        Database database = CommandLineUtils.createDatabaseObject(new ClassLoaderResourceAccessor(this.getClass().getClassLoader()), url, null, null, null, null, null, false, false, null, null, null, null, null, null, null);

        assertNotNull(database);

        DatabaseSnapshot snapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(CatalogAndSchema.DEFAULT, database, new SnapshotControl(database));

        assertPojoHibernateMapped(snapshot);
    }

    @Test
    public void nationalizedCharactersHibernateUrl() throws Exception {
        String url = "hibernate:classic:com/example/pojo/Hibernate.cfg.xml?hibernate.use_nationalized_character_data=true";
        Database database = CommandLineUtils.createDatabaseObject(new ClassLoaderResourceAccessor(this.getClass().getClassLoader()), url, null, null, null, null, null, false, false, null, null, null, null, null, null, null);

        assertNotNull(database);

        DatabaseSnapshot snapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(CatalogAndSchema.DEFAULT, database, new SnapshotControl(database));

        assertPojoHibernateMapped(snapshot);
        Table watcherTable = (Table) snapshot.get(new Table().setName("watcher").setSchema(new Schema()));
        assertEquals("varchar", watcherTable.getColumn("name").getType().getTypeName());
    }

    public static void assertPojoHibernateMapped(DatabaseSnapshot snapshot) {
        assertThat(snapshot.get(Table.class), containsInAnyOrder(
                hasProperty("name", is("Bid")),
                hasProperty("name", is("Watcher")),
                hasProperty("name", is("AuctionUser")),
                hasProperty("name", is("AuctionItem"))));


        Table bidTable = (Table) snapshot.get(new Table().setName("bid").setSchema(new Schema()));
        Table auctionItemTable = (Table) snapshot.get(new Table().setName("auctionitem").setSchema(new Schema()));

        assertTrue(bidTable.getColumn("id").isAutoIncrement());
        assertFalse(bidTable.getColumn("isBuyNow").isAutoIncrement());
        assertEquals("Y if a \"buy now\", N if a regular bid.", bidTable.getColumn("isBuyNow").getRemarks());
        assertFalse(bidTable.getColumn("datetime").isNullable());
        assertTrue(auctionItemTable.getColumn("condition").isNullable());

        assertThat(bidTable.getColumns(), containsInAnyOrder(
                hasProperty("name", is("id")),
                hasProperty("name", is("isBuyNow")),
                hasProperty("name", is("item")),
                hasProperty("name", is("amount")),
                hasProperty("name", is("datetime")),
                hasProperty("name", is("bidder"))
        ));

        assertThat(bidTable.getPrimaryKey().getColumnNames(), is("id"));

        assertThat(bidTable.getOutgoingForeignKeys(), containsInAnyOrder(
                allOf(
                        hasProperty("primaryKeyColumns", hasToString("[HIBERNATE.AuctionItem.id]")),
                        hasProperty("foreignKeyColumns", hasToString("[HIBERNATE.Bid.item]")),
                        hasProperty("primaryKeyTable", hasProperty("name", is("AuctionItem")))
                ),
                allOf(
                        hasProperty("primaryKeyColumns", hasToString("[HIBERNATE.AuctionUser.id]")),
                        hasProperty("foreignKeyColumns", hasToString("[HIBERNATE.Bid.bidder]")),
                        hasProperty("primaryKeyTable", hasProperty("name", is("AuctionUser")))
                )
        ));
    }
}
