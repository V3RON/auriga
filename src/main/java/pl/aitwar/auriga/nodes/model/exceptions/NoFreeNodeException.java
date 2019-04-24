package pl.aitwar.auriga.nodes.model.exceptions;

public class NoFreeNodeException extends Exception {
    public NoFreeNodeException() {
        super("There is no free node");
    }
}
