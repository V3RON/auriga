package pl.aitwar.auriga.nodes.model.exceptions;

public class NodeAlreadyRegisteredException extends Exception {
    public NodeAlreadyRegisteredException(String node) {
        super("Node '" + node + "' has been already registered");
    }
}
