package pl.aitwar.auriga.collection.model.exceptions;

public class CollectionAllocationException extends Exception {
    public CollectionAllocationException(final String collection, final String node) {
        super("Collection '" + collection + "' failed to allocate at '" + node + "'");
    }
}

