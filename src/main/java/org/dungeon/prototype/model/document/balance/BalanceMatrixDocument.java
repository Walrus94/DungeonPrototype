package org.dungeon.prototype.model.document.balance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "balance_matrices")
@CompoundIndex(name = "chatId_name_idx", def = "{'chatId': 1, 'name': 1}", unique = true)
@NoArgsConstructor
public class BalanceMatrixDocument {
    @Id
    private String id;
    private Long chatId;
    private String name;
    private List<List<Double>> data;
}
