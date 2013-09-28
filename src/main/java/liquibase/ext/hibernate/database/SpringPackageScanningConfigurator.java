package liquibase.ext.hibernate.database;

import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.Ejb3Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

public class SpringPackageScanningConfigurator {

    private static final Logger LOG = LoggerFactory
            .getLogger(SpringConfigurator.class);

    private final ConfigLocator locator;

    public SpringPackageScanningConfigurator(ConfigLocator locator) {
        this.locator = locator;
    }

    public Configuration createSpringConfiguration() {
        String[] packagesToScan = locator.getPath().split(",");

        for (String packageName : packagesToScan) {
            LOG.info("Found package {}", packageName);
        }

        DefaultPersistenceUnitManager internalPersistenceUnitManager = new DefaultPersistenceUnitManager();

        internalPersistenceUnitManager.setPackagesToScan(packagesToScan);

        String dialectName = locator.getProperties().getProperty("dialect",
                null);
        if (dialectName == null) {
            throw new IllegalArgumentException(
                    "A 'dialect' has to be specified.");
        }
        LOG.info("Found dialect {}", dialectName);

        internalPersistenceUnitManager.preparePersistenceUnitInfos();
        PersistenceUnitInfo persistenceUnitInfo = internalPersistenceUnitManager
                .obtainDefaultPersistenceUnitInfo();
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setDatabasePlatform(dialectName);
        if (jpaVendorAdapter != null
                && persistenceUnitInfo instanceof SmartPersistenceUnitInfo) {
            ((SmartPersistenceUnitInfo) persistenceUnitInfo)
                    .setPersistenceProviderPackageName(jpaVendorAdapter
                            .getPersistenceProviderRootPackage());
        }

        Ejb3Configuration configured = new Ejb3Configuration().configure(
                persistenceUnitInfo, jpaVendorAdapter.getJpaPropertyMap());

        Configuration configuration = configured.getHibernateConfiguration();
        configuration.buildMappings();
        return configuration;
    }

}
