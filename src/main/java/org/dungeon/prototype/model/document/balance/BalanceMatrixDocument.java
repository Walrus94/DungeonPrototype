package org.dungeon.prototype.model.document.balance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "balance_matrices")
@NoArgsConstructor
public class BalanceMatrixDocument {
    @Id
    private String id;
    @Indexed
    private Long chatId;
    private String name;
    private List<List<Double>> data;
}
