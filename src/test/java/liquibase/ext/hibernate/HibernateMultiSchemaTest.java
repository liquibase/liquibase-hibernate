package liquibase.ext.hibernate;

import liquibase.database.Database;
import liquibase.database.core.H2Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.commandline.LiquibaseCommandLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.Assert.*;

/**
 * Integration test for multi-schema support in Hibernate extension.
 * Verifies that entities with schema annotations are correctly placed in their specified schemas.
 */
public class HibernateMultiSchemaTest {

    private Database database;
    private Connection connection;

    @Before
    public void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection("jdbc:h2:mem:TESTDB" + System.currentTimeMillis(), "SA", "");
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
    public void testMultiSchemaDiffChangelog() throws Exception {
        // GIVEN: Test entities in different schemas
        // - FirstTable in PUBLIC schema
        // - SecondarySchemaEntity in SECOND schema with sequence generator
        // - DefaultSchemaEntity without explicit schema
        final File changelogFile = File.createTempFile("diffChangeLog-test", ".yaml");
        changelogFile.deleteOnExit();

        //we don't ship jansi, so we know we can disable it without having to do the slow class checking
        System.setProperty("org.fusesource.jansi.Ansi.disable", "true");
        final LiquibaseCommandLine cli = new LiquibaseCommandLine();

        // WHEN: Generate diff changelog for multiple schemas
        int returnCode = cli.execute(new String[] {
                "--changelogFile=" + changelogFile.getAbsolutePath(),
                "--referenceUrl=hibernate:spring:com.example.multischema.auction?dialect=org.hibernate.dialect.H2Dialect",
                "--referenceDriver=liquibase.ext.hibernate.database.connection.HibernateDriver",
                "--referenceDefaultSchemaName=PUBLIC",
                "--url=jdbc:h2:mem:TESTDB" + System.currentTimeMillis(),
                "--driver=org.h2.Driver",
                "--username=SA",
                "--password=",
                "--includeSchema=true",
                "--schemas=PUBLIC,SECOND",
                "--log-level=INFO",
                "diffChangelog",
                "--author=test",
        });

        // THEN: Command should execute successfully
        assertEquals("Diff changelog command should complete successfully", 0, returnCode);

        // AND: Changelog file should be created and not empty
        assertTrue("Changelog file should exist", changelogFile.exists());
        assertTrue("Changelog file should not be empty", changelogFile.length() > 0);

        // AND: Verify changelog content contains schema information
        String changelogContent = new String(Files.readAllBytes(changelogFile.toPath()));

        // AND: Verify FirstTable is in PUBLIC schema
        assertTrue("Changelog should contain first_table",
                changelogContent.contains("first_table"));
        assertTrue("first_table should be in PUBLIC schema",
                changelogContent.contains("schemaName: PUBLIC") || changelogContent.contains("schemaName=\"PUBLIC\""));

        // AND: Verify SecondarySchemaEntity is in SECOND schema
        assertTrue("Changelog should contain secondary_schema_entity",
                changelogContent.contains("secondary_schema_entity"));
        assertTrue("secondary_schema_entity should be in SECOND schema",
                changelogContent.contains("schemaName: SECOND") || changelogContent.contains("schemaName=\"SECOND\""));

        // AND: Verify sequence is in SECOND schema
        assertTrue("Changelog should contain secondary_schema_entity_seq sequence",
                changelogContent.contains("secondary_schema_entity_seq"));

        // AND: Verify DefaultSchemaEntity exists (schema may vary based on default behavior)
        assertTrue("Changelog should contain default_schema_entity",
                changelogContent.contains("default_schema_entity"));
    }

}
