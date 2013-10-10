package liquibase.ext.hibernate.customfactory;

import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import org.hibernate.cfg.Configuration;

/**
 * Implement this interface to dynamically generate a hibernate:classic configuration.
 * For example, if you create a class called com.example.hibernate.MyConfig, specify a url of hibernate:classic:com.example.hibernate.MyConfig.
 */
public interface CustomClassicConfigurationFactory {

    /**
     * Create a hibernate Configuration for the given database and connection.
     */
    Configuration getConfiguration(HibernateDatabase hibernateDatabase, HibernateConnection connection);

}
