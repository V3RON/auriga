package pl.aitwar.auriga.nodes.model;

import lombok.Data;

import java.util.Set;

@Data
public class NodeUsageMetric {
    private String address;
    private String name;
    private Double load;
    private Set<String> collections;

    public boolean isEmpty() {
        return collections.isEmpty();
    }
}
