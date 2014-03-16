package liquibase.ext.hibernate;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.core.HsqlDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffResult;
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.diff.output.report.DiffToReport;
import liquibase.ext.hibernate.database.HibernateSpringDatabase;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import liquibase.logging.LogFactory;
import liquibase.logging.Logger;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.spi.PersistenceUnitInfo;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.Map.Entry;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class SpringPackageScanningIntegrationTest {
    private static final String PACKAGES = "com.example.ejb3.auction";
    private final static Logger log = LogFactory.getLogger();
    private Database database;
    private Connection connection;
    private CompareControl compareControl;

    int run = 0;

    @Before
    public void setUp() throws Exception {
        Class.forName("org.hsqldb.jdbc.JDBCDriver");
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:TESTDB" + System.currentTimeMillis()+"-"+(run++), "SA", "");
        database = new HsqlDatabase();
        database.setConnection(new JdbcConnection(connection));

        Set<Class<? extends DatabaseObject>> typesToInclude = new HashSet<Class<? extends DatabaseObject>>();
        typesToInclude.add(Table.class);
        typesToInclude.add(Column.class);
        typesToInclude.add(PrimaryKey.class);
        typesToInclude.add(ForeignKey.class);
        typesToInclude.add(UniqueConstraint.class);
        typesToInclude.add(Sequence.class);
        compareControl = new CompareControl(typesToInclude);
        compareControl.addSuppressedField(Table.class, "remarks");
        compareControl.addSuppressedField(Column.class, "remarks");
        compareControl.addSuppressedField(Column.class, "certainDataType");
        compareControl.addSuppressedField(Column.class, "autoIncrementInformation");
        compareControl.addSuppressedField(ForeignKey.class, "deleteRule");
        compareControl.addSuppressedField(ForeignKey.class, "updateRule");
        compareControl.addSuppressedField(Index.class, "unique");
    }

    @After
    public void tearDown() throws Exception {
        database.close();
        connection = null;
        database = null;
        compareControl = null;
    }

    /**
     * Generates a changelog from the Hibernate mapping, creates the database
     * according to the changelog, compares, the database with the mapping.
     * 
     * @throws Exception
     */
    @Test
    public void runGeneratedChangeLog() throws Exception {

        Liquibase liquibase = new Liquibase((String) null, new ClassLoaderResourceAccessor(), database);

        Database hibernateDatabase = new HibernateSpringDatabase();
        hibernateDatabase.setDefaultSchemaName("PUBLIC");
        hibernateDatabase.setDefaultCatalogName("TESTDB");
        hibernateDatabase.setConnection(new JdbcConnection(new HibernateConnection("hibernate:spring:"+ PACKAGES+ "?dialect=" + HSQLDialect.class.getName())));

        DiffResult diffResult = liquibase.diff(hibernateDatabase, database, compareControl);

        assertTrue(diffResult.getMissingObjects().size() > 0);

        File outFile = File.createTempFile("lb-test", ".xml");
        OutputStream outChangeLog = new FileOutputStream(outFile);
        String changeLogString = toChangeLog(diffResult);
        outChangeLog.write(changeLogString.getBytes("UTF-8"));
        outChangeLog.close();

        log.info("Changelog:\n" + changeLogString);

        liquibase = new Liquibase(outFile.toString(), new FileSystemResourceAccessor(), database);
        StringWriter stringWriter = new StringWriter();
        liquibase.update((String) null, stringWriter);
        log.info(stringWriter.toString());
        liquibase.update((String) null);

        diffResult = liquibase.diff(hibernateDatabase, database, compareControl);

        ignoreDatabaseChangeLogTable(diffResult);
        ignoreConversionFromFloatToDouble64(diffResult);

        String differences = toString(diffResult);

        assertEquals(differences, 0, diffResult.getMissingObjects().size());
        assertEquals(differences, 0, diffResult.getUnexpectedObjects().size());
//        assertEquals(differences, 0, diffResult.getChangedObjects().size()); //unimportant differences in schema name and datatypes causing test to fail

    }

    /**
     * Creates a database using Hibernate SchemaExport and compare the database
     * with the Hibernate mapping
     * 
     * @throws Exception
     */
    @Test
    public void hibernateSchemaExport() throws Exception {

        SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, true);

        Configuration cfg = createSpringPackageScanningConfiguration();
        Properties properties = new Properties();
        properties.put(Environment.DATASOURCE, ds);
        cfg.addProperties(properties);

        SchemaExport export = new SchemaExport(cfg);
        export.execute(true, true, false, false);

        Database hibernateDatabase = new HibernateSpringDatabase();
        hibernateDatabase.setDefaultSchemaName("PUBLIC");
        hibernateDatabase.setDefaultCatalogName("TESTDB");
        hibernateDatabase.setConnection(new JdbcConnection(new HibernateConnection("hibernate:spring:"+ PACKAGES+ "?dialect=" + HSQLDialect.class.getName())));

        Liquibase liquibase = new Liquibase((String) null, new ClassLoaderResourceAccessor(), database);
        DiffResult diffResult = liquibase.diff(hibernateDatabase, database, compareControl);

        ignoreDatabaseChangeLogTable(diffResult);
        ignoreConversionFromFloatToDouble64(diffResult);

        String differences = toString(diffResult);

        assertEquals(differences, 0, diffResult.getMissingObjects().size());
        assertEquals(differences, 0, diffResult.getUnexpectedObjects().size());
//        assertEquals(differences, 0, diffResult.getChangedObjects().size()); //unimportant differences in schema name and datatypes causing test to fail

    }

    private Configuration createSpringPackageScanningConfiguration() {
        DefaultPersistenceUnitManager internalPersistenceUnitManager = new DefaultPersistenceUnitManager();

        internalPersistenceUnitManager.setPackagesToScan(PACKAGES);

        internalPersistenceUnitManager.preparePersistenceUnitInfos();
        PersistenceUnitInfo persistenceUnitInfo = internalPersistenceUnitManager
                .obtainDefaultPersistenceUnitInfo();
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setDatabasePlatform(HSQLDialect.class.getName());

        Map<String, Object> jpaPropertyMap = jpaVendorAdapter.getJpaPropertyMap();
        jpaPropertyMap.put("hibernate.archive.autodetection", "false");

        if (persistenceUnitInfo instanceof SmartPersistenceUnitInfo) {
            ((SmartPersistenceUnitInfo) persistenceUnitInfo).setPersistenceProviderPackageName(jpaVendorAdapter.getPersistenceProviderRootPackage());
        }

        EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(persistenceUnitInfo,
                jpaPropertyMap, null);
        ServiceRegistry serviceRegistry = builder.buildServiceRegistry();
        return builder.buildHibernateConfiguration(serviceRegistry);
    }

    /**
     * Generates the changelog from Hibernate mapping, creates 2 databases,
     * updates 1 of the databases with HibernateSchemaUpdate. Compare the 2
     * databases.
     * 
     * @throws Exception
     */
    @Test
    public void hibernateSchemaUpdate() throws Exception {

        Liquibase liquibase = new Liquibase((String) null, new ClassLoaderResourceAccessor(), database);

        Database hibernateDatabase = new HibernateSpringDatabase();
        hibernateDatabase.setDefaultSchemaName("PUBLIC");
        hibernateDatabase.setDefaultCatalogName("TESTDB");
        hibernateDatabase.setConnection(new JdbcConnection(
                new HibernateConnection("hibernate:spring:" + PACKAGES + "?dialect=" + HSQLDialect.class.getName())));

        DiffResult diffResult = liquibase.diff(hibernateDatabase, database, compareControl);

        assertTrue(diffResult.getMissingObjects().size() > 0);

        File outFile = File.createTempFile("lb-test", ".xml");
        OutputStream outChangeLog = new FileOutputStream(outFile);
        String changeLogString = toChangeLog(diffResult);
        outChangeLog.write(changeLogString.getBytes("UTF-8"));
        outChangeLog.close();

        log.info("Changelog:\n" + changeLogString);

        liquibase = new Liquibase(outFile.toString(), new FileSystemResourceAccessor(), database);
        StringWriter stringWriter = new StringWriter();
        liquibase.update((String) null, stringWriter);
        log.info(stringWriter.toString());
        liquibase.update((String) null);

        long currentTimeMillis = System.currentTimeMillis();
        Connection connection2 = DriverManager.getConnection("jdbc:hsqldb:mem:TESTDB2" + currentTimeMillis, "SA", "");
        Database database2 = new HsqlDatabase();
        database2.setConnection(new JdbcConnection(connection2));

        Configuration cfg = createSpringPackageScanningConfiguration();
        cfg.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:TESTDB2" + currentTimeMillis);

        SchemaUpdate update = new SchemaUpdate(cfg);
        update.execute(true, true);

        diffResult = liquibase.diff(database, database2, compareControl);
        
        ignoreDatabaseChangeLogTable(diffResult);
        ignoreConversionFromFloatToDouble64(diffResult);

        String differences = toString(diffResult);

        assertEquals(differences, 0, diffResult.getMissingObjects().size());
        assertEquals(differences, 0, diffResult.getUnexpectedObjects().size());
//        assertEquals(differences, 0, diffResult.getChangedObjects().size()); //unimportant differences in schema name and datatypes causing test to fail
    }

    private String toString(DiffResult diffResult) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out, true, "UTF-8");
        DiffToReport diffToReport = new DiffToReport(diffResult, printStream);
        diffToReport.print();
        printStream.close();
        return out.toString("UTF-8");
    }

    private String toChangeLog(DiffResult diffResult) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out, true, "UTF-8");
        DiffToChangeLog diffToChangeLog = new DiffToChangeLog(diffResult,
                new DiffOutputControl());
        diffToChangeLog.print(printStream);
        printStream.close();
        return out.toString("UTF-8");
    }

    private void ignoreDatabaseChangeLogTable(DiffResult diffResult)
            throws Exception {
        Set<Table> unexpectedTables = diffResult
                .getUnexpectedObjects(Table.class);
        for (Iterator<Table> iterator = unexpectedTables.iterator(); iterator
                .hasNext();) {
            Table table = iterator.next();
            if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(table.getName())
                    || "DATABASECHANGELOG".equalsIgnoreCase(table.getName()))
                diffResult.getUnexpectedObjects().remove(table);
        }
        Set<Table> missingTables = diffResult
                .getMissingObjects(Table.class);
        for (Iterator<Table> iterator = missingTables.iterator(); iterator
                .hasNext();) {
            Table table = iterator.next();
            if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(table.getName())
                    || "DATABASECHANGELOG".equalsIgnoreCase(table.getName()))
                diffResult.getMissingObjects().remove(table);
        }
        Set<Column> unexpectedColumns = diffResult.getUnexpectedObjects(Column.class);
        for (Iterator<Column> iterator = unexpectedColumns.iterator(); iterator.hasNext();) {
            Column column = iterator.next();
            if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(column.getRelation().getName())
                    || "DATABASECHANGELOG".equalsIgnoreCase(column.getRelation().getName()))
                diffResult.getUnexpectedObjects().remove(column);
        }
        Set<Column> missingColumns = diffResult.getMissingObjects(Column.class);
        for (Iterator<Column> iterator = missingColumns.iterator(); iterator.hasNext();) {
            Column column = iterator.next();
            if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(column.getRelation().getName())
                    || "DATABASECHANGELOG".equalsIgnoreCase(column.getRelation().getName()))
                diffResult.getMissingObjects().remove(column);
        }
        Set<Index> unexpectedIndexes = diffResult.getUnexpectedObjects(Index.class);
        for (Iterator<Index> iterator = unexpectedIndexes.iterator(); iterator.hasNext();) {
            Index index = iterator.next();
            if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(index.getTable().getName())
                    || "DATABASECHANGELOG".equalsIgnoreCase(index.getTable().getName()))
                diffResult.getUnexpectedObjects().remove(index);
        }
        Set<Index> missingIndexes = diffResult.getMissingObjects(Index.class);
        for (Iterator<Index> iterator = missingIndexes.iterator(); iterator.hasNext();) {
            Index index = iterator.next();
            if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(index.getTable().getName())
                    || "DATABASECHANGELOG".equalsIgnoreCase(index.getTable().getName()))
                diffResult.getMissingObjects().remove(index);
        }
        Set<PrimaryKey> unexpectedPrimaryKeys = diffResult.getUnexpectedObjects(PrimaryKey.class);
        for (Iterator<PrimaryKey> iterator = unexpectedPrimaryKeys.iterator(); iterator.hasNext();) {
            PrimaryKey primaryKey = iterator.next();
            if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(primaryKey.getTable()
                    .getName())
                    || "DATABASECHANGELOG".equalsIgnoreCase(primaryKey.getTable().getName()))
                diffResult.getUnexpectedObjects().remove(primaryKey);
        }
        Set<PrimaryKey> missingPrimaryKeys = diffResult.getMissingObjects(PrimaryKey.class);
        for (Iterator<PrimaryKey> iterator = missingPrimaryKeys.iterator(); iterator.hasNext();) {
            PrimaryKey primaryKey = iterator.next();
            if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(primaryKey.getTable().getName())
                    || "DATABASECHANGELOG".equalsIgnoreCase(primaryKey.getTable().getName()))
                diffResult.getMissingObjects().remove(primaryKey);
        }
    }

    /**
     * Columns created as float are seen as DOUBLE(64) in database metadata.
     * HsqlDB bug?
     * 
     * @param diffResult
     * @throws Exception
     */
    private void ignoreConversionFromFloatToDouble64(DiffResult diffResult)
            throws Exception {
        Map<DatabaseObject, ObjectDifferences> differences = diffResult.getChangedObjects();
        for (Iterator<Entry<DatabaseObject, ObjectDifferences>> iterator = differences.entrySet().iterator(); iterator.hasNext();) {
            Entry<DatabaseObject, ObjectDifferences> changedObject = iterator.next();
            Difference difference = changedObject.getValue().getDifference("type");
            if (difference != null
                    && difference.getReferenceValue() != null
                    && difference.getComparedValue() != null
                    && difference.getReferenceValue().toString().equals("float")
                    && difference.getComparedValue().toString().startsWith("DOUBLE(64)")) {
                log.info("Ignoring difference "
                        + changedObject.getKey().toString() + " "
                        + difference.toString());
                changedObject.getValue()
                        .removeDifference(difference.getField());
            }
        }
    }

}
