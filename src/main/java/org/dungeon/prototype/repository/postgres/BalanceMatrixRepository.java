package org.dungeon.prototype.repository.postgres;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BalanceMatrixRepository {

    @Value("${spring.profiles.active}")
    private String env;
    private final JdbcTemplate jdbcTemplate;

    public BalanceMatrixRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isTableExists(long chatId, String tableName) {
        String sql = String.format("""
        SELECT EXISTS (
            SELECT 1 FROM %s WHERE chat_id = ?
        )
        """, tableName);

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, chatId));
    }

    public double getValue(Long chatId, String name, int row, int col) {
        String sql = "SELECT data[?][?] FROM ? WHERE chat_id = ? AND name = ?";
        return jdbcTemplate.queryForObject(sql, Double.class, row, col, "matrices_" + env, chatId, name);
    }

    public void clearBalanceMatrix(long chatId, String name) {
        String sql = "DELETE FROM matrices_" + env + " WHERE chat_id = ? AND name = ?";
        jdbcTemplate.update(sql, chatId, name);

    }

    public double[][] getBalanceMatrix(long chatId, String name) {
        String sql = "SELECT data FROM matrices_" + env + " WHERE chat_id = ? AND name = ?";
        return jdbcTemplate.queryForObject(sql, double[][].class, chatId, name);
    }
}
