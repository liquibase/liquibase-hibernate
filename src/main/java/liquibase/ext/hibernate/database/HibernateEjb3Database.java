package liquibase.ext.hibernate.database;

import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.customfactory.CustomEjb3ConfigurationFactory;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.event.PostInsertEventListener;

/**
 * Database implementation for "ejb3" hibernate configurations.
 * This supports passing an persistence unit name or a {@link liquibase.ext.hibernate.customfactory.CustomEjb3ConfigurationFactory} implementation
 */
public class HibernateEjb3Database extends HibernateDatabase {
    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return conn.getURL().startsWith("hibernate:ejb3:");
    }

    @Override
    protected Configuration buildConfiguration(HibernateConnection connection) throws DatabaseException {
        Ejb3Configuration ejb3Configuration;
        if (isCustomFactoryClass(connection.getPath())) {
            ejb3Configuration = buildConfigurationFromFactory(connection);
        } else {
            ejb3Configuration = buildConfigurationFromFile(connection);
        }

        Configuration configuration = ejb3Configuration.getHibernateConfiguration();
        configuration.setProperty("hibernate.dialect", ejb3Configuration.getProperties().getProperty("hibernate.dialect"));

        String namingStrategy = ejb3Configuration.getProperties().getProperty("hibernate.ejb.naming_strategy");
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

        return configuration;
    }

    /**
     * Create an Ejb3Configuration assuming the passed URL contains a {@link CustomEjb3ConfigurationFactory} class name
     */
    protected Ejb3Configuration buildConfigurationFromFactory(HibernateConnection connection) throws DatabaseException {
        try {
            return ((CustomEjb3ConfigurationFactory) Class.forName(connection.getPath()).newInstance()).getConfiguration(this, connection);
        } catch (InstantiationException e) {
            throw new DatabaseException(e);
        } catch (IllegalAccessException e) {
            throw new DatabaseException(e);
        } catch (ClassNotFoundException e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Create an Ejb3Configuration assuming the passed URL contains a persistence unit name
     */
    protected Ejb3Configuration buildConfigurationFromFile(HibernateConnection connection) {
        Ejb3Configuration ejb3Configuration = new Ejb3Configuration();
        ejb3Configuration.configure(connection.getPath(), connection.getProperties());

        return ejb3Configuration;
    }


    /**
     * Return true if the given path is a {@link CustomEjb3ConfigurationFactory}
     */
    protected boolean isCustomFactoryClass(String path) {
        if (path.contains("/")) {
            return false;
        }

        try {
            Class<?> clazz = Class.forName(path);
            return CustomEjb3ConfigurationFactory.class.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String getShortName() {
        return "hibernateEjb3";
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return "Hibernate EJB3";
    }

}
