package pl.aitwar.auriga.collection.model.exceptions;

public class DocumentAllocationException extends Exception {
    public DocumentAllocationException(final String collection, final String node) {
        super("Document from  '" + collection + "' failed to allocate at '" + node + "'");
    }
}

