package database;

/**
 * This class is just a container to pass the database details around.
 */
public class DatabaseCredentials {
    //Only jdbc supported.
    private final String protocol = "jdbc";
    private final String vendor;
    private String host;
    private String port;

    private String user;
    private String password;

    private String database;

    public DatabaseCredentials(String vendor, String host, String port,
                               String user, String password, String databaseName) {
        this.vendor = vendor;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = databaseName;
    }

    /**
     * Helper method for construction the database connection URL from default parameters
     * @return The connection URL.
     */
    public String getConnectionURL() {
        StringBuilder connectionURL = new StringBuilder();
        connectionURL.append(protocol).append(':')
                .append(vendor).append("://")
                .append(host).append(':')
                .append(port).append('/')
                .append("?useSSL=false&allowPublicKeyRetrieval=true");

        return connectionURL.toString();
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabase() {
        return database;
    }
}
