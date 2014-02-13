package liquibase.ext.hibernate.database;

import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import liquibase.ext.hibernate.database.connection.HibernateDriver;
import liquibase.logging.LogFactory;
import liquibase.logging.Logger;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;

/**
 * Base class for all Hibernate Databases. This extension interacts with Hibernate by creating standard liquibase.database.Database implementations that
 * bridge what Liquibase expects and the Hibernate APIs.
 */
public abstract class HibernateDatabase extends AbstractJdbcDatabase {

    protected static final Logger LOG = LogFactory.getLogger("liquibase-hibernate");

    private Configuration configuration;

    private Dialect dialect;

    private boolean indexesForForeignKeys = false;

    public HibernateDatabase() {
        setDefaultCatalogName("HIBERNATE");
        setDefaultSchemaName("HIBERNATE");
    }

    @Override
    public void setConnection(DatabaseConnection conn) {
        super.setConnection(conn);

        try {
            LOG.info("Reading hibernate configuration " + getConnection().getURL());

            this.configuration = buildConfiguration(((HibernateConnection) ((JdbcConnection) conn).getUnderlyingConnection()));

            this.configuration.buildMappings();
            this.dialect = configureDialect();

            afterSetup();
        } catch (DatabaseException e) {
            throw new UnexpectedLiquibaseException(e);
        }

    }

    /**
     * Return the dialect used by hibernate
     */
    protected Dialect configureDialect() throws DatabaseException {
        Dialect dialect;
        String dialectString = configuration.getProperty("hibernate.dialect");
        if (dialectString != null)
            try {
                dialect = (Dialect) Class.forName(dialectString).newInstance();
                LOG.info("Using dialect " + dialectString);
            } catch (Exception e) {
                throw new DatabaseException(e);
            }
        else {
            LOG.info("Could not determine hibernate dialect, using HibernateGenericDialect");
            dialect = new HibernateGenericDialect();
        }

        return dialect;
    }

    /**
     * Perform any post-configuration setting logic.
     */
    protected void afterSetup() {
        if (dialect instanceof MySQLDialect) {
            indexesForForeignKeys = true;
        }
    }

    /**
     * Concrete implementations use this method to create the hibernate Configuration object based on the passed URL
     */

    protected abstract Configuration buildConfiguration(HibernateConnection conn) throws DatabaseException;

    public boolean requiresPassword() {
        return false;
    }

    public boolean requiresUsername() {
        return false;
    }

    public String getDefaultDriver(String url) {
        if (url.startsWith("hibernate")) {
            return HibernateDriver.class.getName();
        }
        return null;
    }

    public int getPriority() {
        return PRIORITY_DEFAULT;
    }

    @Override
    public boolean createsIndexesForForeignKeys() {
        return indexesForForeignKeys;
    }

    @Override
    public Integer getDefaultPort() {
        return 0;
    }

    @Override
    public boolean supportsInitiallyDeferrableColumns() {
        return false;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    public Configuration getConfiguration() throws DatabaseException {
        return configuration;
    }

    public Dialect getDialect() throws DatabaseException {
        return dialect;
    }

    @Override
    protected String getConnectionCatalogName() throws DatabaseException {
        return null;
    }

    @Override
    protected String getConnectionSchemaName() {
        return null;
    }

    @Override
    public boolean isSafeToRunUpdate() throws DatabaseException {
        return true;
    }

    @Override
    public boolean isCaseSensitive() {
        return false;
    }

    @Override
    public boolean supportsSchemas() {
        return true;
    }

    @Override
    public boolean supportsCatalogs() {
        return false;
    }
}
