package MainProgram;

import database.DataSource;
import database.DatabaseException;
import database.PersonTable;
import database.ProjectTable;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * This class is where most interactions with the user should occur. It maintains a Scanner instance that can be used to
 * capture user input. Because of this, it's {@code close()} method should be called when no longer in use.
 */
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
        System.out.println(
            """
            Main  Menu
            ==========
            
            1. Add a new project
            2. View & Select from all ongoing projects
            3. View & Select from all overdue projects
            4. View & Select from all projects
            5. Edit people
            6. Progress/Edit/Delete the selected project
            0. Exit
            """
        );
        System.out.println("Selected project: " + selectedProject.getOneLineString());
        boolean haveValidInput = false;
        return getMenuChoice("Menu choice: ", 0, 6);
    }

    /**
     * Prints the main menu and returns the user's selection. This version of the menu is called when there is no
     * currently selected book and thus excludes any menu options that require a selection.
     *
     * @return The user's selection
     */
    public int printMainMenu() {
        System.out.println(
            """
            Main Menu
            =========
            
            1. Add a new project
            2. View & Select from all ongoing projects
            3. View & Select from all overdue projects
            4. View & Select from all projects
            5. Edit people
            0. Exit
            """
        );
        System.out.println();
        return getMenuChoice("Menu choice: ", 0, 5);
    }


    /**
     * The method called by selecting 'Add to Project' from the menu. Collects the information from the
     * user and calls to the database as needed.
     *
     * @return The project that should be the new currently selected project.
     * @throws DatabaseException If an error occurs when calling the database.
     */
    public Project addProject() throws DatabaseException{
        System.out.println("Please search for a customer to assign the new project (The customer will be created if they don't exist yet):");
        Project newProject = getBasicProjectInfoFromUser();
        newProject.number = DataSource.getInstance().insertProject(newProject);

        return newProject;
    }

    /**
     * Called when the user selects "Add new Project" from the main menu. Collects the bare minimum information required
     * to create a project record. The resulting project will not yet have a project number because it has not been
     * added into the database yet.
     *
     * @return The resulting project object.
     * @throws DatabaseException If a database error occurs.
     */
    private Project getBasicProjectInfoFromUser() throws DatabaseException {
        Person customer = findOrCreatePerson();
        ProjectType type = chooseProjectType();

        String projectName = getStringFromUser("Project name [leave blank if this is not known yet]: ",
                ProjectTable.COL_PROJECT_NAME_SIZE, "Project names are limited to ? characters", true);
        if (projectName.isBlank()) {
            projectName = type.toString() + " " + customer.surname;
        }

        return new Project(projectName, type, customer);
    }

    /**
     * Asks the user to choose a project type from a list
     *
     * @return The selected type (Enum value).
     */
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

    /**
     * Attempts to search for a person record using the name from the user as a search term. If possible matches are
     * found, allows the user to select one. If the user doesn't select a person or no matches are found, proceeds to
     * create a new record.
     *
     * @return The selected/created person
     * @throws DatabaseException If a database error occurs.
     */
    private Person findOrCreatePerson() throws DatabaseException {
        String name = getStringFromUser("Name to search: ", PersonTable.COL_FIRST_NAME_SIZE,
                "People's names are limited to ? characters.", false);
        ArrayList<Pickable> peopleFound = new ArrayList(DataSource.getInstance().searchPeople(name));
        Person answer;
        if (!peopleFound.isEmpty()) {
            System.out.println("Here are some similar people already in the system.\n Choose one of the or cancel [enter 0] to continue creating a new persona:\n");
            answer = (Person) printAndPickResult(peopleFound);
        } else {
            System.out.println("No existing matches found.\n");
            answer = new Person();
            if (getYesNoFromUser("Is " + name + " a surname? [y/n] ")) {
                answer.surname = name;
                answer.firstName = getStringFromUser("First name : ", PersonTable.COL_FIRST_NAME_SIZE,
                        "People's names are limited to ? characters.", false);
            } else {
                answer.firstName = name;
                answer.surname = getStringFromUser("Surname : ", PersonTable.COL_FIRST_NAME_SIZE,
                        "People's names are limited to ? characters.", false);
            }

            answer.email = getStringFromUser("E-mail : ", PersonTable.COL_EMAIL_SIZE,
                    "People's names are limited to ? characters.", false);
            answer.address = getStringFromUser("Physical Address : ", PersonTable.COL_PHYS_ADDR_SIZE,
                    "People's names are limited to ? characters.", false);

            answer.id = DataSource.getInstance().insertPerson(answer);
        }
        System.out.println();
        return answer;
    }

    /**
     * Finds and displays all projects that are not overdue or finalised then allows the user to select one.
     *
     * @param currentSelection The currently selected project. Will be displayed to assist the user. Can be null.
     * @return The selected project
     * @throws DatabaseException If a database error occurs.
     */
    public Project showCurrentProjects(Project currentSelection) throws DatabaseException{
        ArrayList<Pickable> currentProjects = new ArrayList<>(DataSource.getInstance().getCurrentProjects());

        if ( currentProjects.isEmpty() ) {
            System.out.println("---  No active or unscheduled projects on record  ---\n");
            return currentSelection;
        } else {
            return (Project) printAndPickResult(currentProjects);
        }
    }

    /**
     * Finds and displays all overdue projects then allows the user to select one.
     *
     * @param currentSelection The currently selected project. Will be displayed to assist the user. Can be null.
     * @return The selected project
     * @throws DatabaseException If a database error occurs.
     */
    public Project showOverdueProjects(Project currentSelection) throws DatabaseException{
        ArrayList<Pickable> overdueProjects = new ArrayList<>(DataSource.getInstance().getOverdueProjects());

        if ( overdueProjects.isEmpty() ) {
            System.out.println("---  No overdue projects on record.  ---\n");
            return currentSelection;
        } else {
            return (Project) printAndPickResult(overdueProjects);
        }
    }

    /**
     * Finds and displays all projects then allows the user to select one.
     *
     * @param currentSelection The currently selected project. Will be displayed to assist the user. Can be null.
     * @return The selected project
     * @throws DatabaseException If a database error occurs.
     */
    public Project showAllProjects(Project currentSelection) throws DatabaseException{
        ArrayList<Pickable> allProjects = new ArrayList<>(DataSource.getInstance().getOverdueProjects());

        if ( allProjects.isEmpty() ) {
            System.out.println("---  No projects on record.  ---\n");
            return currentSelection;
        } else {
            return (Project) printAndPickResult(allProjects);
        }
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

        System.out.println();
        final int input = getMenuChoice("Menu choice: ", 0, 3);

        ArrayList<Pickable> searchResults;
        System.out.println();
        String searchTerm;

        switch (input) {
            case 0:
                return null;
            case 1:
                final int maxProjectLength = ProjectTable.COL_PROJECT_NAME_SIZE;
                searchTerm = getStringFromUser("Project name: ",
                        maxProjectLength,
                        "Project names can be at most" + maxProjectLength + " characters long."
                        , false);
                searchResults = new ArrayList<>(DataSource.getInstance().getProjectsByName(searchTerm));
                break;
            case 2:
                final int maxAddressLength = ProjectTable.COL_PHYS_ADDR_SIZE;
                searchTerm = getStringFromUser("Project Address: ",
                        maxAddressLength,
                        "Project addresses can be at most" + maxAddressLength + " characters long."
                        , false);
                searchResults = new ArrayList<>(DataSource.getInstance().getProjectsByAddress(searchTerm));
                break;
            case 3:
                final Person personToSearch = findOrCreatePerson();
                if (personToSearch == null) {
                    return null;
                }
                searchResults = new ArrayList<>(DataSource.getInstance().getProjectsByPerson(personToSearch));
                break;
            default:
                throw new AssertionError("Unhandled menu choice" + input + " encountered in search dialog");
        }

        if (searchResults.isEmpty()) {
            System.out.println(" -- No search results -- ");
            return null;
        } else {
            return (Project) printAndPickResult(searchResults);
        }
    }


    /**
     * Helper method for getting user input for a menu and validating that it is within the range of values from the menu.
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
            input = minChoice -1;
            System.out.print(prompt);
            String stringInput = consoleReader.nextLine();
            try {
                input = Integer.parseInt(stringInput);
            } catch (NumberFormatException exc) {
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
     * Helper method to print out a list of {@link Pickable} objects (i.e. Projects or People) and ask the user to select one.
     * <p><br>
     * Called on search results received from a database search method.
     *
     * @param searchResults A List of {@link Pickable} objects to select from.
     * @return The selected {@link Pickable}. Typical you should cast this to the relevant object type afterwards.
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
     * Displays the update menu. This menu is used to change/enrich each project record but also to delete them or
     * advance the status to the next stage.
     *
     * @param selectedProject The currently selected project which will be modified.
     * @throws DatabaseException If an error occurs in the underlying database call.
     */
    public Project updateMenu(Project selectedProject) throws DatabaseException{
        int choice = -1;
        while (choice != 0) {
            System.out.println("Selected project: " + selectedProject.getOneLineString());
            System.out.println(""" 
                    What would you like to change in the selected item?
                    1. Update project record
                    2. Advance project stage
                    3. Delete project
                    0. Return to Main Menu
                    """);

            choice = getMenuChoice("Make a selection [0 to cancel]: ", 0, 3);
            System.out.println();

            switch (choice) {
                case 1:
                    selectedProject = updateProjectDetailMenu(selectedProject);
                    break;
                case 2:
                    selectedProject.advanceStage();
                    break;
                case 3:
                    if (deleteProject(selectedProject)) {
                        selectedProject = null;
                        choice = 0;
                    }
                    else {
                        choice = -1;
                    }
                    break;
            }
        }
        System.out.println();
        return selectedProject;
    }

    /**
     * Presents the user with a menu for editing the fields of a project (all the fields except the project status/stage).
     * The menu is repeatedly shown to the user as they specify changes and the requested changes are buffered until the
     * user indicates that they are done and wish to save their changes. Those changes are then submitted to the database
     * in a single query. 
     * 
     * @param projectToChange The project being edited.
     * @return A project object representing the new state. Set the selected project to this value.
     * @throws DatabaseException If a database error occurs.
     */
    private Project updateProjectDetailMenu(Project projectToChange) throws DatabaseException{
        StringBuilder menuText = new StringBuilder();
        HashMap<String, Object> changes = new HashMap<>();
        String input = "";
        boolean shouldContinue = true;
        while (shouldContinue) {
            System.out.println("Current project record: \n" +projectToChange.getFullDescription());
            menuText.setLength(0);
            menuText.append(
                    """
                            Update options:
                            1. Project name
                            2. Address
                            3. ERF number
                            4. Total Fee
                            5. Total Paid to-date
                            6. Project deadline 
                            """);
            menuText.append(projectToChange.customer == null ? "7. Assign customer\n" : "7. Reassign customer\n");
            menuText.append(projectToChange.engineer == null ? "8. Assign engineer\n" : "8. Reassign engineer\n");
            menuText.append(projectToChange.projectManager == null ? "9. Assign project manager\n" : "9. Reassign project manager\n");
            menuText.append(projectToChange.architect == null ? "10. Assign architect\n" : "10. Reassign architect\n");
            menuText.append("11. Change project type\n\n");
            if (!changes.isEmpty()) {
                menuText.append("'S'= Save changes and exit\n");
            }
            menuText.append("'Q'= Quit without saving\n\n");
            menuText.append("Please make a selection: ");

            System.out.print(menuText);

            input = consoleReader.nextLine();
            input = input.trim();

            int numberChoice;
            try {
                numberChoice = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                numberChoice = -1;
            }

            if (input.equalsIgnoreCase("S") || input.equalsIgnoreCase("Q")) {
                shouldContinue = false;
            } else if (numberChoice < 1 || numberChoice > 11 ) {
                System.out.println("Invalid input. Please try again.");
            } else {
                String newStringValue;
                int newIntValue = 0;
                BigDecimal newBigDecimalValue;
                Person newPerson;


                switch (numberChoice) {
                    case 1: //Change Project Name
                        newStringValue = getNewStringValueForUpdateMenu(
                                "New project name",
                                projectToChange.name,
                                ProjectTable.COL_PROJECT_NAME_SIZE,
                                "Project names are limited to ? characters"

                        );
                        if (!newStringValue.isBlank()) {
                            changes.put(ProjectTable.COL_PROJECT_NAME, newStringValue);
                        } else {
                            changes.remove(ProjectTable.COL_PROJECT_NAME);
                        }
                        break;
                    case 2: //Change address
                        newStringValue = getNewStringValueForUpdateMenu(
                                "New project address",
                                projectToChange.address,
                                ProjectTable.COL_PHYS_ADDR_SIZE,
                                "Project addresses are limited to ? characters"

                        );
                        if (!newStringValue.isBlank()) {
                            changes.put(ProjectTable.COL_PHYS_ADDR, newStringValue);
                        } else {
                            changes.remove(ProjectTable.COL_PHYS_ADDR);
                        }
                        break;
                    case 3: //Change ERF
                        newIntValue = getNewIntValueForUpdateMenu("New ERF number", projectToChange.erfNum, false);
                        if (newIntValue != projectToChange.erfNum) {
                            changes.put(ProjectTable.COL_ERF, newIntValue);
                        } else {
                            changes.remove(ProjectTable.COL_ERF);
                        }
                        break;
                    case 4: //Change total fee
                        newBigDecimalValue = getNewDecimalValueForUpdateMenu("New Total Fee", projectToChange.totalFee, true);
                        if (newBigDecimalValue != null) {
                            changes.put(ProjectTable.COL_TOTAL_FEE, newBigDecimalValue);
                        } else {
                            changes.remove(ProjectTable.COL_TOTAL_FEE);
                        }
                        break;
                    case 5: //Change total paid to-date
                        newBigDecimalValue = getNewDecimalValueForUpdateMenu("New amount paid to-date", projectToChange.totalPaid, true);
                        if (newBigDecimalValue != null) {
                            changes.put(ProjectTable.COL_TOTAL_PAID, newBigDecimalValue);
                        } else {
                            changes.remove(ProjectTable.COL_TOTAL_PAID);
                        }
                        break;
                    case 6: //Change deadline
                        final DateTimeFormatter formatter  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        StringBuilder prompt = new StringBuilder();
                        prompt.append("Enter a new deadline in the format YYYY-MM-DD");
                        if (projectToChange.deadline != null) {
                            prompt.append(" [leave blank to keep the current value of ")
                                    .append(projectToChange.deadline.format(formatter)).append(']');
                        }
                        prompt.append(": ");
                        LocalDate newDate = null;
                        while (newDate == null) {
                            newStringValue = getStringFromUser(prompt.toString(), false);
                            try {
                                newDate = LocalDate.parse(newStringValue, formatter);
                            } catch ( DateTimeParseException ex ) {
                                System.out.println("Check your date format and try again");
                            }
                        }

                        if (!newDate.equals(projectToChange.deadline)) {
                            changes.put(ProjectTable.COL_DEADLINE, newDate);
                        } else {
                            changes.remove(ProjectTable.COL_DEADLINE);
                        }
                        break;
                    case 7: //Change customer
                        newPerson = getNewAssignee(projectToChange.customer, "customer");
                        if (projectToChange.customer.id != newPerson.id) {
                            changes.put(ProjectTable.COL_CUSTOMER, newPerson.id);
                        } else {
                            changes.remove(ProjectTable.COL_CUSTOMER);
                        }
                        break;
                    case 8: //Change engineer
                        newPerson = getNewAssignee(projectToChange.engineer, "engineer");
                        if (projectToChange.engineer == null || projectToChange.engineer.id != newPerson.id) {
                            changes.put(ProjectTable.COL_ENGINEER, newPerson.id);
                        } else {
                            changes.remove(ProjectTable.COL_ENGINEER);
                        }
                        break;
                    case 9: //Change PM
                        newPerson = getNewAssignee(projectToChange.projectManager, "project manager");
                        if (projectToChange.projectManager == null || projectToChange.projectManager.id != newPerson.id) {
                            changes.put(ProjectTable.COL_PROJ_MANAGER, newPerson.id);
                        } else {
                            changes.remove(ProjectTable.COL_PROJ_MANAGER);
                        }
                        break;
                    case 10: //Change architect
                        newPerson = getNewAssignee(projectToChange.architect, "architect");
                        if (projectToChange.architect == null || projectToChange.architect.id != newPerson.id) {
                            changes.put(ProjectTable.COL_ARCHITECT, newPerson.id);
                        } else {
                            changes.remove(ProjectTable.COL_ARCHITECT);
                        }
                        break;
                    case  11: //Change project type
                        System.out.println("The current project type is " + projectToChange.type.toString());
                        ProjectType newType = chooseProjectType();
                        if (newType != projectToChange.type) {
                            changes.put(ProjectTable.COL_TYPE, newType.id());
                        }
                        break;
                }
            }
        }

        if (input.equalsIgnoreCase("S")) {
            //Process all the changes and update the selected object
            DataSource dataSource = DataSource.getInstance();
            System.out.println("About to update the project");
            if (dataSource.updateProject(projectToChange, changes)) {
                return dataSource.getProjectByNumber(projectToChange.number);
            }
        }
        return projectToChange;
    }

    /**
     * Called from the main menu. Facilitates the process of editing an existing person's details.
     * <p><br>
     * Allows the user to search for the person to edit and then displays a sub-menu asking for the specific value to
     * change and the new value.
     * <p><br>
     * There is no buffering of changes. Each change is executed on the database immediately.
     * 
     * @throws DatabaseException if a database error occurs
     */
    public void editPeople() throws DatabaseException{
        final int maxLength = PersonTable.COL_FIRST_NAME_SIZE;
        String searchTerm = getStringFromUser("Enter a person's name to search for [blank to list all records]: ",
                maxLength, "Names are limited to " + maxLength + "characters.", true);
        Person personToEdit = null;
        DataSource dataSource = DataSource.getInstance();
        ArrayList<Pickable> results;
        if (searchTerm.isBlank()) {
            results = new ArrayList<>(dataSource.getAllPeople());
        } else {
            results = new ArrayList<>(dataSource.searchPeople(searchTerm));
        }
        personToEdit = (Person) printAndPickResult(results);
        if (personToEdit == null){
            return;
        }

        int choice = -1;
        while (choice != 0) {
            choice = personEditMenu(personToEdit);
            String input;
            int maxInput = 0;
            switch (choice) {
                case 1:
                    maxInput = PersonTable.COL_FIRST_NAME_SIZE;
                    input = getStringFromUser("New first name: ", maxInput,
                            "Names are limited to " + maxInput + " characters.", false);
                    if (dataSource.updatePerson(personToEdit, PersonTable.COL_FIRST_NAME, input)) {
                        personToEdit.firstName = input;
                    }
                    break;
                case 2:
                    maxInput = PersonTable.COL_SURNAME_SIZE;
                    input = getStringFromUser("New surname: ", maxInput,
                            "Names are limited to " + maxInput + " characters.", false);
                    if (dataSource.updatePerson(personToEdit, PersonTable.COL_SURNAME, input)) {
                        personToEdit.surname = input;
                    }
                    break;
                case 3:
                    maxInput = PersonTable.COL_EMAIL_SIZE;
                    input = getStringFromUser("New e-mail: ", maxInput,
                            "E-mails are limited to " + maxInput + " characters.", false);
                    if (dataSource.updatePerson(personToEdit, PersonTable.COL_EMAIL, input)) {
                        personToEdit.email = input;
                    }
                    break;
                case 4:
                    maxInput = PersonTable.COL_PHYS_ADDR_SIZE;
                    input = getStringFromUser("New address: ", maxInput,
                            "Addresses are limited to " + maxInput + " characters.", false);
                    if (dataSource.updatePerson(personToEdit, PersonTable.COL_PHYS_ADDR, input)) {
                        personToEdit.address = input;
                    }
                    break;
            }
        }

    }

    /**
     * Prints a menu showing all the possible elements of a person record that can be changed and returns the user's 
     * selection.
     * 
     * @param selectedPerson The person that is going to be edited. To allow the menu to show the details of the record 
     *                       above the menu, aiding the user with their selection
     * @return An integer matching the number in the menu of the user's selection. 
     */
    private int personEditMenu(Person selectedPerson) {
        int choice = -1;
        if (selectedPerson != null) {
            System.out.println("Selected person:\n" + selectedPerson.toString());
        }
        System.out.println(""" 
                What would you like to change in the selected item?
                1. Update firstName
                2. Update surname
                3. Update e-mail
                4. Update address
                0. Return to Main Menu
                """);

        choice = getMenuChoice("Make a selection [0 to cancel]: ", 0, 4);
        System.out.println();
        return choice;
    }

    /**
     * Used when updating a string field in a Project record. Prints a prompt that shows the user what the previous value in 
     * the field was.
     * 
     * @param promptPrefix The prompt to the user. Will be appended with a message containing the previous value.
     * @param oldValue The previous value. Can be null or blank if there was no previous value. 
     * @param maxLength The maximum length of input allowed
     * @param lengthErrorMsg The error message to display if the user exceeds the {@code maxLength}. And '?' will be 
     *                       replaced with the value of {@code maxLength}  
     * @return The string input from the user.
     */
    private String getNewStringValueForUpdateMenu(String promptPrefix, String oldValue, int maxLength, String lengthErrorMsg){
        StringBuilder prompt = new StringBuilder();
        prompt.append(promptPrefix);
        if (oldValue != null && !oldValue.isBlank()) {
            prompt.append(" [leave blank to keep the current value (").append(oldValue).append(")]");
        }
        prompt.append(':');
        return getStringFromUser(prompt.toString(), maxLength, lengthErrorMsg, true);
    }

    /**
     * Used when updating an integer field in a Project record. Prints a prompt that shows the user what the previous value in 
     * the field was.
     *
     * @param promptPrefix The prompt to the user. Will be appended with a message containing the previous value.
     * @param oldValue The previous value. Can be zero if there was no previous value.
     * @param zeroAllowed Whether or not zero is considered valid input.
     * @return The integer input from the user.
     */
    private int getNewIntValueForUpdateMenu(String promptPrefix, int oldValue, boolean zeroAllowed){
        StringBuilder prompt = new StringBuilder();
        prompt.append(promptPrefix);
        if (!zeroAllowed) {
            prompt.append(" (Required Value) ");
        }
        if (oldValue != 0 || zeroAllowed) {
            prompt.append(" [leave blank to keep the current value (").append(oldValue).append(")]");
        }
        prompt.append(':');
        String newValue;
        Integer answer = null;
        while (answer == null) {
            newValue = getStringFromUser(prompt.toString(), true);
            if ( newValue.isBlank() && (oldValue != 0 || zeroAllowed) ) {
                return oldValue;
            } else {
                try {
                    answer = Integer.parseInt(newValue);
                } catch (NumberFormatException ex) {
                    System.out.println("The input given does not appear to be a number.");
                }
            }
            if ( answer != null && answer == 0 && !zeroAllowed ) {
                answer = null;
            }
        }
        return answer;
    }

    /**
     * Used when updating a decimal field in a Project record. Prints a prompt that shows the user what the previous value in 
     * the field was.
     *
     * @param promptPrefix The prompt to the user. Will be appended with a message containing the previous value.
     * @param oldValue The previous value. Can be zero if there was no previous value.
     * @param zeroAllowed Whether or not zero is considered valid input.
     * @return The input from the user in a BigDecimal object.
     */
    private BigDecimal getNewDecimalValueForUpdateMenu(String promptPrefix, BigDecimal oldValue, boolean zeroAllowed){
        final BigDecimal ZERO = new BigDecimal(0);
        StringBuilder prompt = new StringBuilder();
        prompt.append(promptPrefix);
        boolean oldValueIsNotZero = !oldValue.equals(ZERO);
        if (!zeroAllowed) {
            prompt.append(" (Required Value) ");
        }
        if ( oldValueIsNotZero || zeroAllowed) {
            prompt.append(" [leave blank to keep the current value (").append(oldValue).append(")]");
        }
        prompt.append(':');
        String newValue;
        BigDecimal answer = null;
        while (answer == null) {
            newValue = getStringFromUser(prompt.toString(), true);
            if ( newValue.isBlank() && (oldValueIsNotZero || zeroAllowed) ) {
                return oldValue;
            } else {
                try {
                    answer = new BigDecimal(newValue);
                    answer = answer.setScale(2, RoundingMode.HALF_UP);
                } catch (NumberFormatException ex) {
                    System.out.println("The input given does not appear to be a number.");
                }
            }
            if (answer != null && answer.equals(ZERO) && !zeroAllowed) {
                answer = null;
            }
        }
        return answer;
    }

    /**
     * A method for asking the user who to assign to a role. Displays the old assignee to the user in the prompt.
     * Initiates a search dialog to allow the user to select a person or create a new one. 
     * 
     * @param oldPerson The previous assignee. Can be null if there was no previous assignee.
     * @param role A String with the name of the role being assigned. Typical you can use the Project table column name. 
     * @return The new person to assign
     * @throws DatabaseException If a database error occurs.
     */
    private Person getNewAssignee(Person oldPerson, String role) throws DatabaseException {
        if (oldPerson != null) {
            System.out.println("Current assigned " + role + " is: " + oldPerson.getOneLineString());
        }
        return findOrCreatePerson();
    }

    /**
     * Asks the user to confirm that they want to delete the given project and then does so if the answer is yes.
     * 
     * @param projectToDelete The project to delete.
     * @return {@code true} if the database is modified.
     * @throws DatabaseException If a database error occurs.
     */
    private boolean deleteProject(Project projectToDelete) throws DatabaseException{

        System.out.println("Selected project: " + projectToDelete.getOneLineString());
        boolean confirmedDeletion = getYesNoFromUser("Are you sure you want to delete the selected project? [y/n]: ");

        boolean deleted = false;
        if (confirmedDeletion) {
            deleted = DataSource.getInstance().deleteProject(projectToDelete);
        } else {
            return false;
        }

        return deleted;
    }

    /**
     * Helper function to ask the user a yes/no question and collect their response.
     * 
     * @param prompt The question the user is being asked.
     * @return {@code true} if the user answers 'yes'. {@code false} otherwise.
     */
    private boolean getYesNoFromUser(String prompt) {
        while (true) {
            System.out.print(prompt);
            String answer = consoleReader.nextLine();
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
     * Helper method to get a String from the user with just a basic length validation check
     *
     * @param prompt The text to display prompting the user for input.
     * @return The String from the user.
     */
    public String getStringFromUser(String prompt, int maxLength, String lengthErrorMsg, boolean acceptBlank) {
        boolean haveValidInput = false;
        String answer = "";
        while (!haveValidInput) {
            System.out.print(prompt);
            answer = consoleReader.nextLine();
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
