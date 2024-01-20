package database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public abstract class DatabaseTable {

    public String TABLE_NAME;
    protected String[] ALL_COLUMN_NAMES;
    public abstract String getCreationQuery();

    public abstract List<String> getInitialDataQueries();
}
