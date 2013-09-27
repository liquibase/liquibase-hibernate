package liquibase.ext.hibernate.database;

import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.event.PostInsertEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class HibernateDatabase extends AbstractJdbcDatabase {

    private static final Logger LOG                   = LoggerFactory.getLogger(HibernateDatabase.class);

    private Configuration       configuration;

    private Dialect             dialect;

    private boolean             indexesForForeignKeys = false;

    private ConfigLocator       locator;

    public HibernateDatabase() {
    }

    public boolean requiresPassword() {
        return false;
    }

    public boolean requiresUsername() {
        return false;
    }

    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return ConfigType.forUrl(conn.getURL()) != null;
    }

    public String getDefaultDriver(String url) {
        return HibernateDriver.class.getName();
    }

    public int getPriority() {
        return PRIORITY_DEFAULT;
    }

    private void configure() throws DatabaseException {
        LOG.info("Reading configuration " + getConnection().getURL());
        locator = new ConfigLocator(getConnection().getURL());
        String namingStrategy;
        switch (locator.getType()) {
            case EJB3:
                Ejb3Configuration ejb3Configuration = new Ejb3Configuration();
                ejb3Configuration.configure(getConnection().getURL().substring("persistence:".length()), Collections.EMPTY_MAP);
                configuration = ejb3Configuration.getHibernateConfiguration();
                configuration.setProperty("hibernate.dialect", ejb3Configuration.getProperties().getProperty("hibernate.dialect"));

                namingStrategy = ejb3Configuration.getProperties().getProperty("hibernate.ejb.naming_strategy");
                if (namingStrategy != null) {
                    try {
                        configuration.setNamingStrategy((NamingStrategy) Class.forName(namingStrategy).newInstance());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Failed to instantiate naming strategy", e);
                    } catch (InstantiationException e) {
                        throw new IllegalStateException("Couldn't access naming strategy", e);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("Failed to find naming strategy", e);
                    }
                }

                for (PostInsertEventListener postInsertEventListener : configuration.getEventListeners().getPostInsertEventListeners()) {
                    if (postInsertEventListener instanceof org.hibernate.envers.event.AuditEventListener) {
                        AuditConfiguration.getFor(configuration);
                    }
                }
                break;
            case SPRING:
                try {
                    configuration = new SpringConfigurator(locator).createSpringConfiguration();
                } catch (Exception e1) {
                    throw new DatabaseException(e1);
                }
                break;
            case HIBERNATE:
                configuration = new Configuration();
                configuration.configure(locator.getPath());

                namingStrategy = configuration.getProperty("hibernate.namingStrategy");
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
                    break;
                }
            case CUSTOM: {
                configuration = new CustomConfigurator(locator).getConfiguration();
                break;
            }
        }
        configuration.buildMappings();
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
        if (dialect instanceof MySQLDialect)
            indexesForForeignKeys = true;
    }

    @Override
    public boolean createsIndexesForForeignKeys() {
        return indexesForForeignKeys;
    }

    @Override
    public String getShortName() {
        return "hibernate";
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

    @Override
    protected String getDefaultDatabaseProductName() {
        return getDatabaseProductName();
    }

    public Configuration getConfiguration() throws DatabaseException {
        if (configuration == null)
            configure();
        return configuration;
    }

    public Dialect getDialect() throws DatabaseException {
        if (dialect == null)
            configure();
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
    public void checkDatabaseChangeLogLockTable() throws DatabaseException {
        // Nothing to do
    }

    @Override
    public void checkDatabaseChangeLogTable(boolean updateExistingNullChecksums, DatabaseChangeLog databaseChangeLog, String... contexts) throws DatabaseException {
        // Nothing to do
    }

    @Override
    public boolean isSafeToRunUpdate() throws DatabaseException {
        // Do not prompt the user before running Liquibase
        return true;
    }

    @Override
    public boolean isCaseSensitive() {
        return false;
    }
}
