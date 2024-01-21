package MainProgram;

import database.DataSource;
import database.DatabaseCredentials;
import database.DatabaseException;

import java.sql.SQLException;

public class PmsProgram {

    /**
     * Represents all the possible program states.
     */
    private enum ProgramState {
        ERROR,
        MAIN_MENU,
        NEW_PROJECT,
        VIEW_CURRENT,
        VIEW_LATE,
        SEARCH_ALL,
        EDIT_PROJECT,
        EDIT_PEOPLE,
        EXIT
    }
    private static DatabaseCredentials credentials;

    public static void main (String[] args) {
        CliHandler consoleHandler = new CliHandler();
        if (args.length > 0 && args[0].equals("-t")) {
            credentials = new DatabaseCredentials("mariadb", "localhost", "3306", "Jason", "KochiraDozo", "PoisePMS");
            System.out.println(credentials);
        } else {
            credentials = getCredentialsFromUser(consoleHandler);
        }
        DataSource dataSource;
        dataSource = DataSource.getInstance(credentials);
        if ( dataSource == null ) {
            System.out.println("Fatal error: Could not establish database connection.\n");
            consoleHandler.close();
            return;
        }
        try {
            dataSource.initialiseDatabase();
        } catch (DatabaseException ex) {
            System.out.println("Fatal error: Could not initialise database configuration.\n");
            System.out.println(ex.getMessage());
            System.out.println(ex.getCause().getMessage());
            consoleHandler.close();
            return;
        }

        //The program is modelled as a state machine. Each state determines the current behaviour of the program.
        //The program loops continuously until the EXIT state is reached.
        ProgramState programState = ProgramState.MAIN_MENU;
        Project currentSelection = null;

        consoleHandler.printTitle();
        while (programState != ProgramState.EXIT) {
            try {
                switch (programState) {
                    case MAIN_MENU:
                        int menuChoice = consoleHandler.printMainMenu(currentSelection);
                        programState = newStateFromMainMenu(menuChoice);
                        break;
                    case NEW_PROJECT:
                        currentSelection = consoleHandler.addProject();
                        programState = ProgramState.MAIN_MENU;
                        break;
                    case VIEW_CURRENT:
                        currentSelection = consoleHandler.showCurrentProjects(currentSelection);
                        programState = ProgramState.MAIN_MENU;
                        break;
                    case VIEW_LATE:
                        currentSelection = consoleHandler.showOverdueProjects(currentSelection);
                        programState = ProgramState.MAIN_MENU;
                        break;
                    case SEARCH_ALL:
                        Project newSelection = consoleHandler.searchDialog();
                        if (newSelection != null) {
                            currentSelection = newSelection;
                        }
                        programState = ProgramState.MAIN_MENU;
                        break;
                    case EDIT_PROJECT:
                        currentSelection = consoleHandler.updateMenu(currentSelection);
                        programState = ProgramState.MAIN_MENU;
                        break;
                    case EDIT_PEOPLE:
                        consoleHandler.editPeople();
                        programState = ProgramState.MAIN_MENU;
                        break;
                }
            } catch (DatabaseException exc) {
                System.out.println("Database error occurred. Check the status of the database and consider restarting the program.");
                System.out.println(exc.getMessage());
                System.out.println(exc.getCause().getMessage());
                programState = ProgramState.MAIN_MENU;
            }
        }
        //Exiting program. Cleanup any open resources.
        try{
            dataSource.close();
        } catch (SQLException ex) {
            System.out.println("Error encountered while closing database connection.\n" + ex.getMessage());
        }
        consoleHandler.close();

    }

    private static ProgramState newStateFromMainMenu(int menuSelection){
        return switch(menuSelection) {
            case 1:
                yield ProgramState.NEW_PROJECT;
            case 2:
                yield ProgramState.VIEW_CURRENT;
            case 3:
                yield ProgramState.VIEW_LATE;
            case 4:
                yield ProgramState.SEARCH_ALL;
            case 5:
                yield ProgramState.EDIT_PEOPLE;
            case 6:
                yield ProgramState.EDIT_PROJECT;
            case 0:
                yield ProgramState.EXIT;
            default:
                yield ProgramState.ERROR;
        };
    }

    public static DatabaseCredentials getCredentialsFromUser(CliHandler handler) {
        System.out.println("\nPlease enter the database details (Leave the prompt blank to accept the defaults):");
        System.out.println("==================================================================================\n");

        String vendor = handler.getStringFromUser("Database vendor mysql/mariadb[default = mysql]: ", true);
        if (vendor.isBlank()) {
            vendor = "mysql";
        }

        String host = handler.getStringFromUser("Server host name/ip [default = localhost]: ", true);
        if (host.isBlank()) {
            host = "localhost";
        }

        String port = handler.getStringFromUser("Server port [default = 3306]: ", true);
        if (port.isBlank()){
            port = "3306";
        }

        String database = handler.getStringFromUser("Database name [default = PoisePMS]: ", true);
        if (database.isBlank()){
            database = "PoisePMS";
        }

        System.out.println("\nEnter the database user and password to use (This user must have privileges to use the "
                + database + " database):\n");

        String user = handler.getStringFromUser("Database User: ", false);
        String password = handler.getStringFromUser("Database Password: ", false);


        return new DatabaseCredentials(vendor, host, port, user, password, database);
    }
}
