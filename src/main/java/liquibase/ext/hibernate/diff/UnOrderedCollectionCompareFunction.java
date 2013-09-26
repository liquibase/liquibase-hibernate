package liquibase.ext.hibernate.diff;

import liquibase.database.Database;
import liquibase.structure.core.UniqueConstraint;

import java.util.Collection;
import java.util.Iterator;

class UnOrderedCollectionCompareFunction extends ToStringCompareFunction {

    public UnOrderedCollectionCompareFunction(Database accordingTo) {
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

        if (!(referenceValue instanceof Collection) || (!(compareToValue instanceof Collection))) {
            return false;
        }

        if (((Collection) referenceValue).size() != ((Collection) compareToValue).size()) {
            return false;
        }

        Object firstObject = ((Collection) referenceValue).iterator().next();

        Iterator referenceIterator = ((Collection) referenceValue).iterator();
        while (referenceIterator.hasNext()) {
            int found = 0;
            Object referenceObj = referenceIterator.next();
            Iterator compareIterator = ((Collection) compareToValue).iterator();
            while (compareIterator.hasNext()) {
                Object compareObj = compareIterator.next();
                if (referenceObj instanceof UniqueConstraint && compareObj instanceof UniqueConstraint) {
                    if (((UniqueConstraint) referenceObj).getTable().getName().equalsIgnoreCase(((UniqueConstraint) compareObj).getTable().getName())
                            && (((UniqueConstraint) referenceObj).getColumnNames().equalsIgnoreCase(((UniqueConstraint) compareObj).getColumnNames())))
                        found++;
                } else {
                    if (compareObj.equals(referenceObj))
                        found++;
                }
            }
            if (found != 1)
                return false;
        }

        return true;

    }

}