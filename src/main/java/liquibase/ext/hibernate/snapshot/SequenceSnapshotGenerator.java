package liquibase.ext.hibernate.snapshot;

import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.id.NativeGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * Snapshots sequences from Hibernate metadata, including sequences
 * managed by @NativeGenerator that are not registered in the relational namespace.
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

        if (foundObject instanceof Schema schema) {
            HibernateDatabase database = (HibernateDatabase) snapshot.getDatabase();
            Set<String> addedSequences = new HashSet<>();

            for (org.hibernate.boot.model.relational.Namespace namespace : database.getMetadata().getDatabase().getNamespaces()) {
                for (org.hibernate.boot.model.relational.Sequence sequence : namespace.getSequences()) {
                    String name = sequence.getName().getSequenceName().getText();
                    schema.addDatabaseObject(new Sequence()
                            .setName(name)
                            .setSchema(schema)
                            .setStartValue(BigInteger.valueOf(sequence.getInitialValue()))
                            .setIncrementBy(BigInteger.valueOf(sequence.getIncrementSize()))
                    );
                    addedSequences.add(name.toLowerCase());
                }
            }

            addNativeGeneratorSequences(database, schema, addedSequences);
        }
    }

    /**
     * Scans entity bindings for sequence-based generators.
     */
    private void addNativeGeneratorSequences(HibernateDatabase database, Schema schema, Set<String> addedSequences) {
        MetadataImplementor metadata = (MetadataImplementor) database.getMetadata();
        var dialect = database.getDialect();

        for (PersistentClass entityBinding : metadata.getEntityBindings()) {
            if (!(entityBinding instanceof RootClass rootClass)) {
                continue;
            }
            var identifier = rootClass.getIdentifier();
            if (!(identifier instanceof SimpleValue simpleValue)) {
                continue;
            }

            try {
                var memberDetails = simpleValue.getMemberDetails();
                // Detection of Generation Intent:
                // For annotation-based entities, only create a sequence snapshot if
                // @GeneratedValue is present. For XML-mapped entities (memberDetails
                // is null), always process since intent is declared in the mapping.
                if (memberDetails != null && !memberDetails.hasDirectAnnotationUsage(jakarta.persistence.GeneratedValue.class)) {
                    continue;
                }

                var identifierProperty = rootClass.getIdentifierProperty();
                var settings = createGeneratorSettings(simpleValue);
                var generator = simpleValue.createGenerator(dialect, rootClass, identifierProperty, settings);

                SequenceStyleGenerator seqGen = null;
                // NativeGenerator might wrap a SequenceStyleGenerator delegate depending on the dialect.
                if (generator instanceof NativeGenerator nativeGen) {
                    var delegate = getNativeGeneratorDelegate(nativeGen);
                    if (delegate instanceof SequenceStyleGenerator s) {
                        seqGen = s;
                    }
                } else if (generator instanceof SequenceStyleGenerator s) {
                    seqGen = s;
                }

                if (seqGen != null) {
                    var structure = seqGen.getDatabaseStructure();
                    if (structure != null && structure.getPhysicalName() != null) {
                        String name = structure.getPhysicalName().render();
                        if (!addedSequences.contains(name.toLowerCase())) {
                            // Final Liquibase Model Synthesis:
                            // If the logic resolved to a sequence, create a Sequence object
                            // in the Liquibase metadata for snapshot comparison.
                            schema.addDatabaseObject(new Sequence()
                                    .setName(name)
                                    .setSchema(schema)
                                    .setStartValue(BigInteger.valueOf(structure.getInitialValue()))
                                    .setIncrementBy(BigInteger.valueOf(structure.getIncrementSize()))
                            );
                            addedSequences.add(name.toLowerCase());
                        }
                    }
                }
            } catch (Exception e) {
                Scope.getCurrentScope().getLog(getClass()).fine(
                        "Could not resolve generator for " + rootClass.getEntityName(), e);
            }
        }
    }

    private org.hibernate.generator.Generator getNativeGeneratorDelegate(NativeGenerator nativeGen) {
        try {
            var field = NativeGenerator.class.getDeclaredField("dialectNativeGenerator");
            field.setAccessible(true);
            return (org.hibernate.generator.Generator) field.get(nativeGen);
        } catch (ReflectiveOperationException e) {
            Scope.getCurrentScope().getLog(getClass()).fine(
                    "Could not access NativeGenerator delegate", e);
            return null;
        }
    }

    private GeneratorSettings createGeneratorSettings(SimpleValue simpleValue) {
        var buildingContext = simpleValue.getBuildingContext();
        return new GeneratorSettings() {
            @Override
            public String getDefaultCatalog() {
                return null;
            }

            @Override
            public String getDefaultSchema() {
                return null;
            }

            @Override
            public SqlStringGenerationContext getSqlStringGenerationContext() {
                var db = buildingContext.getMetadataCollector().getDatabase();
                return SqlStringGenerationContextImpl.fromExplicit(
                        db.getJdbcEnvironment(), db, getDefaultCatalog(), getDefaultSchema());
            }
        };
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces() {
        return new Class[]{ liquibase.snapshot.jvm.SequenceSnapshotGenerator.class };
    }
}
