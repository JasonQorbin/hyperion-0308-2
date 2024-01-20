package database;

import java.util.ArrayList;
import java.util.List;

public class PersonTable {

    static public final String TABLE_NAME = "Person";
    //Columns
    static public final String COL_ID = "ID";

    static public final String COL_FIRST_NAME = "FirstName";
    static public final int COL_FIRST_NAME_SIZE = 40;

    static public final String COL_SURNAME = "Surname";
    static public final int COL_SURNAME_SIZE = 40;

    static public final String COL_EMAIL = "Email";
    static public final int COL_EMAIL_SIZE = 50;

    static public final String COL_PHYS_ADDR = "PhysAddress";
    static public final int COL_PHYS_ADDR_SIZE = 120;

    static public final String[] ALL_COLUMN_NAMES = {
        COL_ID,
        COL_FIRST_NAME,
        COL_SURNAME,
        COL_EMAIL,
        COL_PHYS_ADDR
    };

    public static String getCreationQuery() {
        final String padding = "  ";
        StringBuilder query = new StringBuilder();
        query.append("CREATE TABLE person (\n")
                .append("  ").append(COL_ID).append(" int(10) UNSIGNED NOT NULL AUTO_INCREMENT,\n")
                .append("  ").append(COL_FIRST_NAME).append(" varchar(").append(COL_FIRST_NAME_SIZE).append(") DEFAULT NULL,\n")
                .append("  ").append(COL_SURNAME).append(" varchar(").append(COL_SURNAME_SIZE).append(") DEFAULT NULL,\n")
                .append("  ").append(COL_EMAIL).append(" varchar(").append(COL_EMAIL_SIZE).append(") DEFAULT NULL,\n")
                .append("  ").append(COL_PHYS_ADDR).append(" varchar(").append(COL_PHYS_ADDR_SIZE).append(") DEFAULT NULL,\n")
                .append("  ").append("PRIMARY KEY (").append(COL_ID).append(")\n")
                .append(") COMMENT='Records of all actors in the PM process from customers to engineers'");
        return query.toString();
    }

    //Initial Data
    private static final String[] INITIAL_FIRST_NAMES = {
        "Aubree",
        "Lauren",
        "Forest",
        "Everest",
        "Kora",
        "Paola",
        "Zayne",
        "Jamie",
        "Dane",
        "Kenzie"
    };

    static final String[] INITIAL_SURNAMES = {
        "Beck",
        "Benitez",
        "Hensley",
        "Parker",
        "Jenkins",
        "Murillo",
        "Marsh",
        "Cohen",
        "Steele",
        "Simon"
    };

    private static final String[] INITIAL_EMAILS = {
        "a.beck@rendar.co.uk",
        "laurenB@herspace.com",
        "fh@hensleydomain.com",
        "e.parker@mountain.com",
        "notleroy@elmstreet.com",
        "paola@littleitaly.co.eu",
        "zayne.marsh@gmail.com",
        "Jamie@telkomsa.net",
        "dSteele@Redfootconstruction.com",
        "KenzieSimmy@Outlook.com"
    };

    private static final String[] INITIAL_ADDRESSES = {
        "72 Haskell Rd, Harare, Zimbabwe",
        "12c Thompson Street, London, United Kingdom",
        "45 Church Street, Syndey, Australia",
        "1 Main Street, Kilimanjaro",
        "13 Elm Str, Somewhere in America",
        "23 Fettucinni Str, Rome ",
        "88 Hillfort Rd, Parys, South Africa",
        "33 Malibongwe Rd, Johannesburg",
        "45 Basil Rd, Polokwane, South Africa",
        "4445 Jones Str, Orlando, Florida"
    };

    public static List<String> getInitialDataQueries() {
        StringBuilder queryBase = new StringBuilder();
        queryBase.append("INSERT INTO ").append(TABLE_NAME).append(" (");
        for (int i = 1; i < ALL_COLUMN_NAMES.length; ++i) {
            queryBase.append(ALL_COLUMN_NAMES[i]);
            if (i < ALL_COLUMN_NAMES.length -1) {
                queryBase.append(", ");
            }
        }
        queryBase.append(") VALUES (");

        ArrayList<String> answer = new ArrayList<>();

        final int numOfEntries = INITIAL_FIRST_NAMES.length;
        for (int i = 0; i < numOfEntries; i++) {
            answer.add(new StringBuilder(queryBase)
                .append('\'').append(INITIAL_FIRST_NAMES[i]).append("', ")
                .append('\'').append(INITIAL_SURNAMES[i]).append("', ")
                .append('\'').append(INITIAL_EMAILS[i]).append("', ")
                .append('\'').append(INITIAL_ADDRESSES[i]).append("');").toString());

        }
        return answer;
    }
}
