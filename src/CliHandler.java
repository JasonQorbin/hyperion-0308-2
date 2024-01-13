import database.DataSource;

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
     * The method called by selecting 'Add to Project' from the menu. Collects the information from the
     * user and calls to the database as needed.
     *
     * @return The project that should be the new currently selected book (may be a new or existing project)
     * @throws DatabaseException If an error occurs when calling the database.
     */
    public Project addProject() throws DatabaseException{
        try {
            Project newProject = getProjectInfoFromUser();//Implement
            if (newProject.number == -1) {
                newProject.number = DataSource.getInstance().insertBook(newProject);
            }
            return newProject;
        } catch (SQLException ex) {
            throw new DatabaseException("Database error encountered while adding a new book record.", ex);
        }
    }

    /**
     * Method to collect the information for a new book record from the user.
     *
     * After getting the book title, it requests a search of the database for the first 3 characters to first a list of
     * possible existing records that match the one being added. If something is found the method gives the user the
     * opportunity to select the existing record instead  of creating a new one.
     *
     * @return An existing {@link Project} record or on containing the information of a new record with the project
     * number set to -1
     * @throws SQLException If an error occurs while searching for existing records.
     */
    private Book getBookInfoFromUser() throws SQLException{
        Project book = new Project();

        book.title =  getTitleFromUser("Book title: ");
        ArrayList<Project> searchResults = new ArrayList<>(DataSource.getInstance().searchBooks(SearchCriteria.ByTitle, book.title.substring(0,3)));
        if (!searchResults.isEmpty()) {
            System.out.println(
                """
                We've found a few existing records that are very similar to the book you are adding.
                Perhaps consider selecting one of these existing records instead.
                Select a record below or cancel (enter 0) to continue adding a new book:
                """);
            Book selection = printAndPickResult(searchResults);
            if (selection != null) {
                return selection;
            }
        }
        book.author = getAuthorFromUser("Book author: ");
        book.qty = getQtyFromUser("Starting quantity: ");
        book.id = -1;
        return book;
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
     * Allows the user to search through the database records and select one for further action.
     * Called from the main menu.
     * Gives the user the option to search by Title or by Author. The users search string is passed o to the appropriate
     * method to search the database.
     *
     * @return A {@link Book} object representing the new selection.
     * @throws DatabaseException If a database error occurs during the search.
     */
    public Book searchDialog() throws DatabaseException{
        System.out.println("""
            How would you like to search:

            1. By book title
            2. By author
            0. Back to Main menu
        """);
        
        //TODO: Repeating code block used to get and validate input. Put in method.
        System.out.println();
        int input = getMenuChoice("Menu choice: ", 0, 2);
        ArrayList<Book> searchResults;
        System.out.println();

        try {
            if (input == 0) { //Operation aborted. No new selection.
                return null;
            } else if (input == 1) {
                searchResults = new ArrayList<Book>(searchByTitleDialog());
            } else if (input == 2) {
                searchResults = new ArrayList<Book>(searchByAuthorDialog());
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
    private Project printAndPickResult(List<Project> searchResults) {
        System.out.println(" -- Search results -- ");
        System.out.println();
        for (int index = 0; index < searchResults.size(); ++index) {
            StringBuilder result = new StringBuilder();
            if ( index < 9 ) {
                result.append(' ');
            }
            result.append(index + 1);
            result.append(" - ");
            result.append(searchResults.get(index).toString());
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
     * Allows methods to signal whether a search should be done by Book title or author so that the database query
     * can be constructed correctly.
     */
    public static enum SearchCriteria {
        ByTitle,
        ByAuthor
    }

    //TODO: redo these searches

    /**
     * Displays a prompt to the user asking them to enter a book title to search for
     *
     * @return A list of search results (Books)
     * @throws SQLException If the underlying database call fails.
     */
    private List<Book> searchByTitleDialog() throws SQLException{
        return searchByCriteria("Book title to search for (Leave blank to show all records) : ", SearchCriteria.ByTitle);
    }
    /**
     *
     * Displays a prompt to the user asking them to enter a book author to search for
     *
     * @return A list of search results (Books)
     * @throws SQLException If the underlying database call fails.
     */
    private List<Book> searchByAuthorDialog() throws SQLException {
        return searchByCriteria("Book author to search for (Leave blank to show all records) : ", SearchCriteria.ByAuthor);
    }

    /**
     * Helper function that reads the search term from the user as input and passes it to the datasource.
     *
     * @return A list of search results (Books)
     * @throws SQLException If the underlying database call fails.
     */
    private List<Book> searchByCriteria(String prompt, SearchCriteria criteria) throws SQLException {
        System.out.print(prompt);
        String searchString = consoleReader.nextLine();

        return DataSource.getInstance().searchBooks(criteria, searchString);
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
     * Helper method for asking the user to input a book title with a given prompt.
     * The method does input validation by checking the length of the input against the database column parameters.
     *
     * @param prompt The prompt that should be shown to the user to ask for the book title.
     * @param allowBlankAnswer {@code true} if blank titles should be allowed by the validation check.
     * @return The user input.
     */
    private String getTitleFromUser(String prompt, boolean allowBlankAnswer) {
        String title = "";
        boolean valid = false;

        while (!valid) {
            System.out.print(prompt);
            title = consoleReader.nextLine();
            if (title.isBlank() && allowBlankAnswer){
                return title;
            }
            if (title.isBlank()){
                System.out.println("Titles can't be blank.");
            } else if (title.length() > DataSource.COLUMN_TITLE_SIZE) {
                System.out.println("Maximum length is " + DataSource.COLUMN_TITLE_SIZE + " characters");
            } else {
                valid = true;
            }
        }

        return title;
    }

    /**
     * Helper method for asking the user to input a book title with a given prompt.
     * Defaults to not allowing blank input.
     * @param prompt The prompt that should be shown to the user to ask for the book title.
     * @return The user input.
     */
    private String getTitleFromUser(String prompt) {
        return getTitleFromUser(prompt, false);
    }


    /**
     * Helper method to get a String from the user without any validation checks
     *
     * @param prompt The text to display prompting the user for input.
     * @return The String from the user.
     */
    public String getStringFromUser(String prompt) {
        System.out.print(prompt);
        String answer = consoleReader.nextLine();
        System.out.println();
        return answer;
    }

    /**
     * Closes the connection to the console. Should only be called once at the end of the program when no more input
     * is needed from the user.
     */
    public void close() {
        consoleReader.close();
    }
}
