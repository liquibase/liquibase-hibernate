package liquibase.ext.hibernate.database;

import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.service.ServiceRegistry;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedProperties;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Database implementation for "spring" hibernate configurations.
 * This supports passing a spring XML file reference and bean name or a package containing hibernate annotated classes.
 */
public class HibernateSpringPackageDatabase extends JpaPersistenceDatabase {

    private BeanDefinition beanDefinition;
    private ManagedProperties beanDefinitionProperties;
    private EntityManagerFactory entityManagerFactory;

    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return conn.getURL().startsWith("hibernate:spring:") && !isXmlFile(conn);
    }

    @Override
    public int getPriority() {
        return super.getPriority() + 10; //want this to be picked over HibernateSpringBeanDatabase if it is not xml file
    }

    /**
     * Return true if the given path is a spring XML file.
     */
    protected boolean isXmlFile(DatabaseConnection connection) {
        HibernateConnection hibernateConnection;
        if (connection instanceof JdbcConnection) {
            hibernateConnection = ((HibernateConnection) ((JdbcConnection) connection).getUnderlyingConnection());
        } else if (connection instanceof HibernateConnection) {
            hibernateConnection = (HibernateConnection) connection;
        } else {
            return false;
        }


        String path = hibernateConnection.getPath();
        if (path.contains("/")) {
            return true;
        }
        ClassPathResource resource = new ClassPathResource(path);
        try {
            if (resource.exists() && !resource.getFile().isDirectory()) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }


    }

    protected EntityManagerFactory createEntityManagerFactory() {
        DefaultPersistenceUnitManager internalPersistenceUnitManager = new DefaultPersistenceUnitManager();

        String[] packagesToScan = getHibernateConnection().getPath().split(",");

        for (String packageName : packagesToScan) {
            LOG.info("Found package "+packageName);
        }

        internalPersistenceUnitManager.setPackagesToScan(packagesToScan);

        internalPersistenceUnitManager.preparePersistenceUnitInfos();
        PersistenceUnitInfo persistenceUnitInfo = internalPersistenceUnitManager.obtainDefaultPersistenceUnitInfo();
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();

        if (persistenceUnitInfo instanceof SmartPersistenceUnitInfo) {
            ((SmartPersistenceUnitInfo) persistenceUnitInfo).setPersistenceProviderPackageName(jpaVendorAdapter.getPersistenceProviderRootPackage());
        }

        HashMap map = new HashMap();
        map.put(AvailableSettings.DIALECT, getProperty(AvailableSettings.DIALECT));
        EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(persistenceUnitInfo, map);
        return  builder.build();

    }

    @Override
    public String getShortName() {
        return "hibernateSpringPackage";
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return "Hibernate Spring Package";
    }

}
