package liquibase.ext.hibernate.diff;

import liquibase.database.Database;
import liquibase.structure.core.PrimaryKey;

public class PrimaryKeyCompareFunction extends ToStringCompareFunction {

    public PrimaryKeyCompareFunction(Database accordingTo) {
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
        PrimaryKey referencePK = (PrimaryKey) referenceValue;
        PrimaryKey compareToPK = (PrimaryKey) compareToValue;
        return super.areEqual(referencePK.getColumnNames(), compareToPK.getColumnNames());
    }

}
