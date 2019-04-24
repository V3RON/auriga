package pl.aitwar.auriga.nodes.model.exceptions;

public class UnknownNodeException extends Exception {
    public UnknownNodeException(String node) {
        super("Node '" + node + "' not found");
    }
}
