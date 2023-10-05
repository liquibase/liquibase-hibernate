# Liquibase Hibernate Integration [![Build and Test Extension](https://github.com/liquibase/liquibase-hibernate/actions/workflows/build.yml/badge.svg)](https://github.com/liquibase/liquibase-hibernate/actions/workflows/build.yml)

This is a Liquibase extension for connecting to Hibernate. The extension lets you use your Hibernate configuration as a comparison database for diff, diffChangeLog, and generateChangeLog in Liquibase.

## Configuring the extension

These instructions will help you get the extension up and running on your local machine for development and testing purposes. This extension has a prerequisite of Liquibase core in order to use it. Liquibase core can be found at https://www.liquibase.org/download.

### Compatiblity

The Liquibase Hibernate extension requires Liquibase 4.x and Java 1.8+. Use `liquibase-hibernate5.jar` or `liquibase-hibernate6.jar` depending on your Hibernate version.
Ideally the extension version should be the same one as Liquibase version.

This extension can be used with any method of running Liquibase (Command line, Gradle, Maven, Ant, and others.)

### Liquibase CLI

Download [the latest released Liquibase extension](https://github.com/liquibase/liquibase-hibernate/releases) `.jar` file and place it in the `liquibase/lib` install directory. If you want to use another location, specify the extension `.jar` file in the `classpath` of your [liquibase.properties file](https://docs.liquibase.com/workflows/liquibase-community/creating-config-properties.html).

## Maven

This extension is available in the maven repository under group __org.liquibase.ext__, artifacts:

* __liquibase-hibernate6__ Hibernate 6.0+ support
* __liquibase-hibernate5__ Hibernate 5.0+ support

Specify the Liquibase extension in the `<dependency>` section of your POM file by adding the `org.liquibase.ext` dependency for the Liquibase plugin. 
 
```  
<plugin>
     <!--start with basic information to get Liquibase plugin:
     include <groupId>, <artifactID>, and <version> elements-->
     <groupId>org.liquibase</groupId>
     <artifactId>liquibase-maven-plugin</artifactId>
     <version>4.19.0</version>
     <configuration>
        <!--set values for Liquibase properties and settings
        for example, the location of a properties file to use-->
        <propertyFile>liquibase.properties</propertyFile>
     </configuration>
     <dependencies>
     <!--set up any dependencies for Liquibase to function in your
     environment for example, a database-specific plugin-->
            <dependency>
                 <groupId>org.liquibase.ext</groupId>
                 <artifactId>liquibase-hibernate</artifactId>
                 <version>${liquibase-hibernate.version}</version>
            </dependency>
         </dependencies>
      </plugin>
  ``` 

## Contribution

To file a bug, improve documentation, or contribute code, follow our [guidelines for contributing](https://www.liquibase.org/community). 

[This step-by-step instructions](https://www.liquibase.org/community/contribute/code) will help you contribute code for the extension. 

Once you have created a PR for this extension you can find the artifact for your build using the following link: [https://github.com/liquibase/liquibase-hibernate/actions/workflows/build.yml](https://github.com/liquibase/liquibase-hibernate/actions/workflows/build.yml).

## Hibernate 5 vs. Hibernate 6

The master branch is compatible with Hibernate 6+.
The `hibernate5` branch is compatible with Hibernate 5.6+

Ideally changes should go into the `hibernate5` branch and then be merged into master in order to support Hibernate 5 and 6.

## Documentation

[Using Liquibase with Hibernate](https://docs.liquibase.com/workflows/database-setup-tutorials/hibernate.html)

## More Information

For more information, see the [project wiki](https://github.com/liquibase/liquibase-hibernate/wiki/).

## Issue Tracking

Any issues can be logged in the [Github issue tracker](https://github.com/liquibase/liquibase-hibernate/issues).

## License

This project is licensed under the [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

## Using Liquibase Test Harness' Diff test
Liquibase's Hibernate extension uses [Liquibase Test Harness](https://github.com/liquibase/liquibase-test-harness) for integration testing.


The `HibernateDiffCommandTest` class extends the `DiffCommandTest` class from Test Harness and utilizes the ability to check differences between two databases.
As Hibernate is not a Relational database, this is our method of checking that database objects generated/updated by the Liquibase Hibernate extension against a database have the correct attributes. 

In general the `DiffCommandTest` works by utilizing the Liquibase diff command to check 
differences between two databases, then it creates a changelog file based on diff, then it applies these changes to the target database and checks the diff again. 
There still could be some differences afterwards as different DBs support different features, so while checking diffs again the test will ignore diffs that are expected.

### Configurations for this test are hosted in 2 files:
 * `src/test/resources/harness-config.yml` -- this is a general config file for Test Harness where DB connection details are specified.
 * `src/test/resources/liquibase/harness/diff/diffDatabases.yml` -- this file specifies which DBs should be compared and what the diffs are expected even after we try to bring the target DB to same state as the reference DB.

The `DiffCommandTest` will take all pairs of targetDB-referenceDB from `diffDatabases.yml` . The test also takes the `*.cfg.xml` configuration files into consideration. And then the paths to these config files work as the DB connection URLs in the `harness-config.yml` file.
