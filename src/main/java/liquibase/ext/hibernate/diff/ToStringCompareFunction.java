package liquibase.ext.hibernate.diff;

import liquibase.database.Database;
import liquibase.diff.ObjectDifferences;

class ToStringCompareFunction extends ObjectDifferences.StandardCompareFunction {

    private final boolean caseSensitive;

    public ToStringCompareFunction(Database accordingTo) {
	caseSensitive = accordingTo.isCaseSensitive();
    }

    @Override
    public boolean areEqual(Object referenceValue, Object compareToValue) {
	if (referenceValue == null && compareToValue == null) {
	    return true;
	}
	if (referenceValue == null || compareToValue == null) {
	    return false;
	}

	if (caseSensitive)
	    return referenceValue.toString().equals(compareToValue.toString());
	else
	    return referenceValue.toString().equalsIgnoreCase(compareToValue.toString());
    }
}