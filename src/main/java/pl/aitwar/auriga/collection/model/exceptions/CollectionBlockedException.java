package pl.aitwar.auriga.collection.model.exceptions;

public class CollectionBlockedException extends Exception {
    public CollectionBlockedException(final String collection) {
        super("Collection '" + collection + "' is blocked");
    }
}

