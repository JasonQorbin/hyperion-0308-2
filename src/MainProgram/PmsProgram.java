package MainProgram;

import database.DataSource;
import database.DatabaseCredentials;

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
        EXIT
    }
    private static DatabaseCredentials credentials;

    public static void main (String[] args) {
        CliHandler consoleHandler = new CliHandler();
        DataSource dataSource;
        try {
            dataSource = DataSource.getInstance(credentials);
        } catch (SQLException ex) {
            System.out.println("Fatal error: Could not establish database connection.\n" + ex.getMessage());
            return;
        }
        //The program is modelled as a state machine. Each state determines the current behaviour of the program.
        //The program loops continuously until the EXIT state is reached.
        ProgramState programState = ProgramState.MAIN_MENU;
        Project currentSelection = null;

        consoleHandler.printTitle();
        while (programState != ProgramState.EXIT) {
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
                    currentSelection = consoleHandler.searchDialog();
                    programState = ProgramState.MAIN_MENU;
                    break;
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
                yield ProgramState.EDIT_PROJECT;
            case 0:
                yield ProgramState.EXIT;
            default:
                yield ProgramState.ERROR;
        };
    }
}
