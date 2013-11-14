# Liquibase Hibernate Integration

This extension lets you use your Hibernate configuration as a comparison database for diff, diffChangeLog and generateChangeLog in Liquibase.

## Download

Download the extension from the [project releases tab](https://github.com/liquibase/liquibase-hibernate/releases)

## Maven

This extension is available in the maven repository under group org.liquibase.ext, artifacts liquibase-hibernate3 and liquibase-hibernate4

## More Information

For more information, see the [project wiki](https://github.com/liquibase/liquibase-hibernate/wiki/)

## Issue Tracking

Any issues can be logged in the [Github issue tracker](https://github.com/liquibase/liquibase-hibernate/issues)

## Hibernate 3 vs. Hibernate 4

The master branch is Hibernate 3 compatible. For Hibernate 4 support, use the master branch.

Ideally changes should go into the hibernate3 branch and then be merged into master in order to support both Hibernate 3 and 4.