package com.campus.exchange.controller;

import com.campus.exchange.util.AppConfig;
import com.campus.exchange.util.DbConnection;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates storage directories and checks the database at startup so that
 * configuration problems are visible in the Tomcat log immediately.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            Path uploads = AppConfig.uploadDir();
            Path backups = AppConfig.backupDir();
            Files.createDirectories(uploads);
            Files.createDirectories(backups);
            event.getServletContext().setAttribute("uploadDir", uploads.toString());
            event.getServletContext().setAttribute("backupDir", backups.toString());
            LOG.info("Upload directory: " + uploads);
            LOG.info("Backup directory: " + backups);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not create storage directories", e);
        }

        try (Connection ignored = DbConnection.getConnection()) {
            LOG.info("Database connection verified at startup.");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Database is not reachable. Check DB_URL, DB_USER, "
                    + "and DB_PASSWORD in the Tomcat environment.", e);
        }
    }
}
