package database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public abstract class DatabaseTable {

    protected String TABLE_NAME;
    protected String[] ALL_COLUMN_NAMES;
    public static String getCreationQuery() {
        return null;
    }

    public static List<String> getInitialDataQueries(){
        return null;
    }
}
