package liquibase.ext.hibernate.database;

import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import liquibase.logging.LogFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.spi.PersistenceUnitTransactionType;

/**
 * Database implementation for "ejb3" hibernate configurations.
 */
public class HibernateEjb3Database extends HibernateDatabase {

    protected EntityManagerFactory entityManagerFactory;

    @Override
    public String getShortName() {
        return "hibernateEjb3";
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return "Hibernate EJB3";
    }


    @Override
    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return conn.getURL().startsWith("hibernate:ejb3:");
    }

    /**
     * Calls {@link #createEntityManagerFactory()} to create and save the entity manager factory.
     */
    protected Metadata buildMetadataFromPath() throws DatabaseException {
        this.entityManagerFactory = createEntityManagerFactory();

        return super.buildMetadataFromPath();
    }

    protected EntityManagerFactory createEntityManagerFactory() {
        MyHibernatePersistenceProvider persistenceProvider = new MyHibernatePersistenceProvider();

        Map properties = new HashMap();
        properties.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, Boolean.FALSE.toString());

        final EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) persistenceProvider.getEntityManagerFactoryBuilderOrNull(getHibernateConnection().getPath(), properties, null);
        return builder.build();
    }

    @Override
    public String getProperty(String name) {
        String property = null;
        if (entityManagerFactory != null) {
            property = (String) entityManagerFactory.getProperties().get(name);
        }

        if (property == null) {
            return super.getProperty(name);
        } else {
            return property;
        }

    }

    @Override
    protected String findDialectName() {
        String dialectName = super.findDialectName();
        if (dialectName != null) {
            return dialectName;
        }

        return (String) entityManagerFactory.getProperties().get(AvailableSettings.DIALECT);
    }

    /**
     * Adds sources based on what is in the saved entityManagerFactory
     */
    @Override
    protected void configureSources(MetadataSources sources) throws DatabaseException {
        for (ManagedType<?> managedType : entityManagerFactory.getMetamodel().getManagedTypes()) {
            Class<?> javaType = managedType.getJavaType();
            if (javaType == null) {
                continue;
            }
            sources.addAnnotatedClass(javaType);
        }

        Package[] packages = Package.getPackages();
        for (Package p : packages) {
            sources.addPackage(p);
        }
    }

    private static class MyHibernatePersistenceProvider extends HibernatePersistenceProvider {

        private void setField(final Object obj, String fieldName, final Object value) throws Exception {
            final Field declaredField;

            declaredField = obj.getClass().getDeclaredField(fieldName);
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    boolean wasAccessible = declaredField.isAccessible();
                    try {
                        declaredField.setAccessible(true);
                        declaredField.set(obj, value);
                        return null;
                    } catch (Exception ex) {
                        throw new IllegalStateException("Cannot invoke method get", ex);
                    } finally {
                        declaredField.setAccessible(wasAccessible);
                    }
                }
            });
        }

        @Override
        protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map properties, ClassLoader providedClassLoader) {
            return super.getEntityManagerFactoryBuilderOrNull(persistenceUnitName, properties, providedClassLoader);
        }

        @Override
        protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(PersistenceUnitDescriptor persistenceUnitDescriptor, Map integration, ClassLoader providedClassLoader) {
            try {
                setField(persistenceUnitDescriptor, "jtaDataSource", null);
                setField(persistenceUnitDescriptor, "transactionType", PersistenceUnitTransactionType.RESOURCE_LOCAL);
            } catch (Exception ex) {
                LogFactory.getInstance().getLog().severe(null, ex);
            }
            return super.getEntityManagerFactoryBuilder(persistenceUnitDescriptor, integration, providedClassLoader);
        }
    }
}