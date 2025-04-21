package org.dungeon.prototype.repository.postgres;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BalanceMatrixRepository {

    @Value("${spring.profiles.active}")
    private String env;
    private final JdbcTemplate jdbcTemplate;

    public BalanceMatrixRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isMatrixExists(long chatId, String matrixName) {
        String sql = String.format("""
        SELECT EXISTS (
            SELECT 1 FROM %s WHERE chat_id = ? AND name = ?
        )
        """, "matrices_" + env);

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, chatId, matrixName));
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

    public Map<String, Double[][]> getAllMatrices(long chatId) {
        String sql = "SELECT name, data FROM matrices_"+ env +" WHERE chat_id = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, chatId);

        Map<String, Double[][]> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String name = (String) row.get("name");
            Double[][] data = (Double[][]) row.get("data");
            result.put(name, data);
        }

        return result;
    }
}
