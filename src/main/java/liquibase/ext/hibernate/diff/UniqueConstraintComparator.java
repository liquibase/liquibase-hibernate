package liquibase.ext.hibernate.diff;

import liquibase.database.Database;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.compare.DatabaseObjectComparator;
import liquibase.diff.compare.DatabaseObjectComparatorChain;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.UniqueConstraint;

public class UniqueConstraintComparator implements DatabaseObjectComparator {
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
	if (UniqueConstraint.class.isAssignableFrom(objectType)) {
	    return PRIORITY_TYPE;
	}
	return PRIORITY_NONE;
    }

    public boolean isSameObject(DatabaseObject databaseObject1, DatabaseObject databaseObject2, Database accordingTo, DatabaseObjectComparatorChain chain) {
	if (databaseObject1.getAttribute("hibernate", String.class) == null && databaseObject2.getAttribute("hibernate", String.class) == null)
	    return chain.isSameObject(databaseObject1, databaseObject2, accordingTo);

	if (!(databaseObject1 instanceof UniqueConstraint && databaseObject2 instanceof UniqueConstraint)) {
	    return false;
	}
	UniqueConstraint uniqueConstraint1 = (UniqueConstraint) databaseObject1;
	UniqueConstraint uniqueConstraint2 = (UniqueConstraint) databaseObject2;

	String uniqueConstraint1String = uniqueConstraint1.getTable().getName() + "(" + uniqueConstraint1.getColumnNames() + ")";
	String uniqueConstraint2String = uniqueConstraint2.getTable().getName() + "(" + uniqueConstraint2.getColumnNames() + ")";

	if (accordingTo.isCaseSensitive())
	    return uniqueConstraint1String.equals(uniqueConstraint2String);
	else
	    return uniqueConstraint1String.equalsIgnoreCase(uniqueConstraint2String);
    }

    public ObjectDifferences findDifferences(DatabaseObject databaseObject1, DatabaseObject databaseObject2, Database accordingTo, DatabaseObjectComparatorChain chain) {
	if (databaseObject1.getAttribute("hibernate", String.class) == null && databaseObject2.getAttribute("hibernate", String.class) == null)
	    return chain.findDifferences(databaseObject1, databaseObject2, accordingTo);
	ObjectDifferences differences = chain.findDifferences(databaseObject1, databaseObject2, accordingTo);
	differences.removeDifference("name");

	return differences;
    }
}
