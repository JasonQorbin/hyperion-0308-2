package database;

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
            COL_FIRST_NAME,
            COL_SURNAME,
            COL_EMAIL,
            COL_PHYS_ADDR
    };
}
