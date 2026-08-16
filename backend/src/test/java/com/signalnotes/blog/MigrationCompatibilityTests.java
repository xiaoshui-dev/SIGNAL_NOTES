package com.signalnotes.blog;

import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MigrationCompatibilityTests {
    @Test
    void authorIdentityMigrationIsSafeForMysql57() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V11__account_author_identity.sql");
        assertTrue(Files.exists(migration), "V11 author identity migration must exist");
        String sql = Files.readString(migration);
        assertTrue(sql.contains("utf8mb4_unicode_ci"));
        assertFalse(sql.contains("utf8mb4_0900_ai_ci"));
        assertTrue(sql.contains("avatar_url"));
        assertTrue(sql.contains("author_id"));
        assertTrue(sql.contains("fk_posts_author"));
        assertTrue(sql.contains("ON DELETE SET NULL"));
    }

    @Test
    void legacyAuthorUpdateSelectsLowestIdActiveAdminInH2MysqlMode() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V11__account_author_identity.sql"));
        String authorUpdate = sql.substring(sql.indexOf("UPDATE posts")).trim();

        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:v11_author_identity;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE site_users (id BIGINT PRIMARY KEY, name VARCHAR(80), role VARCHAR(30), status VARCHAR(20))");
                statement.execute("CREATE TABLE posts (id BIGINT PRIMARY KEY, author_name VARCHAR(80), author_id BIGINT)");
                statement.execute("INSERT INTO site_users VALUES "
                    + "(1, 'Disabled admin', 'ADMIN', 'DISABLED'), "
                    + "(3, 'First active admin', 'ADMIN', 'ACTIVE'), "
                    + "(7, 'Second active admin', 'ADMIN', 'ACTIVE')");
                statement.execute("INSERT INTO posts VALUES (1, '\u6797\u9ed8', NULL), (2, 'Other author', NULL)");
                assertEquals(1, statement.executeUpdate(authorUpdate));
            }

            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT author_id, author_name FROM posts WHERE id = 1")) {
                assertTrue(result.next());
                assertEquals(3L, result.getLong("author_id"));
                assertEquals("First active admin", result.getString("author_name"));
            }
        }
    }

    @Test
    void followUpMigrationRepairsLegacyPostsAfterTheOriginalMigrationAlreadyRan() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V12__repair_legacy_author_links.sql");
        assertTrue(Files.exists(migration), "V12 legacy author repair migration must exist");
        String sql = Files.readString(migration);
        assertFalse(sql.contains("utf8mb4_0900_ai_ci"));

        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:v12_author_repair;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE site_users (id BIGINT PRIMARY KEY, name VARCHAR(80), role VARCHAR(30), status VARCHAR(20))");
                statement.execute("CREATE TABLE posts (id BIGINT PRIMARY KEY, author_name VARCHAR(80), author_id BIGINT)");
                statement.execute("INSERT INTO site_users VALUES (2, 'Sheldon', 'ADMIN', 'ACTIVE')");
                statement.execute("INSERT INTO posts VALUES (1, '\u6797\u9ed8', NULL), (2, '\u7ad9\u70b9\u4f5c\u8005', NULL), (3, 'Guest', NULL)");
                assertEquals(2, statement.executeUpdate(sql.trim()));
            }

            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT id, author_id, author_name FROM posts ORDER BY id")) {
                assertTrue(result.next());
                assertEquals(2L, result.getLong("author_id"));
                assertEquals("Sheldon", result.getString("author_name"));
                assertTrue(result.next());
                assertEquals(2L, result.getLong("author_id"));
                assertEquals("Sheldon", result.getString("author_name"));
                assertTrue(result.next());
                assertNull(result.getObject("author_id"));
                assertEquals("Guest", result.getString("author_name"));
            }
        }
    }
}
