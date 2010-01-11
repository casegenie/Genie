package org.alliance.core.file.h2database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.alliance.core.CoreSubsystem;
import org.h2.jdbc.JdbcSQLException;

/**
 *
 * @author Bastvera
 */
public class DatabaseCore {

    private final CoreSubsystem core;
    private Connection conn;
    private DatabaseShares dbShares;
    private DatabaseHashes dbHashes;
    private DatabaseDuplicates dbDuplicates;
    private static final String DRIVERURL = "jdbc:h2:";
    private static final String TYPE = "file:";
    //private static final String PATH = "./data/alliancedb";
    private static final String OPTIONS = ";";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private static final String DRIVER = "org.h2.Driver";

    public DatabaseCore(CoreSubsystem core) {
        this.core = core;
        connect();
    }

    public void connect() {
        try {
            Class.forName(DRIVER);
            String path = core.getSettings().getInternal().getDatabasefile();
            conn = DriverManager.getConnection(DRIVERURL + TYPE + path + OPTIONS, USER, PASSWORD);
            changeCache(8 * 1024);
            dbShares = new DatabaseShares(conn);
            dbHashes = new DatabaseHashes(conn);
            dbDuplicates = new DatabaseDuplicates(conn);
        } catch (JdbcSQLException ex) {
            ex.printStackTrace();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void shutdown() {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SHUTDOWN COMPACT;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void changeCache(int cache) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SET CACHE_SIZE ?;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setInt(1, cache);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public DatabaseHashes getDbHashes() {
        return dbHashes;
    }

    public DatabaseShares getDbShares() {
        return dbShares;
    }

    public DatabaseDuplicates getDbDuplicates() {
        return dbDuplicates;
    }
}
