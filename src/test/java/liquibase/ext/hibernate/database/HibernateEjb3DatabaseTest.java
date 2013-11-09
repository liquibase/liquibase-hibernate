package liquibase.ext.hibernate.database;

import liquibase.CatalogAndSchema;
import liquibase.database.Database;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class HibernateEjb3DatabaseTest {

    @Test
    public void simpleEjb3Url() throws Exception {
        String url = "hibernate:ejb3:auction";
        Database database = CommandLineUtils.createDatabaseObject(this.getClass().getClassLoader(), url, null, null, null, null, null, false, false, null, null, null, null);

        assertNotNull(database);

        DatabaseSnapshot snapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(CatalogAndSchema.DEFAULT, database, new SnapshotControl(database));

        assertEjb3HibernateMapped(snapshot);
    }

    public static void assertEjb3HibernateMapped(DatabaseSnapshot snapshot) {
        assertThat(snapshot.get(Table.class), containsInAnyOrder(
                hasProperty("name", is("Bid")),
                hasProperty("name", is("Watcher")),
                hasProperty("name", is("User")),
                hasProperty("name", is("AuctionInfo")),
                hasProperty("name", is("AuctionItem")),
                hasProperty("name", is("Item"))));


        Table bidTable = (Table) snapshot.get(new Table().setName("bid").setSchema(new Schema()));
        Table auctionItemTable = (Table) snapshot.get(new Table().setName("auctionitem").setSchema(new Schema()));

        assertThat(bidTable.getColumns(), containsInAnyOrder(
                hasProperty("name", is("id")),
                hasProperty("name", is("buyNow")),
                hasProperty("name", is("item_id")),
                hasProperty("name", is("amount")),
                hasProperty("name", is("datetime")),
                hasProperty("name", is("bidder_id")),
                hasProperty("name", is("DTYPE"))
        ));

        assertTrue(bidTable.getColumn("id").isAutoIncrement());
        assertFalse(bidTable.getColumn("buyNow").isAutoIncrement());
        assertFalse(bidTable.getColumn("datetime").isNullable());
        assertTrue(auctionItemTable.getColumn("ends").isNullable());

        assertThat(bidTable.getPrimaryKey().getColumnNames(), is("id"));

        assertThat(bidTable.getOutgoingForeignKeys(), containsInAnyOrder(
                allOf(
                        hasProperty("primaryKeyColumns", is("id")),
                        hasProperty("foreignKeyColumns", is("item_id")),
                        hasProperty("primaryKeyTable", hasProperty("name", is("AuctionItem")))
                ),
                allOf(
                        hasProperty("primaryKeyColumns", is("id")),
                        hasProperty("foreignKeyColumns", is("bidder_id")),
                        hasProperty("primaryKeyTable", hasProperty("name", is("User")))
                )
        ));
    }

    @Test
    public void ejb3UrlWithNamingStrategy() throws Exception {
        String url = "hibernate:ejb3:auction?hibernate.ejb.naming_strategy=org.hibernate.cfg.ImprovedNamingStrategy";
        Database database = CommandLineUtils.createDatabaseObject(this.getClass().getClassLoader(), url, null, null, null, null, null, false, false, null, null, null, null);

        assertNotNull(database);

        DatabaseSnapshot snapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(CatalogAndSchema.DEFAULT, database, new SnapshotControl(database));

        assertThat(snapshot.get(Table.class), containsInAnyOrder(
                hasProperty("name", is("bid")),
                hasProperty("name", is("watcher")),
                hasProperty("name", is("user")),
                hasProperty("name", is("auction_info")),
                hasProperty("name", is("auction_item"))));

    }
}
