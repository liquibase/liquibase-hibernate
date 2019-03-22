package liquibase.ext.hibernate.snapshot;

import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Sequence;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.SequenceGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import java.math.BigInteger;
import java.util.Iterator;

/**
 * Sequence snapshots are not yet supported, but this class needs to be implemented in order to prevent the default SequenceSnapshotGenerator from running.
 */
public class SequenceSnapshotGenerator extends HibernateSnapshotGenerator {

    public SequenceSnapshotGenerator() {
        super(Sequence.class, new Class[]{Schema.class});
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        return example;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(Sequence.class)) {
            return;
        }

        if (foundObject instanceof Schema) {

            Schema schema = (Schema) foundObject;
            HibernateDatabase database = (HibernateDatabase) snapshot.getDatabase();
            MetadataImplementor metadata = (MetadataImplementor) database.getMetadata();
            Iterator<PersistentClass> classMappings = metadata.getEntityBindings().iterator();

            while (classMappings.hasNext()) {
                PersistentClass persistentClass = (PersistentClass) classMappings
                        .next();
                if (!persistentClass.isInherited()) {
                    IdentifierGenerator ig = persistentClass.getIdentifier().createIdentifierGenerator(
                            metadata.getIdentifierGeneratorFactory(),
                            database.getDialect(),
                            null,
                            null,
                            (RootClass) persistentClass
                    );
                    if (ig instanceof SequenceGenerator) {
                        SequenceGenerator sequenceGenerator = (SequenceGenerator) ig;
                        createSequence(sequenceGenerator.getSequenceName(), schema);
                    } else if (ig instanceof SequenceStyleGenerator) {
                        SequenceStyleGenerator sequenceGenerator = (SequenceStyleGenerator) ig;
                        createSequence((String) sequenceGenerator.generatorKey(),
                                sequenceGenerator.getDatabaseStructure().getInitialValue(),
                                sequenceGenerator.getDatabaseStructure().getIncrementSize(), schema);
                    }
                }

            }
        }
    }

    private void createSequence(String sequenceName, Integer initialValue, Integer incrementBy, Schema schema) {
        Sequence sequence = new Sequence();
        sequence.setName(sequenceName);
        sequence.setSchema(schema);
        if(initialValue != null) {
            sequence.setStartValue(BigInteger.valueOf(initialValue));
        }
        if(incrementBy != null) {
            sequence.setIncrementBy(BigInteger.valueOf(incrementBy));
        }
        schema.addDatabaseObject(sequence);
    }

    private void createSequence(String sequenceName, Schema schema) {
        createSequence(sequenceName, null, null, schema);
    }

}
