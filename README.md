# Liquibase Hibernate Integration [![Build and Test Extension](https://github.com/liquibase/liquibase-hibernate/actions/workflows/build-nightly.yml/badge.svg)](https://github.com/liquibase/liquibase-hibernate/actions/workflows/build.yml)

This is a Liquibase extension for connecting to Hibernate. The extension lets you use your Hibernate configuration as a comparison database for diff, diffChangeLog, and generateChangeLog in Liquibase.

## Configuring the extension

These instructions will help you get the extension up and running on your local machine for development and testing purposes. This extension has a prerequisite of Liquibase core in order to use it. Liquibase core can be found at https://www.liquibase.org/download.

### Compatibility

**Important:** The extension version must match your Hibernate major version. You cannot mix and match plugins across different Hibernate versions.

| Hibernate | Liquibase Core | Java | Branch | Artifact ID | Support Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **7.x** | **4.x (OSS)** | **17+** | `liquibase-hibernate7-oss` | `liquibase-hibernate7-oss` | **Active (Stability/Grails Track)** |
| **7.x** | **5.x** | 17+ | `main` | `liquibase-hibernate7` | Active (Modern/Default Track) |
| **6.x** | 4.x | 11+ | `hibernate6` | `liquibase-hibernate6` | Legacy / Maintenance |

This extension can be used with any method of running Liquibase (Command line, Gradle, Maven, Ant, and others.)

### Liquibase CLI

Download [the latest released Liquibase extension](https://github.com/liquibase/liquibase-hibernate/releases) `.jar` file and place it in the `liquibase/lib` install directory.

## Maven

This extension is available in the maven repository under group __org.liquibase.ext__. You must use the specific `artifactId` that corresponds to your environment:

* __liquibase-hibernate7__: Use this for **Hibernate 7** when running on **Liquibase 5.x**.
* __liquibase-hibernate7-oss__: Use this for **Hibernate 7** when running on **Liquibase 4.x**.
* __liquibase-hibernate6__: Use this for **Hibernate 6** environments.

```xml
<plugin>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-maven-plugin</artifactId>
    <version>${liquibase.version}</version>
    <configuration>
        <propertyFile>liquibase.properties</propertyFile>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>org.liquibase.ext</groupId>
            <artifactId>${liquibase-hibernate.artifactId}</artifactId>
            <version>${liquibase-hibernate.version}</version>
        </dependency>
    </dependencies>
</plugin>