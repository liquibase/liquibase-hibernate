package liquibase.ext.hibernate.snapshot.extension;

public interface ExtensibleSnapshotGenerator<T, U> {

    U snapshot(T object);

    boolean supports(T object);

}
