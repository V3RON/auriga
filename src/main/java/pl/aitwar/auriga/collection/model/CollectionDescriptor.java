package pl.aitwar.auriga.collection.model;

import lombok.Builder;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
public class CollectionDescriptor {
    private int targetReplicationLevel;
    private int currentReplicationLevel;
    private String name;
    private Set<String> containingNodesNames = new HashSet<>();

    public boolean isFullyReplicated() {
        return currentReplicationLevel == targetReplicationLevel;
    }

    public boolean isAvailable() {
        return containingNodesNames.size() != 0;
    }

    public boolean removeNode(String nodeName) {
        return containingNodesNames.remove(nodeName);
    }
}
