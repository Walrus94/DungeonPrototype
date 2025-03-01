package org.dungeon.prototype.repository.postgres;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.SQLException;

@Service
public class BalanceMatrixRepository {

    @Value("${spring.profiles.active}")
    private String env;
    private final JdbcTemplate jdbcTemplate;

    public BalanceMatrixRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public double[][] getMatrix(Long chatId, String name) {
        String sql = "SELECT data FROM ? WHERE chat_id = ? AND name = ? AND is_template = false";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> convertToJavaArray(rs.getArray("data")), "matrices_" + env, chatId, name);
    }

    private double[][] convertToJavaArray(Array sqlArray) {
        try {
            return (double[][]) sqlArray.getArray();
        } catch (SQLException e) {
            throw new RuntimeException("Error converting SQL array to Java array", e);
        }
    }
}
