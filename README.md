# Liquibase Hibernate Integration

This extension lets you use your Hibernate configuration as a comparison database for diff, diffChangeLog and generateChangeLog in Liquibase.

## Download

Download the extension from the [project releases tab](https://github.com/liquibase/liquibase-hibernate/releases)

## Maven

This extension is available in the maven repository under group __org.liquibase.ext__, artifacts:

* __liquibase-hibernate4__ Hibernate 4.3+ support
* __liquibase-hibernate4.2__ Hibernate 4.0-4.2 support
* __liquibase-hibernate3__ Hibernate 3.x support

## More Information

For more information, see the [project wiki](https://github.com/liquibase/liquibase-hibernate/wiki/)

## Issue Tracking

Any issues can be logged in the [Github issue tracker](https://github.com/liquibase/liquibase-hibernate/issues)

## Hibernate 3 vs. Hibernate 4 vs. Hibernate 5

The master branch is Hibernate 5+ compatible.
The hibernate4 branch is Hibernate 4.3+ compatible.
For Hibernate 3 support, use the hibernate3 release and/or branch.
For hibernate 4.0-4.2 support, use the hibernate4.2 release and/or branch.

Ideally changes should go into the hibernate3 branch and then be merged into master in order to support both Hibernate 3 and 4.