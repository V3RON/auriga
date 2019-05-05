package pl.aitwar.auriga.collection.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CollectionDescriptor {
    private int targetReplicationLevel;
    private int currentReplicationLevel;
    private boolean blocked = false;
    private String name;
    private Set<String> containingNodesNames = new HashSet<>();

    @JsonIgnore
    public boolean isFullyReplicated() {
        return currentReplicationLevel == targetReplicationLevel;
    }

    @JsonIgnore
    public boolean isAvailable() {
        return containingNodesNames.size() != 0;
    }

    public boolean removeNode(String nodeName) {
        return containingNodesNames.remove(nodeName);
    }
}
