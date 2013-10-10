package liquibase.ext.hibernate.database;

import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.customfactory.CustomClassicConfigurationFactory;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;

/**
 * Database implementation for "classic" hibernate configurations.
 * This supports passing a hibernate xml configuration file or a {@link CustomClassicConfigurationFactory} implementation
 */
public class HibernateClassicDatabase extends HibernateDatabase {

    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return conn.getURL().startsWith("hibernate:classic:");
    }

    @Override
    protected Configuration buildConfiguration(HibernateConnection connection) throws DatabaseException {

        if (isCustomFactoryClass(connection.getPath())) {
            return buildConfigurationFromFactory(connection);
        } else {
            return buildConfigurationfromFile(connection);
        }
    }

    /**
     * Build a Configuration object assuming the connection path is a {@link CustomClassicConfigurationFactory} class name
     */
    protected Configuration buildConfigurationFromFactory(HibernateConnection connection) throws DatabaseException {
        try {
            return ((CustomClassicConfigurationFactory) Class.forName(connection.getPath()).newInstance()).getConfiguration(this, connection);
        } catch (InstantiationException e) {
            throw new DatabaseException(e);
        } catch (IllegalAccessException e) {
            throw new DatabaseException(e);
        } catch (ClassNotFoundException e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Build a Configuration object assuming the connection path is a hibernate XML configuration file.
     */
    protected Configuration buildConfigurationfromFile(HibernateConnection connection) {
        Configuration configuration = new Configuration();
        configuration.configure(connection.getPath());

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

        return configuration;
    }

    /**
     * Returns true if the given path is a factory class
     */
    protected boolean isCustomFactoryClass(String path) {
        if (path.contains("/")) {
            return false;
        }

        try {
            Class<?> clazz = Class.forName(path);
            return CustomClassicConfigurationFactory.class.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
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