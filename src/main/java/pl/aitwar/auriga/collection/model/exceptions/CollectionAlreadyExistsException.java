package pl.aitwar.auriga.collection.model.exceptions;

public class CollectionAlreadyExistsException extends Exception {
    public CollectionAlreadyExistsException(final String collection) {
        super("Collection '" + collection + "' already exists");
    }
}
