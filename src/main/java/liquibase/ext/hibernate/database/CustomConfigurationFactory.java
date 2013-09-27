package liquibase.ext.hibernate.database;

import org.hibernate.cfg.Configuration;

public interface CustomConfigurationFactory {

    Configuration getConfiguration(ConfigLocator locator);

}
