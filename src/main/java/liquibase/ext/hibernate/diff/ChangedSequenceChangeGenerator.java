package liquibase.ext.hibernate.diff;

import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Sequence;

/**
 * startValue and incrementBy values are retrieved by {@link liquibase.ext.hibernate.snapshot.SequenceSnapshotGenerator},
 * but it may not be the case for {@link liquibase.snapshot.jvm.SequenceSnapshotGenerator}, so we should drop
 * differences where compared value is null.
 */
public class ChangedSequenceChangeGenerator extends liquibase.diff.output.changelog.core.ChangedSequenceChangeGenerator {

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (Sequence.class.isAssignableFrom(objectType)) {
            return PRIORITY_ADDITIONAL;
        }
        return PRIORITY_NONE;
    }

    /**
     * Remove a difference if one of it's value is null.
     *
     * @param differences
     * @param difference
     * @return
     */
    private boolean removeIfNull(ObjectDifferences differences, Difference difference) {
        if (difference.getComparedValue() == null || difference.getReferenceValue() == null) {
            return differences.removeDifference(difference.getField());
        }
        return false;
    }


    @Override
    public Change[] fixChanged(DatabaseObject changedObject, ObjectDifferences differences, DiffOutputControl control, Database referenceDatabase, Database comparisonDatabase, ChangeGeneratorChain chain) {
        if (referenceDatabase instanceof HibernateDatabase || comparisonDatabase instanceof HibernateDatabase) {
            for (Difference difference : differences.getDifferences()) {
                removeIfNull(differences, difference);
            }
        }

        return super.fixChanged(changedObject, differences, control, referenceDatabase, comparisonDatabase, chain);
    }
}
