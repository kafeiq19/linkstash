package com.linkstash.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Ensures the SQLite parent directory exists, then runs schema.sql (idempotent DDL)
 * before the app starts serving. Replaces spring.sql.init so directory creation
 * is guaranteed immediately before the first connection.
 */
@Component
public class SchemaInitializer {

    private final DataSource dataSource;
    private final Resource schemaResource;
    private final String jdbcUrl;

    public SchemaInitializer(DataSource dataSource,
                             @Value("classpath:schema.sql") Resource schemaResource,
                             @Value("${spring.datasource.url}") String jdbcUrl) {
        this.dataSource = dataSource;
        this.schemaResource = schemaResource;
        this.jdbcUrl = jdbcUrl;
    }

    public static void ensureSqliteDir(String jdbcUrl) {
        try {
            if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:sqlite:")) {
                String path = jdbcUrl.substring("jdbc:sqlite:".length());
                if (!path.isBlank() && !path.startsWith(":") && !path.startsWith("file:")) {
                    Path parent = Path.of(path).toAbsolutePath().getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                }
            }
        } catch (Exception ignored) {
            // best-effort
        }
    }

    @PostConstruct
    public void initSchema() throws Exception {
        ensureSqliteDir(jdbcUrl);
        String sql = new String(schemaResource.getInputStream().readAllBytes());
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String part : sql.split(";")) {
                String ddl = part.trim();
                if (!ddl.isEmpty()) {
                    stmt.execute(ddl);
                }
            }
        }
    }
}
