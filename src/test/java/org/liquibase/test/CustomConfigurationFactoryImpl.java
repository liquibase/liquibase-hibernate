package org.liquibase.test;

import org.hibernate.cfg.Configuration;

import liquibase.ext.hibernate.database.ConfigLocator;
import liquibase.ext.hibernate.database.CustomConfigurationFactory;

public class CustomConfigurationFactoryImpl implements CustomConfigurationFactory {

    @Override
    public Configuration getConfiguration(ConfigLocator locator) {
        Configuration config = new Configuration();
        config.addAnnotatedClass(Item.class);
        return config;
    }

}
