package liquibase.ext.hibernate.diff;

import liquibase.database.Database;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.compare.DatabaseObjectComparator;
import liquibase.diff.compare.DatabaseObjectComparatorChain;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.DataType;
import liquibase.structure.core.ForeignKey;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.UniqueConstraint;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class DefaultDatabaseObjectComparator implements DatabaseObjectComparator {

    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof HibernateDatabase) {
            return PRIORITY_DEFAULT + 1;
        } else {
            return PRIORITY_NONE;
        }
    }

    @Override
    public String[] hash(DatabaseObject databaseObject, Database accordingTo, DatabaseObjectComparatorChain chain) {
        return chain.hash(databaseObject, accordingTo);
    }

    public boolean isSameObject(DatabaseObject databaseObject1, DatabaseObject databaseObject2, Database accordingTo, DatabaseObjectComparatorChain chain) {
        if (databaseObject1.getAttribute("hibernate", String.class) == null && databaseObject2.getAttribute("hibernate", String.class) == null)
            return chain.isSameObject(databaseObject1, databaseObject2, accordingTo);
        if (databaseObject1.getClass().isAssignableFrom(databaseObject2.getClass()) || databaseObject2.getClass().isAssignableFrom(databaseObject1.getClass())) {
            return nameMatches(databaseObject1, databaseObject2, accordingTo);
        }
        return false;

    }

    @Override
    public ObjectDifferences findDifferences(DatabaseObject databaseObject1, DatabaseObject databaseObject2, Database accordingTo, CompareControl compareControl, DatabaseObjectComparatorChain chain, Set<String> exclude) {

        if (databaseObject1.getAttribute("hibernate", String.class) == null && databaseObject2.getAttribute("hibernate", String.class) == null)
            return chain.findDifferences(databaseObject1, databaseObject2, accordingTo, compareControl, exclude);

        Set<String> attributes = new HashSet<String>();
        attributes.addAll(databaseObject1.getAttributes());
        attributes.addAll(databaseObject2.getAttributes());

        ObjectDifferences differences = new ObjectDifferences(compareControl);

        for (String attribute : attributes) {
            Object attribute1 = databaseObject1.getAttribute(attribute, Object.class);
            Object attribute2 = databaseObject2.getAttribute(attribute, Object.class);

            ObjectDifferences.CompareFunction compareFunction;
            if (attribute1 instanceof PrimaryKey || attribute2 instanceof PrimaryKey) {
                // Ignore primary key names
                compareFunction = new PrimaryKeyCompareFunction(accordingTo);
            } else if (attribute1 instanceof DatabaseObject || attribute2 instanceof DatabaseObject) {
                Class<? extends DatabaseObject> type;
                if (attribute1 != null) {
                    type = (Class<? extends DatabaseObject>) attribute1.getClass();
                } else {
                    type = (Class<? extends DatabaseObject>) attribute2.getClass();
                }
                compareFunction = new ObjectDifferences.DatabaseObjectNameCompareFunction(type, accordingTo);
            } else if (attribute1 instanceof DataType || attribute2 instanceof DataType) {
                // Ignore case and default length differences
                compareFunction = new DataTypeCompareFunction(accordingTo);
            } else if (attribute1 instanceof Collection || attribute2 instanceof Collection) {
                Collection collection1 = (Collection) attribute1;
                Object item1 = null;
                if (!collection1.isEmpty())
                    item1 = collection1.iterator().next();
                if (item1 != null && (item1 instanceof ForeignKey || item1 instanceof UniqueConstraint)) {
                    // Ignore order for foreign keys and unique constraints
                    compareFunction = new UnOrderedCollectionCompareFunction(accordingTo);
                } else {
                    compareFunction = new OrderedCollectionCompareFunction(new ToStringCompareFunction(accordingTo));
                }
            } else if ("name".equals(attribute) || "foreignKeyColumns".equals(attribute) || "primaryKeyColumns".equals(attribute)) {
                // Ignore case for object name and column names
                compareFunction = new ToStringCompareFunction(accordingTo);
            } else {
                compareFunction = new ObjectDifferences.StandardCompareFunction(accordingTo);
            }
            differences.compare(attribute, databaseObject1, databaseObject2, compareFunction);

        }

        return differences;
    }

    private boolean nameMatches(DatabaseObject databaseObject1, DatabaseObject databaseObject2, Database accordingTo) {
        String object1Name = accordingTo.correctObjectName(databaseObject1.getName(), databaseObject1.getClass());
        String object2Name = accordingTo.correctObjectName(databaseObject2.getName(), databaseObject2.getClass());

        if (object1Name == null && object2Name == null) {
            return true;
        }
        if (object1Name == null || object2Name == null) {
            return false;
        }
        if (accordingTo.isCaseSensitive()) {
            return object1Name.equals(object2Name);
        } else {
            return object1Name.equalsIgnoreCase(object2Name);
        }
    }

}
