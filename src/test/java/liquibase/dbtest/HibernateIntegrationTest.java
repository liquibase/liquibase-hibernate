package liquibase.dbtest;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.core.H2Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.Diff;
import liquibase.diff.DiffResult;
import liquibase.ext.hibernate.database.HibernateConnection;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

public class HibernateIntegrationTest {
    private static final String HIBERNATE_CONFIG_FILE = "Hibernate.cfg.xml";
    private Database database;
    private Connection connection;

    @Before
    public void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb", "SA", "");
        database = new H2Database();
        database.setConnection(new JdbcConnection(connection));
    }

    @After
    public void tearDown() throws Exception {
        database.close();
        connection = null;
        database = null;
    }

    @Test
    public void runGeneratedChangeLog() throws Exception {

        Liquibase liquibase = new Liquibase(null, new ClassLoaderResourceAccessor(), database);

        Database hibernateDatabase = new HibernateDatabase();
        hibernateDatabase.setConnection(new JdbcConnection(new HibernateConnection(HIBERNATE_CONFIG_FILE)));

        Diff diff = new Diff(hibernateDatabase, database);

        // FIXME
        diff.setDiffIndexes(false);

        DiffResult diffResult = diff.compare();

        assertTrue(diffResult.getMissingTables().size() > 0);

        File outFile = File.createTempFile("lb-test", ".xml");
        OutputStream outChangeLog = new FileOutputStream(outFile);
        diffResult.printChangeLog(new PrintStream(outChangeLog), hibernateDatabase);
        outChangeLog.close();

        liquibase = new Liquibase(outFile.toString(), new FileSystemResourceAccessor(), database);
        liquibase.update(null);

        diff = new Diff(hibernateDatabase, database);

        // FIXME
        diff.setDiffIndexes(false);

        diffResult = diff.compare();

        assertEquals(0, diffResult.getMissingTables().size());
        assertEquals(0, diffResult.getMissingColumns().size());
        assertEquals(0, diffResult.getMissingPrimaryKeys().size());
        assertEquals(0, diffResult.getMissingIndexes().size());
        assertEquals(0, diffResult.getMissingViews().size());

        assertEquals(0, diffResult.getUnexpectedTables().size());
        assertEquals(0, diffResult.getUnexpectedColumns().size());
        assertEquals(0, diffResult.getUnexpectedPrimaryKeys().size());
        assertEquals(0, diffResult.getUnexpectedIndexes().size());
        assertEquals(0, diffResult.getUnexpectedViews().size());
    }

    @Test
    public void hibernateSchemaUpdate() throws Exception {

        SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
        SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, true);
        builder.bind("java:/data", ds);
        builder.activate();

        Configuration cfg = new Configuration();
        cfg.configure(HIBERNATE_CONFIG_FILE);

        SchemaExport export = new SchemaExport(cfg);
        export.execute(true, true, false, false);

        Database hibernateDatabase = new HibernateDatabase();
        hibernateDatabase.setConnection(new JdbcConnection(new HibernateConnection(HIBERNATE_CONFIG_FILE)));

        Diff diff = new Diff(hibernateDatabase, database);

        // FIXME
        diff.setDiffIndexes(false);

        DiffResult diffResult = diff.compare();

        diffResult.printResult(System.out);

        assertEquals(0, diffResult.getMissingTables().size());
        assertEquals(0, diffResult.getMissingColumns().size());
        assertEquals(0, diffResult.getMissingPrimaryKeys().size());
        assertEquals(0, diffResult.getMissingIndexes().size());
        assertEquals(0, diffResult.getMissingViews().size());
        assertEquals(0, diffResult.getMissingForeignKeys().size());

        assertEquals(0, diffResult.getUnexpectedTables().size());
        assertEquals(0, diffResult.getUnexpectedColumns().size());
        assertEquals(0, diffResult.getUnexpectedPrimaryKeys().size());
        assertEquals(0, diffResult.getUnexpectedIndexes().size());
        assertEquals(0, diffResult.getUnexpectedViews().size());
        assertEquals(0, diffResult.getUnexpectedForeignKeys().size());

    }
}
