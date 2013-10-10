package com.example.customconfig;

import com.example.customconfig.auction.Item;
import liquibase.ext.hibernate.customfactory.CustomClassicConfigurationFactory;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import org.hibernate.cfg.Configuration;

public class CustomClassicConfigurationFactoryImpl implements CustomClassicConfigurationFactory {

    @Override
    public Configuration getConfiguration(HibernateDatabase hibernateDatabase, HibernateConnection connection) {
        Configuration config = new Configuration();
        config.addAnnotatedClass(Item.class);
        return config;
    }

}
