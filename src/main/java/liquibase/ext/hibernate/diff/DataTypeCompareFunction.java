package liquibase.ext.hibernate.diff;

import liquibase.database.Database;

class DataTypeCompareFunction extends ToStringCompareFunction {

    public DataTypeCompareFunction(Database accordingTo) {
	super(accordingTo);
    }

    @Override
    public boolean areEqual(Object referenceValue, Object compareToValue) {
	if (referenceValue == null && compareToValue == null) {
	    return true;
	}
	if (referenceValue == null || compareToValue == null) {
	    return false;
	}

	String referenceTypeString = referenceValue.toString().toUpperCase();
	String compareToTypeString = compareToValue.toString().toUpperCase();

	if ((referenceTypeString.contains("(") || compareToTypeString.contains("(")) && (!referenceTypeString.contains("(") || !compareToTypeString.contains("("))) {
	    if (referenceTypeString.contains("("))
		referenceTypeString = referenceTypeString.substring(0, referenceTypeString.indexOf('('));
	    if (compareToTypeString.contains("("))
		compareToTypeString = compareToTypeString.substring(0, compareToTypeString.indexOf('('));
	}

	// CHAR = CHARACTER
	if (referenceTypeString.startsWith("CHARACTER"))
	    referenceTypeString = referenceTypeString.replaceFirst("CHARACTER", "CHAR");
	if (compareToTypeString.startsWith("CHARACTER"))
	    compareToTypeString = compareToTypeString.replaceFirst("CHARACTER", "CHAR");

	return super.areEqual(referenceTypeString, compareToTypeString);
    }

}