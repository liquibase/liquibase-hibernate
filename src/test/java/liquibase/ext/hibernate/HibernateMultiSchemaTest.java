package liquibase.ext.hibernate;

import liquibase.database.Database;
import liquibase.database.core.H2Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.commandline.LiquibaseCommandLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

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
    public void runCommandLineDiffChangelog() throws Exception {
        final File changelogFile = File.createTempFile("diffChangeLog-test", ".yaml");

        //we don't ship jansi, so we know we can disable it without having to do the slow class checking
        System.setProperty("org.fusesource.jansi.Ansi.disable", "true");
        final LiquibaseCommandLine cli = new LiquibaseCommandLine();

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

        if (returnCode != 0) {
            throw new RuntimeException("LiquibaseCommandLine failed: " + returnCode);
        }
    }

}
