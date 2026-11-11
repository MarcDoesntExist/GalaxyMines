package me.marcdoesntexist.galaxymines.stats;

import me.marcdoesntexist.galaxymines.GalaxyMines;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public final class JdbcPlayerStatsStorage implements PlayerStatsStorage {
    private final String type;
    private final String url;
    private final String username;
    private final String password;
    private final boolean sqlite;

    public JdbcPlayerStatsStorage(final String type, final String url, final String username,
                                  final String password) {
        this.type = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        this.sqlite = !this.type.equals("MYSQL");
        if (url == null || url.isBlank()) {
            final File database = new File(GalaxyMines.storageFolder, "players.db");
            this.url = "jdbc:sqlite:" + database.getAbsolutePath();
        } else {
            this.url = url;
        }
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
    }

    @Override
    public void init() throws Exception {
        if (this.sqlite) {
            Class.forName("org.sqlite.JDBC");
        } else {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException first) {
                Class.forName("com.mysql.jdbc.Driver");
            }
        }
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS gm_players ("
                    + "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(64) NOT NULL, "
                    + "total_blocks BIGINT NOT NULL DEFAULT 0, "
                    + "total_sold BIGINT NOT NULL DEFAULT 0, "
                    + "total_earned DOUBLE NOT NULL DEFAULT 0)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS gm_player_mines ("
                    + "uuid VARCHAR(36) NOT NULL, mine VARCHAR(64) NOT NULL, "
                    + "blocks BIGINT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (uuid, mine))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS gm_player_periods ("
                    + "uuid VARCHAR(36) NOT NULL, period_type VARCHAR(8) NOT NULL, "
                    + "period_key VARCHAR(16) NOT NULL, blocks BIGINT NOT NULL DEFAULT 0, "
                    + "earned DOUBLE NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (uuid, period_type, period_key))");
        }
    }

    private Connection open() throws SQLException {
        if (this.username.isEmpty()) {
            return DriverManager.getConnection(this.url);
        }
        return DriverManager.getConnection(this.url, this.username, this.password);
    }

    @Override
    public PlayerStats load(final UUID uuid, final String name) throws Exception {
        final PlayerStats stats = new PlayerStats(uuid, name);
        try (Connection connection = open()) {
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT name, total_blocks, total_sold, total_earned FROM gm_players "
                            + "WHERE uuid = ?")) {
                query.setString(1, uuid.toString());
                try (ResultSet rows = query.executeQuery()) {
                    if (rows.next()) {
                        stats.name(rows.getString(1));
                        stats.setLoadedTotals(rows.getLong(2), rows.getLong(3), rows.getDouble(4));
                    }
                }
            }
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT mine, blocks FROM gm_player_mines WHERE uuid = ?")) {
                query.setString(1, uuid.toString());
                try (ResultSet rows = query.executeQuery()) {
                    while (rows.next()) {
                        stats.mineBlocks().put(rows.getString(1), rows.getLong(2));
                    }
                }
            }
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT period_type, period_key, blocks, earned FROM gm_player_periods "
                            + "WHERE uuid = ?")) {
                query.setString(1, uuid.toString());
                try (ResultSet rows = query.executeQuery()) {
                    while (rows.next()) {
                        final String key = rows.getString(1) + ":" + rows.getString(2);
                        final PlayerStats.PeriodValue value =
                                stats.periods().computeIfAbsent(key,
                                        ignored -> new PlayerStats.PeriodValue());
                        value.blocks = rows.getLong(3);
                        value.earned = rows.getDouble(4);
                    }
                }
            }
        }
        stats.clean();
        return stats;
    }

    @Override
    public void save(final PlayerStats stats) throws Exception {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                writePlayers(connection, stats);
                writeMines(connection, stats);
                writePeriods(connection, stats);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void writePlayers(final Connection connection, final PlayerStats stats)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM gm_players WHERE uuid = ?")) {
            delete.setString(1, stats.uuid().toString());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO gm_players (uuid, name, total_blocks, total_sold, total_earned) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            insert.setString(1, stats.uuid().toString());
            insert.setString(2, stats.name());
            insert.setLong(3, stats.totalBlocks());
            insert.setLong(4, stats.totalSoldItems());
            insert.setDouble(5, stats.totalEarned());
            insert.executeUpdate();
        }
    }

    private void writeMines(final Connection connection, final PlayerStats stats)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM gm_player_mines WHERE uuid = ?")) {
            delete.setString(1, stats.uuid().toString());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO gm_player_mines (uuid, mine, blocks) VALUES (?, ?, ?)")) {
            for (final var entry : stats.mineBlocks().entrySet()) {
                insert.setString(1, stats.uuid().toString());
                insert.setString(2, entry.getKey());
                insert.setLong(3, entry.getValue());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void writePeriods(final Connection connection, final PlayerStats stats)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM gm_player_periods WHERE uuid = ?")) {
            delete.setString(1, stats.uuid().toString());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO gm_player_periods (uuid, period_type, period_key, blocks, earned) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            for (final var entry : stats.periods().entrySet()) {
                final int separator = entry.getKey().indexOf(':');
                if (separator < 0) {
                    continue;
                }
                insert.setString(1, stats.uuid().toString());
                insert.setString(2, entry.getKey().substring(0, separator));
                insert.setString(3, entry.getKey().substring(separator + 1));
                insert.setLong(4, entry.getValue().blocks);
                insert.setDouble(5, entry.getValue().earned);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    @Override
    public List<LeaderboardEntry> leaderboard(final StatsPeriod period, final String key,
                                              final int limit) throws Exception {
        final List<LeaderboardEntry> entries = new ArrayList<>();
        try (Connection connection = open();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT s.uuid, COALESCE(p.name, s.uuid), s.blocks, s.earned "
                             + "FROM gm_player_periods s LEFT JOIN gm_players p "
                             + "ON p.uuid = s.uuid "
                             + "WHERE s.period_type = ? AND s.period_key = ? "
                             + "ORDER BY s.blocks DESC")) {
            query.setString(1, period.name());
            query.setString(2, key);
            query.setMaxRows(limit);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    entries.add(new LeaderboardEntry(UUID.fromString(rows.getString(1)),
                            rows.getString(2), rows.getLong(3), rows.getDouble(4)));
                }
            }
        }
        return entries;
    }

    @Override
    public void close() {
    }
}
