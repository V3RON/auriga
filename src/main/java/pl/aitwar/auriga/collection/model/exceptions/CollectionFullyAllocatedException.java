package pl.aitwar.auriga.collection.model.exceptions;

public class CollectionFullyAllocatedException extends Exception {
    public CollectionFullyAllocatedException(final String collection) {
        super("Collection '" + collection + "' is fully allocated");
    }
}

