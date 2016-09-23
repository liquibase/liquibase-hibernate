package liquibase.ext.hibernate.database;

import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.connection.HibernateDriver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import java.util.Collections;

/**
 * Database implementation for JPA configurations.
 * This supports passing a JPA persistence XML file reference.
 */
public class JpaPersistenceDatabase extends HibernateEjb3Database {

    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return conn.getURL().startsWith("jpa:persistence:");
    }

    @Override
    public String getDefaultDriver(String url) {
        if (url.startsWith("jpa:persistence:")) {
            return HibernateDriver.class.getName();
        }
        return null;
    }

    @Override
    public String getShortName() {
        return "jpaPersistence";
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return "JPA Persistence";
    }

    @Override
    protected EntityManagerFactory createEntityManagerFactory() {
        DefaultPersistenceUnitManager internalPersistenceUnitManager = new DefaultPersistenceUnitManager();

        internalPersistenceUnitManager.setPersistenceXmlLocation(getHibernateConnection().getPath());
        internalPersistenceUnitManager.setDefaultPersistenceUnitRootLocation(null);

        internalPersistenceUnitManager.preparePersistenceUnitInfos();
        PersistenceUnitInfo persistenceUnitInfo = internalPersistenceUnitManager.obtainDefaultPersistenceUnitInfo();

        EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(persistenceUnitInfo, Collections.emptyMap());
        return builder.build();
    }

}
