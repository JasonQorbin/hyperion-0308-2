package database;

import java.util.ArrayList;
import java.util.List;

public class StatusTable {
    
    static public final String TABLE_NAME = "ProjectStatus";
    
    static public final String COL_ID = "ID";
    static public final String COL_NAME = "Name";
    static public final int COL_FIRST_NAME_SIZE = 30;

    static public final String[] ALL_COLUMN_NAMES = {
        COL_ID,
        COL_NAME
    };

    private static final String[] INITIAL_NAMES = {
        "Captured",
        "Logged",
        "Concept and Viability",
        "Pre-feasibility",
        "Bankable feasibility",
        "Construction",
        "Finalisation"
    };

    public static String getCreationQuery() {
        final String padding = "  ";
        StringBuilder query = new StringBuilder();
        query.append("CREATE TABLE ").append(TABLE_NAME).append(" (\n")
            .append(padding).append(COL_ID).append(" INT(10) UNSIGNED NOT NULL,\n")
            .append(padding).append(COL_NAME).append(" VARCHAR(").append(COL_FIRST_NAME_SIZE).append(") NOT NULL,\n")
            .append(padding).append("PRIMARY KEY (").append(COL_ID).append(")\n")
            .append(")");
        return query.toString();
    }


    public static List<String> getInitialDataQueries() {
        StringBuilder queryBase = new StringBuilder();
        queryBase.append("INSERT INTO ").append(TABLE_NAME).append(" (");
        for (int i = 0; i < ALL_COLUMN_NAMES.length; ++i) {
            queryBase.append(ALL_COLUMN_NAMES[i]);
            if (i < ALL_COLUMN_NAMES.length -1) {
                queryBase.append(", ");
            }
        }
        queryBase.append(") VALUES (");

        ArrayList<String> answer = new ArrayList<>();

        final int numOfEntries = INITIAL_NAMES.length;
        for (int i = 1; i <= numOfEntries; i++) {
            answer.add(new StringBuilder(queryBase)
                .append(i).append(", ")
                .append('\'').append(INITIAL_NAMES[i-1]).append("');").toString());

        }
        return answer;
    }
}
