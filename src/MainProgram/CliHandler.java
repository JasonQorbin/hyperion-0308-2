package MainProgram;

import database.DataSource;
import database.DatabaseException;
import database.PersonTable;
import database.ProjectTable;


import java.sql.SQLException;
import java.util.*;

class CliHandler {
    Scanner consoleReader;

    public CliHandler() {
        consoleReader = new Scanner(System.in);
    }
    
    public void printTitle() {
        System.out.println(
            """
                Poised Project Management System
                ================================
            """
        );
        System.out.println();
    }

    /**
     * Prints the main menu and returns the user's selection. This version of the menu requires a project to be selected
     * because there are options that are only relevant when something is selected.
     *
     * @param selectedProject The project that is currently selected.
     * @return The user's selection
     */
    public int printMainMenu(Project selectedProject) {
        if (selectedProject == null) {
            //Nothing selected. Call the version of the menu without the extra options.
            return printMainMenu();
        }
        System.out.println( //TODO: Update
            """
            Main  Menu
            ==========
            
            1. Add a new book
            2. Search/select books
            3. Update selected book
            4. Delete selected book
            0. Exit
            """
        );
        System.out.println("Selected project: "
            + selectedProject.title + " by " + selectedProject.author + " (" + selectedProject.qty + ")\n");
        boolean haveValidInput = false;
        return getMenuChoice("Menu choice: ", 0, 4);
    }

    /**
     * Prints the main menu and returns the user's selection. This version of the menu is called when there is no
     * currently selected book and thus excludes any menu options that require a selection.
     *
     * @return The user's selection
     */
    public int printMainMenu() {
        System.out.println( //TODO: Update
            """
            Main Menu
            =========
            
            1. Add a new book
            2. Search/select books
            0. Exit
            """
        );
        System.out.println();
        return getMenuChoice("Menu choice: ", 0, 2);
    }


    /**
     * The method called by selecting 'Add to MainProgram.Project' from the menu. Collects the information from the
     * user and calls to the database as needed.
     *
     * @return The project that should be the new currently selected book (may be a new or existing project)
     * @throws DatabaseException If an error occurs when calling the database.
     */
    public Project addProject() throws DatabaseException{
        try {
            Project newProject = getBasicProjectInfoFromUser();//Implement
            if (newProject.number == -1) {
                newProject.number = DataSource.getInstance().insertBook(newProject);
            }
            return newProject;

        } catch (SQLException ex) {
            throw new DatabaseException("Database error encountered while adding a new book record.", ex);
        }
    }

    private Project getBasicProjectInfoFromUser() {
        Person customer = getCustomer();
        ProjectType type = chooseProjectType();

        String projectName = getStringFromUser("Project name [leave blank if this is not known yet]: ",
                ProjectTable.COL_PROJECT_NAME_SIZE, "Project names are limited to ? characters", true);
        if (projectName.isBlank()) {
            projectName = type.toString() + " " + customer.surname;
        }

        return new Project(projectName, type, customer);
    }

    private ProjectType chooseProjectType() {
        System.out.println(
                """
                What is the project type:
                =========================
           
                """);
        StringBuilder menu = new StringBuilder();
        ArrayList<ProjectType> allTypes = new ArrayList<>(ProjectType.getList());
        for (int i = 0; i < allTypes.size(); i++) {
            menu.append(i+1).append(": ").append(allTypes.get(i).toString()).append('\n');
        }
        menu.append("What is the project type:");
        int choice = getMenuChoice(menu.toString(), 1, allTypes.size());
        return allTypes.get(choice);
    }

    private Person getCustomer() {
        String name = getStringFromUser("Customer name: ", PersonTable.COL_FIRST_NAME_SIZE, "People's names are limited to ? characters.", false);
        ArrayList<Pickable> peopleFound = new ArrayList(DataSource.getInstance().searchPeople());
        Person answer;
        if (!peopleFound.isEmpty()) {
            System.out.println("Here are some similar people already in the system.\n Choose one of the or cancel [enter 0] to continue creating a new persona:\n");
            answer = (Person) printAndPickResult(peopleFound);
        } else {
            System.out.println("No existing matches found.\n");
            answer = new Person();
            if (getYesNoFromUser("Is " + name + " a surname? [y/n]")) {
                answer.surname = name;
                answer.firstName = getStringFromUser("First name : ", PersonTable.COL_FIRST_NAME_SIZE, "People's names are limited to ? characters.", false);
            } else {
                answer.firstName = name;
                answer.surname = getStringFromUser("Surname : ", PersonTable.COL_FIRST_NAME_SIZE, "People's names are limited to ? characters.", false);
            }

            answer.email = getStringFromUser("E-mail : ", PersonTable.COL_EMAIL_SIZE, "People's names are limited to ? characters.", false);
            answer.address = getStringFromUser("E-mail : ", PersonTable.COL_PHYS_ADDR_SIZE, "People's names are limited to ? characters.", false);
        }
        System.out.println();
        return answer;
    }

    public Project showCurrentProjects(Project currentSelection) throws DatabaseException{
        ArrayList<Pickable> currentProjects = null;
        try {
            currentProjects = new ArrayList<>(DataSource.getInstance().getCurrentProjects());
        } catch (SQLException ex) {
            System.out.println("Database error while searching for current projects.");
        }
        if (currentProjects == null || currentProjects.isEmpty()) {
            System.out.println("No active or unscheduled projects on record");
            return currentSelection;
        } else {
            Project newSelection = (Project) printAndPickResult(currentProjects);
            return newSelection;
        }
    }

    public Project showOverdueProjects(Project currentSelection) throws DatabaseException{
        ArrayList<Pickable> overdueProjects = null;
        try {
            overdueProjects = new ArrayList<>(DataSource.getInstance().getOverdueProjects());
        } catch (SQLException ex) {
            System.out.println("Database error while searching for overdue projects.");
        }
        if (overdueProjects == null || overdueProjects.isEmpty()) {
            System.out.println("No overdue projects on record.");
            return currentSelection;
        } else {
            Project newSelection = (Project) printAndPickResult(overdueProjects);
            return newSelection;
        }
    }

    public Project showAllProjects(Project currentSelection) throws DatabaseException{
        ArrayList<Pickable> allProjects = null;
        try {
            allProjects = new ArrayList<>(DataSource.getInstance().getOverdueProjects());
        } catch (SQLException ex) {
            System.out.println("Database error while searching for all projects.");
        }
        if (allProjects == null || allProjects.isEmpty()) {
            System.out.println("No projects on record.");
            return currentSelection;
        } else {
            Project newSelection = (Project) printAndPickResult(allProjects);
            return newSelection;
        }
    }


    /**
     * Called from the Main menu when the user requests that the selected book be deleted. Displays a message asking
     * the user to confirm the action and then calls the appropriate database method to do the deletion.
     * @param bookToDelete A {@link Book} object representing the record to be deleted.
     * @return {@code true} if the database is successfully modified.
     * @throws DatabaseException If a database error occurs.
     */
    public boolean deleteBook(Book bookToDelete) throws DatabaseException{
        boolean userConfirmed = confirmDeletion(bookToDelete);
        if (userConfirmed) {
            try {
                DataSource.getInstance().deleteBook(bookToDelete);
                return true;
            } catch (SQLException ex) {
                throw new DatabaseException("Database error encountered while deleting book record.", ex);
            }
        } else {
            return false;
        }
    }

    /**
     * Displays a message to the user asking them to confirm that they want to delete the selected record.
     *
     * @param book A {@link Book} object representing the record to be deleted.
     * @return {@code true} if the user confirmed. {@code false} if the user wants to cancel the operation.
     */
    private boolean confirmDeletion(Book book) {
        String input = " ";
        while (!input.equalsIgnoreCase("y") && !input.equalsIgnoreCase("n")) {
            System.out.print("Are you sure you want to delete the selected record? [y/n] ");
            input = consoleReader.nextLine();
            System.out.println();
        }
        return input.equalsIgnoreCase("y");
    }

    /**
     * Allows the user to search through the database records and select a project for further action.
     * Gives the user the option to search by Project name, property address and person. The person can be any one of
     * the actors (customers, project manager etc.)
     * The users search string is passed to the appropriate method to search the database.
     *
     * @return A {@link Project} object representing the new selection.
     * @throws DatabaseException If a database error occurs during the search.
     */
    public Project searchDialog() throws DatabaseException{
        System.out.println("""
            How would you like to search:

            1. By project name
            2. By address
            3. By person
            0. Back to Main menu
        """);
        
        //TODO: Repeating code block used to get and validate input. Put in method.
        System.out.println();
        int input = getMenuChoice("Menu choice: ", 0, 3);
        ArrayList<Project> searchResults;
        System.out.println();

        try {
            if (input == 0) { //Operation aborted. No new selection.
                return null;
            } else if (input == 1) {
                searchResults = new ArrayList<Project>(searchByTitleDialog());
            } else if (input == 2) {
                searchResults = new ArrayList<Project>(searchByAuthorDialog());
            } else {
                throw new AssertionError("Unhandled menu choice" + input + " encountered in search dialog");
            }
        } catch (SQLException sqlEx) {
            throw new DatabaseException("Database error encountered while performing search." , sqlEx);
        }

        if (searchResults.isEmpty()) {
            System.out.println(" -- No search results -- ");
            return null;
        } else {
            return printAndPickResult(searchResults);
        }
    }

    /**
     * Helper method for getting user input for a menu and validating it.
     *
     * @param prompt The prompt displayed to the user asking for input.
     * @param minChoice The smallest acceptable integer input from the user.
     * @param maxChoice The largest acceptable integer input from the user.
     * @return The user's selection.
     */
    private int getMenuChoice(String prompt, int minChoice, int maxChoice) {
        boolean haveValidInput = false;
        int input = minChoice -1;
        while (!haveValidInput) {
            try {
                System.out.print(prompt);
                input = consoleReader.nextInt();
                consoleReader.nextLine();
            } catch (InputMismatchException exc) {
                //The input received doesn't appear to be an integer
                continue;
            } catch (NoSuchElementException exc) {
                //No input received
                continue;
            }

            if (input > maxChoice || input < minChoice ) {
                //Input was valid but out of range.
                //Request input again.
                continue;
            } else {
                haveValidInput = true;
            }
        }
        System.out.println();
        return input;
    }

    /**
     * Helper method to print out a list of {@link Project} objects and ask the user to select one.
     *
     * Called on search results received from a database search method.
     *
     * @param searchResults A List of {@link Project} objects to select from.
     * @return The selected {@link Project}.
     */
    private Pickable printAndPickResult(List<Pickable> searchResults) {
        System.out.println(" -- Search results -- ");
        System.out.println();
        for (int index = 0; index < searchResults.size(); ++index) {
            StringBuilder result = new StringBuilder();
            if ( index < 9 ) {
                result.append(' ');
            }
            result.append(index + 1);
            result.append(" - ");
            result.append(searchResults.get(index).getOneLineString());
            System.out.println(result);
        }
        System.out.println();
        int choice = getMenuChoice("Select a result [0 to cancel]: ", 0, searchResults.size());
        System.out.println();
        if (choice == 0) {
            return null;
        } else {
            return searchResults.get(choice -1);
        }
    }

    /**
     * Allows methods to signal how a search should be done by in the database: Project name, property address or person
     * (which could be any one of the actors associated with a project).
     */
    public static enum SearchCriteria {
        ByProjectName,
        ByAddress,
        ByPerson
    }


    /**
     * Displays the update menu which asks the user which parameter they would like to update.
     * Called from the main menu.
     *
     * @param selectedBook The currently selected book which will be modified.
     * @return {@code true} if the database is actually modified by this operation.
     * @throws DatabaseException If an error occurs in the underlying database call.
     */
    public boolean updateMenu(Project selectedProject) throws DatabaseException{ //TODO: Update
        System.out.println(""" 
                What would you like to change in the selected item?
                1. Book title
                2. Author
                3. Quantity
                """);

        int choice = getMenuChoice("Make a selection [0 to cancel]: ", 0, 3);
        System.out.println();

        boolean changed = false;
        try {
            switch (choice) {
                case 0:
                    return false;
                case 1:
                    changed = updateTitle(selectedBook);
                    break;
                case 2:
                    changed = updateAuthor(selectedBook);
                    break;
                case 3:
                    changed = updateQty(selectedBook);
                    break;
            }
        } catch (SQLException sqlEx) {
            throw new DatabaseException("Database error encountered while updating existing record", sqlEx);
        }
        System.out.println();
        return changed;
    }

    private boolean getYesNoFromUser(String prompt) {
        while (true) {
            System.out.print(prompt);
            String answer = consoleReader.next();
            System.out.println();

            if (answer.length() != 1) {
                continue;
            }

            if (answer.equalsIgnoreCase("Y")) {
                return true;
            }

            if (answer.equalsIgnoreCase("N")) {
                return false;
            }
        }
    }

    /**
     * Prompts the user for a new book title and then calls the appropriate database method to make the
     * change if necessary.
     *
     * @param selectedBook The currently selected book that will be modified.
     * @return {@code true} if the database is actually modified by this operation.
     * @throws SQLException If an error occurs in the underlying database call.
     */
    private boolean updateTitle(Book selectedBook) throws SQLException{
        StringBuilder prompt = new StringBuilder();
        prompt.append("Current title: ").append(selectedBook.title).append("\nEnter new title [leave blank to cancel]: ");
        String newTitle = getTitleFromUser(prompt.toString(), true);

        if (!newTitle.equals(selectedBook.title)) {
            if (DataSource.getInstance().updateTitle(selectedBook, newTitle)) {
                selectedBook.title = newTitle;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Prompts the user for a new book author and then calls the appropriate database method to make the
     * change if necessary.
     *
     * @param selectedBook The currently selected book that will be modified.
     * @return {@code true} if the database is actually modified by this operation.
     * @throws SQLException If an error occurs in the underlying database call.
     */
    private boolean updateAuthor(Book selectedBook) throws SQLException{
        StringBuilder prompt = new StringBuilder();
        prompt.append("Current author: ").append(selectedBook.author).append("\nEnter new author [leave blank to cancel]: ");
        String newAuthor = getAuthorFromUser(prompt.toString(),  true);

        if (!newAuthor.equals(selectedBook.author)) {
            if (DataSource.getInstance().updateAuthor(selectedBook, newAuthor)) {
                selectedBook.author = newAuthor;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Prompts the user for a new book quantity and then calls the appropriate database method to make the
     * change if necessary.
     *
     * @param selectedBook The currently selected book that will be modified.
     * @return {@code true} if the database is actually modified by this operation.
     * @throws SQLException If an error occurs in the underlying database call.
     */
    private boolean updateQty(Book selectedBook) throws SQLException{
        StringBuilder prompt = new StringBuilder();
        prompt.append("Current quantity: ").append(selectedBook.qty).append("\nEnter new qty [leave blank to cancel]: ");
        int newQty = getQtyFromUser(prompt.toString(), true);

        if (newQty == -1){
            return false;
        }
        if (newQty != selectedBook.qty) {
            if (DataSource.getInstance().updateQty(selectedBook, newQty)) {
                selectedBook.qty = newQty;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }






    /**
     * Helper method to get a String from the user with just a basic length validation check
     *
     * @param prompt The text to display prompting the user for input.
     * @return The String from the user.
     */
    public String getStringFromUser(String prompt, int maxLength, String lengthErrorMsg, boolean acceptBlank) {
        System.out.print(prompt);
        boolean haveValidInput = false;
        while (!haveValidInput) {
            String answer = consoleReader.nextLine();
            System.out.println();

            if (answer.isBlank() && !acceptBlank) {
                System.out.println("\nBlank input is not allowed for this field\n");
                continue;
            }

            if (maxLength > 0 && answer.length() > maxLength) {
                System.out.println("\n" + lengthErrorMsg.replace("?", Integer.toString(maxLength)) + "\n");
                continue;
            }
            haveValidInput = true;
        }
        return answer;
    }

    /**
     * Helper method to get a String from the user without any validation checks
     *
     * @param prompt The text to display prompting the user for input.
     * @return The String from the user.
     */
    public String getStringFromUser(String prompt, boolean acceptBlank) {
        return getStringFromUser(prompt, 0, null, acceptBlank);
    }



    /**
     * Closes the connection to the console. Should only be called once at the end of the program when no more input
     * is needed from the user.
     */
    public void close() {
        consoleReader.close();
    }
}
