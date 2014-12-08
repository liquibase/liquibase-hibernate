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
import org.hibernate.cfg.NamingStrategy;
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
    public static final String DEFAULT_SCHEMA = "HIBERNATE";

    public HibernateDatabase() {
        setDefaultCatalogName(DEFAULT_SCHEMA);
        setDefaultSchemaName(DEFAULT_SCHEMA);
    }

    @Override
    public void setConnection(DatabaseConnection conn) {
        super.setConnection(conn);

        try {
            LOG.info("Reading hibernate configuration " + getConnection().getURL());

            this.configuration = buildConfiguration(((HibernateConnection) ((JdbcConnection) conn).getUnderlyingConnection()));
            configureNamingStrategy(this.configuration, ((HibernateConnection) ((JdbcConnection) conn).getUnderlyingConnection()));

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
     * Configures the naming strategy use by the connection
     *
     * @param configuration the {@link Configuration}
     * @param connection the {@link HibernateConnection}
     */
    protected void configureNamingStrategy(Configuration configuration, HibernateConnection connection) {
        String namingStrategy = connection.getProperties().getProperty("hibernate.namingStrategy");
        if (namingStrategy == null) {
            namingStrategy = connection.getProperties().getProperty("hibernate.ejb.naming_strategy");
        }
        if (namingStrategy != null) {
            try {
                configuration.setNamingStrategy((NamingStrategy) Class.forName(namingStrategy).newInstance());
            } catch (InstantiationException e) {
                throw new IllegalStateException("Failed to instantiate naming strategy", e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Couldn't access naming strategy", e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to find naming strategy", e);
            }
        }
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
        return getDefaultCatalogName();
    }

    @Override
    protected String getConnectionSchemaName() {
        return getDefaultSchemaName();
    }

    @Override
    public String getDefaultSchemaName() {
        return DEFAULT_SCHEMA;
    }

    @Override
    public String getDefaultCatalogName() {
        return DEFAULT_SCHEMA;
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
