package liquibase.ext.hibernate.database;

import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.ext.hibernate.customfactory.CustomMetadataFactory;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import liquibase.ext.hibernate.database.connection.HibernateDriver;
import liquibase.logging.LogFactory;
import liquibase.logging.Logger;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.ServiceRegistry;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base class for all Hibernate Databases. This extension interacts with Hibernate by creating standard liquibase.database.Database implementations that
 * bridge what Liquibase expects and the Hibernate APIs.
 */
public abstract class HibernateDatabase extends AbstractJdbcDatabase {

    protected static final Logger LOG = LogFactory.getLogger("liquibase-hibernate");

    private Metadata metadata;
    protected Dialect dialect;

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

            this.metadata = buildMetadata();

            afterSetup();
        } catch (DatabaseException e) {
            throw new UnexpectedLiquibaseException(e);
        }

    }


    protected String findDialectName() {
        return getHibernateConnection().getProperties().getProperty(AvailableSettings.DIALECT);
    }

    public Dialect getDialect() {
        return dialect;
    }

    protected HibernateConnection getHibernateConnection() {
        return ((HibernateConnection) ((JdbcConnection) getConnection()).getUnderlyingConnection());
    }

    protected MetadataSources createMetadataSources() throws DatabaseException {
        String dialectString = findDialectName();
        if (dialectString != null) {
            try {
                dialect = (Dialect) Class.forName(dialectString).newInstance();
                LOG.info("Using dialect " + dialectString);
            } catch (Exception e) {
                throw new DatabaseException(e);
            }
        } else {
            LOG.info("Could not determine hibernate dialect, using HibernateGenericDialect");
            dialect = new HibernateGenericDialect();
        }


        ServiceRegistry standardRegistry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DIALECT, dialect)
                .addService(ConnectionProvider.class, new NoOpConnectionProvider())
                .addService(MultiTenantConnectionProvider.class, new NoOpConnectionProvider())
                .build();

        return new MetadataSources(standardRegistry);

    }

    protected void configureNewIdentifierGeneratorSupport(String value, MetadataBuilder builder) throws DatabaseException {
        String _value;
        _value = getHibernateConnection().getProperties().getProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, value);

        try {
            if (_value != null) {
                builder.enableNewIdentifierGeneratorSupport(Boolean.valueOf(_value));
            }
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    protected void configurePhysicalNamingStrategy(String physicalNamingStrategy, MetadataBuilder builder) throws DatabaseException {
        String namingStrategy;
        namingStrategy = getHibernateConnection().getProperties().getProperty(AvailableSettings.PHYSICAL_NAMING_STRATEGY, physicalNamingStrategy);

        try {
            if (namingStrategy != null) {
                builder.applyPhysicalNamingStrategy((PhysicalNamingStrategy) Class.forName(namingStrategy).newInstance());
            }
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    protected void configureImplicitNamingStrategy(String implicitNamingStrategy, MetadataBuilder builder) throws DatabaseException {
        String namingStrategy;
        namingStrategy = getHibernateConnection().getProperties().getProperty(AvailableSettings.IMPLICIT_NAMING_STRATEGY, implicitNamingStrategy);

        try {
            if (namingStrategy != null) {
                switch (namingStrategy) {
                    case "default":
                    case "jpa":
                        builder.applyImplicitNamingStrategy(org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl.INSTANCE);
                        break;
                    case "legacy-hbm":
                        builder.applyImplicitNamingStrategy(org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl.INSTANCE);
                        break;
                    case "legacy-jpa":
                        builder.applyImplicitNamingStrategy(org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl.INSTANCE);
                        break;
                    case "component-path":
                        builder.applyImplicitNamingStrategy(org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl.INSTANCE);
                        break;
                    default:
                        builder.applyImplicitNamingStrategy((ImplicitNamingStrategy) Class.forName(namingStrategy).newInstance());
                        break;
                }

            }
        } catch (Exception e) {
            throw new DatabaseException(e);
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

    protected Metadata buildMetadata() throws DatabaseException {
        String path = getHibernateConnection().getPath();
        if (!path.contains("/")) {
            try {
                Class<?> clazz = Class.forName(path);
                if (CustomMetadataFactory.class.isAssignableFrom(clazz)) {
                    try {
                        return ((CustomMetadataFactory) clazz.newInstance()).getMetadata(this, getHibernateConnection());
                    } catch (InstantiationException e) {
                        throw new DatabaseException(e);
                    } catch (IllegalAccessException e) {
                        throw new DatabaseException(e);
                    }
                }
            } catch (ClassNotFoundException ignore) {
                //not really a class, continue
            }
        }


        return generateMetadata();
    }

    protected Metadata generateMetadata() throws DatabaseException {
        MetadataSources sources = createMetadataSources();
        addToSources(sources);

        MetadataBuilder metadataBuilder = sources.getMetadataBuilder();
        configureMetadataBuilder(metadataBuilder);

        return metadataBuilder.build();
    }


    protected abstract void addToSources(MetadataSources sources) throws DatabaseException;

    protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) throws DatabaseException {
        configureNewIdentifierGeneratorSupport(getProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS), metadataBuilder);
        configureImplicitNamingStrategy(getProperty(AvailableSettings.IMPLICIT_NAMING_STRATEGY), metadataBuilder);
        configurePhysicalNamingStrategy(getProperty(AvailableSettings.PHYSICAL_NAMING_STRATEGY), metadataBuilder);
    }

    public String getProperty(String name) {
        return getHibernateConnection().getProperties().getProperty(name);
    }


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

    public Metadata getMetadata() throws DatabaseException {
        return metadata;
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

    static class NoOpConnectionProvider implements ConnectionProvider, MultiTenantConnectionProvider{

        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("No connection");
        }

        @Override
        public void closeConnection(Connection conn) throws SQLException {

        }

        @Override
        public boolean supportsAggressiveRelease() {
            return false;
        }

        @Override
        public boolean isUnwrappableAs(Class unwrapType) {
            return false;
        }

        @Override
        public <T> T unwrap(Class<T> unwrapType) {
            return null;
        }

        @Override
        public Connection getAnyConnection() throws SQLException {
            return getConnection();
        }

        @Override
        public void releaseAnyConnection(Connection connection) throws SQLException {

        }

        @Override
        public Connection getConnection(String tenantIdentifier) throws SQLException {
            return getConnection();
        }

        @Override
        public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {

        }
    }
}
