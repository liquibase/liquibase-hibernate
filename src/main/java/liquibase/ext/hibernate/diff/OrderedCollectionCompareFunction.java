package liquibase.ext.hibernate.diff;

import liquibase.diff.ObjectDifferences.CompareFunction;
import liquibase.diff.ObjectDifferences.StandardCompareFunction;
import liquibase.structure.DatabaseObject;

import java.util.Collection;
import java.util.Iterator;

public class OrderedCollectionCompareFunction implements CompareFunction {

    private StandardCompareFunction compareFunction;

    public OrderedCollectionCompareFunction(StandardCompareFunction compareFunction) {
        this.compareFunction = compareFunction;
    }

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

        Iterator referenceIterator = ((Collection) referenceValue).iterator();
        Iterator compareIterator = ((Collection) compareToValue).iterator();

        while (referenceIterator.hasNext()) {
            Object referenceObj = referenceIterator.next();
            Object compareObj = compareIterator.next();

            if (referenceObj instanceof DatabaseObject) {
                if (!compareFunction.areEqual(referenceObj, compareObj)) {
                    return false;
                }
            }
        }

        return true;

    }
}