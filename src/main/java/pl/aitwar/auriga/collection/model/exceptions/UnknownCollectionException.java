package pl.aitwar.auriga.collection.model.exceptions;

public class UnknownCollectionException extends Exception {
    public UnknownCollectionException(final String collection) {
        super("Collection '" + collection + "' not found");
    }
}

