package liquibase.ext.hibernate.database;

import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.service.ServiceRegistry;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.spi.PersistenceUnitInfo;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Database implementation for "spring" hibernate configurations.
 * This supports passing a spring XML file reference and bean name or a package containing hibernate annotated classes.
 */
public class HibernateSpringDatabase extends HibernateEjb3Database {

    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return conn.getURL().startsWith("hibernate:spring:");
    }

    @Override
    protected void addToSources(MetadataSources sources) throws DatabaseException {
        if (isXmlFile()) {
            addToSourcesFromXml(sources);
        } else {
            super.addToSources(sources);
        }
    }


    /**
     * Return true if the given path is a spring XML file.
     */
    protected boolean isXmlFile() {
        String path = getHibernateConnection().getPath();
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

    /**
     * Parse the given URL assuming it is a spring XML file
     */
    protected void addToSourcesFromXml(MetadataSources sources) throws DatabaseException {
        // Read configuration
        BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
        reader.setNamespaceAware(true);
        HibernateConnection connection = getHibernateConnection();
        reader.loadBeanDefinitions(new ClassPathResource(connection.getPath()));

        Properties props = connection.getProperties();
        Class<? extends LocalSessionFactoryBean> beanClass = LocalSessionFactoryBean.class;

        String beanName = props.getProperty("bean", null);
        String beanClassName = props.getProperty("beanClass", null);

        if (beanClassName != null) {
            beanClass = findClass(beanClassName, beanClass);
        }

        if (beanName == null) {
            throw new IllegalStateException("A 'bean' name is required, matching a '" + beanClassName + "' definition in '" + connection.getPath() + "'.");
        }

        BeanDefinition beanDef = registry.getBeanDefinition(beanName);
        if (beanDef == null) {
            throw new IllegalStateException("A bean named '" + beanName + "' could not be found in '" + connection.getPath() + "'.");
        }

        MutablePropertyValues properties = beanDef.getPropertyValues();

        // Add annotated classes list.
        PropertyValue annotatedClassesProperty = properties.getPropertyValue("annotatedClasses");
        if (annotatedClassesProperty != null) {
            List<TypedStringValue> annotatedClasses = (List<TypedStringValue>) annotatedClassesProperty.getValue();
            if (annotatedClasses != null) {
                for (TypedStringValue className : annotatedClasses) {
                    LOG.info("Found annotated class " + className.getValue());
                    sources.addAnnotatedClass(findClass(className.getValue()));
                }
            }
        }

        try {
            // Add mapping locations
            PropertyValue mappingLocationsProp = properties.getPropertyValue("mappingLocations");
            if (mappingLocationsProp != null) {
                List<TypedStringValue> mappingLocations = (List<TypedStringValue>) mappingLocationsProp.getValue();
                if (mappingLocations != null) {
                    ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
                    for (TypedStringValue mappingLocation : mappingLocations) {
                        LOG.info("Found mappingLocation " + mappingLocation.getValue());
                        Resource[] resources = resourcePatternResolver.getResources(mappingLocation.getValue());
                        for (int i = 0; i < resources.length; i++) {
                            URL url = resources[i].getURL();
                            LOG.info("Adding resource  " + url);
                            sources.addURL(url);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                throw (DatabaseException) e;
            } else {
                throw new DatabaseException(e);
            }
        }
    }

    private Class<?> findClass(String className) {
        return findClass(className, Object.class);
    }

    private <T> Class<? extends T> findClass(String className, Class<T> superClass) {
        try {
            Class<?> newClass = Class.forName(className);
            if (superClass.isAssignableFrom(newClass)) {
                return newClass.asSubclass(superClass);
            } else {
                throw new IllegalStateException("The provided class '" + className + "' is not assignable from the '" + superClass.getName() + "' superclass.");
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to find required class: '" + className + "'. Please check classpath and class name.");
        }
    }


    @Override
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

        HashMap map = new HashMap();map.put(AvailableSettings.DIALECT, findDialectName());
        EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(persistenceUnitInfo, map);
        return  builder.build();

    }


    @Override
    public String getShortName() {
        return "hibernateSpring";
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return "Hibernate Spring";
    }

}
