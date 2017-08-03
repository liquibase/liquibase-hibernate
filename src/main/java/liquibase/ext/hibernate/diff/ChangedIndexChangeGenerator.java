package liquibase.ext.hibernate.diff;

import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Index;

/**
 * Hibernate doesn't know about all the variations that occur with index constraint but just whether they exists or not.
 * To prevent changing customized constraints, we suppress all changes with hibernate.
 */

public class ChangedIndexChangeGenerator extends
        liquibase.diff.output.changelog.core.ChangedIndexChangeGenerator {

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (Index.class.isAssignableFrom(objectType)) {
            return PRIORITY_ADDITIONAL;
        }
        return PRIORITY_NONE;
    }

    @Override
    public Change[] fixChanged(DatabaseObject changedObject, ObjectDifferences differences,
                               DiffOutputControl control, Database referenceDatabase, Database comparisonDatabase,
                               ChangeGeneratorChain chain) {
        if (referenceDatabase instanceof HibernateDatabase || comparisonDatabase instanceof HibernateDatabase) {
            Difference unique = differences.getDifference("unique");
            if ( unique != null && isNullOrFalse( unique.getReferenceValue() ) && isNullOrFalse( unique.getComparedValue() ) )
            {
                differences.removeDifference("unique");
                if (!differences.hasDifferences()) {
                    return null;
                }
            }
        }
        return super.fixChanged(changedObject, differences, control, referenceDatabase, comparisonDatabase, chain);
    }
        
    private boolean isNullOrFalse( Object value ) {
        return value == null || !(Boolean)value;
    }
}
