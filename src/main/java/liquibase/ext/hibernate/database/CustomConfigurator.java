package liquibase.ext.hibernate.database;

import org.hibernate.cfg.Configuration;

public class CustomConfigurator {

    private ConfigLocator locator;

    public CustomConfigurator(ConfigLocator locator) {
        this.locator = locator;
    }

    public Configuration getConfiguration() {
        try {
            return ((CustomConfigurationFactory) Class.forName(this.locator.getPath()).newInstance()).getConfiguration(this.locator);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
