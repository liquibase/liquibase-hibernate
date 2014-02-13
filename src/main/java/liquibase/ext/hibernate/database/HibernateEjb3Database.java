package liquibase.ext.hibernate.database;

import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.customfactory.CustomEjb3ConfigurationFactory;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.service.ServiceRegistry;

import java.util.Map;

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

        if (isCustomFactoryClass(connection.getPath())) {
            return buildConfigurationFromFactory(connection);
        } else {
            return buildConfigurationfromFile(connection);
        }
    }
    /**
     * Build a Configuration object assuming the connection path is a hibernate XML configuration file.
     */
    protected Configuration buildConfigurationfromFile(HibernateConnection connection) {

        MyHibernatePersistenceProvider persistenceProvider = new MyHibernatePersistenceProvider();
        EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) persistenceProvider.getEntityManagerFactoryBuilderOrNull(connection.getPath(), null, null);
        ServiceRegistry serviceRegistry = builder.buildServiceRegistry();

        return builder.buildHibernateConfiguration(serviceRegistry);
    }

    /**
     * Build a Configuration object assuming the connection path is a {@link CustomEjb3ConfigurationFactory} class name
     */
    protected Configuration buildConfigurationFromFactory(HibernateConnection connection) throws DatabaseException {
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

    private static class MyHibernatePersistenceProvider extends HibernatePersistenceProvider {
        @Override
        protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map properties, ClassLoader providedClassLoader) {
            return super.getEntityManagerFactoryBuilderOrNull(persistenceUnitName, properties, providedClassLoader);
        }
    }
}
