package liquibase.ext.hibernate.diff;

import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.output.DiffOutputControl;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.statement.DatabaseFunction;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.DataType;

import java.util.List;

/**
 * Hibernate and database types tend to look different even though they are not.
 * There are enough false positives that it works much better to suppress all column changes based on types.
 */
public class ChangedColumnChangeGenerator extends liquibase.diff.output.changelog.core.ChangedColumnChangeGenerator {

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (Column.class.isAssignableFrom(objectType)) {
            return PRIORITY_ADDITIONAL;
        }
        return PRIORITY_NONE;
    }

    @Override
    protected void handleTypeDifferences(Column column, ObjectDifferences differences, DiffOutputControl control, List<Change> changes, Database referenceDatabase, Database comparisonDatabase) {
        if (referenceDatabase instanceof HibernateDatabase || comparisonDatabase instanceof HibernateDatabase) {
            handleSizeChange(column, differences, control, changes, referenceDatabase, comparisonDatabase);
        } else {
            super.handleTypeDifferences(column, differences, control, changes, referenceDatabase, comparisonDatabase);
        }
    }

    private void handleSizeChange(Column column, ObjectDifferences differences, DiffOutputControl control, List<Change> changes, Database referenceDatabase, Database comparisonDatabase) {
        Difference difference = differences.getDifference("type");
        if (difference != null) {
            for (Difference d : differences.getDifferences()) {
                if (!(d.getReferenceValue() instanceof DataType)) {
                    differences.removeDifference(d.getField());
                    continue;
                }
                int originalSize = ((DataType) d.getReferenceValue()).getColumnSize();
                int newSize = ((DataType) d.getComparedValue()).getColumnSize();
                if (newSize == originalSize) {
                    differences.removeDifference(d.getField());
                }
            }
            super.handleTypeDifferences(column, differences, control, changes, referenceDatabase, comparisonDatabase);
        }
    }

    @Override
    protected void handleDefaultValueDifferences(Column column, ObjectDifferences differences, DiffOutputControl control, List<Change> changes, Database referenceDatabase, Database comparisonDatabase) {
        if (referenceDatabase instanceof HibernateDatabase || comparisonDatabase instanceof HibernateDatabase) {
            Difference difference = differences.getDifference("defaultValue");
            if (difference != null && difference.getReferenceValue() == null && difference.getComparedValue() instanceof DatabaseFunction) {
                //database sometimes adds a function default value, like for timestamp columns
                return;
            }
            difference = differences.getDifference("defaultValue");
            if (difference != null) {
                super.handleDefaultValueDifferences(column, differences, control, changes, referenceDatabase, comparisonDatabase);
            }
            // do nothing, types tend to not match with hibernate
        }
        super.handleDefaultValueDifferences(column, differences, control, changes, referenceDatabase, comparisonDatabase);
    }
}
