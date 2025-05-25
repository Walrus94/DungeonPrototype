package org.dungeon.prototype.repository.postgres;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.SQLException;


@Service
public class BalanceMatrixRepository {
    private final JdbcTemplate jdbcTemplate;

    public BalanceMatrixRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isMatrixExists(long chatId, String matrixName) {
        String sql = String.format("""
        SELECT EXISTS (
            SELECT 1 FROM %s WHERE chat_id = ? AND name = ?
        )
        """, "matrices");

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, chatId, matrixName));
    }

    public float getValue(Long chatId, String name, int row, int col) {
        String sql =
                String.format("SELECT data[%d][%d] FROM matrices WHERE chat_id = ? AND name = ?",
                        row, col);
        return jdbcTemplate.query(sql, ps -> {
            ps.setLong(1, chatId);
            ps.setString(2, name);
        }, rs -> {
            if (!rs.next()) {
                throw new EmptyResultDataAccessException(1);
            }

            Array array = rs.getArray("data");
            if (array == null) {
                throw new SQLException("Array column is null");
            }

            Object raw = array.getArray();
            if (!(raw instanceof Object[][] obj2D)) {
                throw new SQLException("Expected 2D array but got: " + raw.getClass());
            }

            Object cell = obj2D[row][col];
            if (cell instanceof Number number) {
                return number.floatValue();
            } else {
                throw new SQLException("Array element is not a number: " + cell);
            }
        });
    }

    public void clearBalanceMatrix(long chatId, String name) {
        String sql = "DELETE FROM matrices WHERE chat_id = ? AND name = ?";
        jdbcTemplate.update(sql, chatId, name);

    }

    public double[][] getBalanceMatrix(long chatId, String name) {
        String sql = "SELECT data FROM matrices WHERE chat_id = ? AND name = ?";
        return jdbcTemplate.queryForObject(sql, double[][].class, chatId, name);
    }
}
