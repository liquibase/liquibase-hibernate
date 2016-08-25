package liquibase.ext.hibernate.database;

import liquibase.database.DatabaseConnection;
import liquibase.database.OfflineConnection;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.ServiceRegistry;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Properties;

/**
 * Database implementation for "classic" hibernate configurations.
 */
public class HibernateClassicDatabase extends HibernateDatabase {

    protected Configuration configuration;

    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return conn.getURL().startsWith("hibernate:classic:");
    }

    @Override
    protected String findDialectName() {
        String dialectName = super.findDialectName();

        if (dialectName == null) {
            dialectName = configuration.getProperty(AvailableSettings.DIALECT);
        }
        return dialectName;
    }


    protected Metadata generateMetadata() throws DatabaseException {
        this.configuration = new Configuration();
        this.configuration.configure(getHibernateConnection().getPath());

        return super.generateMetadata();
    }

    @Override
    protected void addToSources(MetadataSources sources) throws DatabaseException {
        Configuration config = new Configuration(sources);
        config.configure(getHibernateConnection().getPath());

        config.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");


        ServiceRegistry standardRegistry = configuration.getStandardServiceRegistryBuilder()
                .applySettings(config.getProperties())
                .addService(ConnectionProvider.class, new NoOpConnectionProvider())
                .addService(MultiTenantConnectionProvider.class, new NoOpConnectionProvider())
                .build();

        config.buildSessionFactory(standardRegistry);
    }

    @Override
    public String getShortName() {
        return "hibernateClassic";
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return "Hibernate Classic";
    }


}