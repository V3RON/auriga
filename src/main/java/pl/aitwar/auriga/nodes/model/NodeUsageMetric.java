package pl.aitwar.auriga.nodes.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Set;

@Data
public class NodeUsageMetric {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String address;
    private String name;
    private Double load;
    private Set<CollectionStatistics> collections;

    public boolean isEmpty() {
        return true;
    }
}
