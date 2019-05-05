package pl.aitwar.auriga.nodes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Node {
    private String name;
    private String address;

    public Node(String name) {
        this.name = name;
    }
}
