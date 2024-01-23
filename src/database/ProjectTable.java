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
                .append(padding).append(COL_CUSTOMER).append(" INT UNSIGNED NULL,\n")
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
                .append(foreignKeyHandlingString).append('\n')
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
        ArrayList<String> answer = new ArrayList<>();
        answer.add(new StringBuilder()
                .append("INSERT INTO ").append(TABLE_NAME).append(" (")
                .append(COL_PROJECT_NAME).append(", ")
                .append(COL_CUSTOMER).append(", ")
                .append(COL_TYPE).append(") VALUES ('New Office building for Std Bank', 2, 5);").toString());

        answer.add(new StringBuilder()
                .append("INSERT INTO ").append(TABLE_NAME).append(" (")
                .append(COL_PROJECT_NAME).append(", ")
                .append(COL_CUSTOMER).append(", ")
                .append(COL_TYPE).append(", ")
                .append(COL_PHYS_ADDR).append(", ")
                .append(COL_ERF).append(", ")
                .append(COL_DEADLINE).append(", ")
                .append(COL_ARCHITECT).append(", ")
                .append(COL_STATUS).append(", ")
                .append(COL_ENGINEER).append(", ")
                .append(COL_PROJ_MANAGER).append(", ")
                .append(COL_TOTAL_FEE).append(") VALUES ('Corner shop in Athlone', 7, 3, 'Cnr Field Cres & Hickory Str, Athlone, Cape Town', 22230, '2024-07-31', 10, 6, 5, 8, 1600000.00);").toString());

        answer.add(new StringBuilder()
                .append("INSERT INTO ").append(TABLE_NAME).append(" (")
                .append(COL_PROJECT_NAME).append(", ")
                .append(COL_CUSTOMER).append(", ")
                .append(COL_TYPE).append(", ")
                .append(COL_PHYS_ADDR).append(", ")
                .append(COL_ERF).append(", ")
                .append(COL_DEADLINE).append(", ")
                .append(COL_ARCHITECT).append(", ")
                .append(COL_STATUS).append(") VALUES ('Boutique hotel in Parys', 2, 5, '7 Ring Rd, Parys, Free State', 107, '2024-12-01', 5, 3);").toString());

        answer.add(new StringBuilder()
                .append("INSERT INTO ").append(TABLE_NAME).append(" (")
                .append(COL_PROJECT_NAME).append(", ")
                .append(COL_CUSTOMER).append(", ")
                .append(COL_TYPE).append(", ")
                .append(COL_PHYS_ADDR).append(", ")
                .append(COL_ERF).append(", ")
                .append(COL_DEADLINE).append(", ")
                .append(COL_ARCHITECT).append(", ")
                .append(COL_STATUS).append(", ")
                .append(COL_ENGINEER).append(", ")
                .append(COL_PROJ_MANAGER).append(") VALUES ('Luxury apartments in Sandton', 2, 2, '12 Sandton Drive, Sandton', 7800, '2024-09-30', 2, 5, 8, 6);").toString());

        answer.add(new StringBuilder()
                .append("INSERT INTO ").append(TABLE_NAME).append(" (")
                .append(COL_PROJECT_NAME).append(", ")
                .append(COL_CUSTOMER).append(", ")
                .append(COL_TYPE).append(", ")
                .append(COL_PHYS_ADDR).append(", ")
                .append(COL_ERF).append(", ")
                .append(COL_DEADLINE).append(", ")
                .append(COL_ARCHITECT).append(", ")
                .append(COL_STATUS).append(", ")
                .append(COL_ENGINEER).append(") VALUES ('Truck Depot', 9, 4, '67 Janadel Ave, Midrand', 45556, '2025-05-31', 4, 4, 4);").toString());
        return answer;
    }
}
