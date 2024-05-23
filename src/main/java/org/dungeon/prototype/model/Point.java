package org.dungeon.prototype.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor(staticName = "of")
public class Point implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529673298248057691L;
    private Integer x;
    private Integer y;
}
