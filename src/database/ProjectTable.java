package database;

import java.util.ArrayList;
import java.util.List;

public class ProjectTable {

    static public final String TABLE_NAME = "Projects";
    //Columns
    static public final String COL_NUMBER = "Num";
    static public final String COL_PROJECT_NAME = "Name";
    static public final int COL_PROJECT_NAME_SIZE = 80;
    static public final String COL_TOTAL_FEE = "TotalFee";
    static public final String COL_TOTAL_PAID = "TotalPaid";

    static public final String COL_PHYS_ADDR = "PhysAddress";
    static public final int COL_PHYS_ADDR_SIZE = 120;

    static public final String COL_DEADLINE = "Deadline";
    static public final String COL_ERF = "ERF_Num";
    static public final String COL_ENGINEER = "Engineer";
    static public final String COL_PROJ_MANAGER = "ProjectManager";
    static public final String COL_CUSTOMER = "Customer";
    static public final String COL_ARCHITECT = "Architect";
    static public final String COL_STATUS = "Status";
    static public final String COL_TYPE = "Type";


    static public final String[] ALL_COLUMN_NAMES = {
            COL_NUMBER,
            COL_PROJECT_NAME,
            COL_TOTAL_FEE,
            COL_TOTAL_PAID,
            COL_ERF,
            COL_PHYS_ADDR,
            COL_DEADLINE,
            COL_ENGINEER,
            COL_PROJ_MANAGER,
            COL_CUSTOMER,
            COL_ARCHITECT,
            COL_STATUS,
            COL_TYPE
    };

    public static String getCreationQuery() {
        final String padding = "    ";
        final String foreignKeyReferenceString =  ") REFERENCES " + PersonTable.TABLE_NAME + " (" + PersonTable.COL_ID + ")";
        final String foreignKeyHandlingString = padding + padding + padding + "ON UPDATE CASCADE ON DELETE SET NULL,";
        final String foreignKeyHandlingCustomerString = padding + padding + padding + "ON UPDATE CASCADE ON DELETE RESTRICT,";
        StringBuilder query = new StringBuilder();
        query.append("CREATE TABLE ").append(TABLE_NAME).append('\n')
                .append("(\n")
                .append(padding).append(COL_NUMBER).append(" INT UNSIGNED AUTO_INCREMENT COMMENT 'Project Number',\n")
                .append(padding).append(COL_PROJECT_NAME).append(" VARCHAR(").append(COL_PROJECT_NAME_SIZE).append(") NOT NULL,\n")
                .append(padding).append(COL_PHYS_ADDR).append(" VARCHAR(").append(COL_PHYS_ADDR_SIZE).append(") NULL,\n")
                .append(padding).append(COL_ERF).append(" INT UNSIGNED NULL,\n")
                .append(padding).append(COL_TOTAL_FEE).append(" DECIMAL(20, 2) UNSIGNED DEFAULT 0 NOT NULL,\n")
                .append(padding).append(COL_TOTAL_PAID).append(" DECIMAL(20, 2) UNSIGNED DEFAULT 0 NOT NULL,\n")
                .append(padding).append(COL_DEADLINE).append(" DATE NULL,\n")
                .append(padding).append(COL_ENGINEER).append(" INT UNSIGNED NULL,\n")
                .append(padding).append(COL_PROJ_MANAGER).append(" INT UNSIGNED NULL,\n")
                .append(padding).append(COL_CUSTOMER).append(" INT UNSIGNED NOT NULL,\n")
                .append(padding).append(COL_ARCHITECT).append(" INT UNSIGNED NULL,\n")
                .append(padding).append(COL_STATUS).append(" INT UNSIGNED DEFAULT 1 NOT NULL,\n")
                .append(padding).append(COL_TYPE).append(" INT UNSIGNED NOT NULL,\n")
                .append(padding).append("CONSTRAINT ").append(TABLE_NAME).append("_pk\n")
                .append(padding).append(padding).append("PRIMARY KEY (Num),\n")
                .append(padding).append("CONSTRAINT ").append(TABLE_NAME).append("_Arch_Person_fk\n")
                .append(padding).append(padding).append("FOREIGN KEY (").append(COL_ARCHITECT)
                .append(foreignKeyReferenceString).append('\n')
                .append(foreignKeyHandlingString).append('\n')
                .append(padding).append("CONSTRAINT ").append(TABLE_NAME).append("_Cust_Person_fk\n")
                .append(padding).append(padding).append("FOREIGN KEY (").append(COL_CUSTOMER)
                .append(foreignKeyReferenceString).append('\n')
                .append(foreignKeyHandlingCustomerString).append('\n')
                .append(padding).append("CONSTRAINT ").append(TABLE_NAME).append("_Engineer_Person_fk\n")
                .append(padding).append(padding).append("FOREIGN KEY (").append(COL_ENGINEER)
                .append(foreignKeyReferenceString).append('\n')
                .append(foreignKeyHandlingString).append('\n')
                .append(padding).append("CONSTRAINT ").append(TABLE_NAME).append("_PMan_Person_fk\n")
                .append(padding).append(padding).append("FOREIGN KEY (").append(COL_PROJ_MANAGER)
                .append(foreignKeyReferenceString).append('\n')
                .append(foreignKeyHandlingString).append('\n')
                .deleteCharAt(query.lastIndexOf(","))
                .append(");");

        return query.toString();
    }

    public static List<String> getInitialDataQueries() {
        //No initial data
        return new ArrayList<String>();
    }
}
