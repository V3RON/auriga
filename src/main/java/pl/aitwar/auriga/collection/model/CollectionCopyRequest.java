package pl.aitwar.auriga.collection.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CollectionCopyRequest {
    private String name;
    private String url;
}
