package pl.aitwar.auriga.nodes.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class NodeRegistration {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String address;
    private String name;
}
