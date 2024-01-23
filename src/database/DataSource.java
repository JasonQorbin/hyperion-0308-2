package database;

import MainProgram.*;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * This class is meant to be the single touch point to reach the database. It is therefore modelled using the \
 * Singleton pattern so that only one database Connection object is ever created.
 */
public class DataSource {
    private DatabaseCredentials credentials;
    private Connection connection;

    // Singleton pattern (instance, instance-getter & private constructor)
    //-----
    private static DataSource instance;

    /**
     * Returns the static instance of the singleton database.DataSource.
     * @return A reference to the database.DataSource object
     * @throws SQLException If a connection could not be established. Ensure that the connection parameters are correct
     * and the database is reachable and running.
     */
    public static DataSource getInstance() {
        return instance;
    }

    /**
     * Returns the static instance of the singleton database.DataSource, instantiating one if it doesn't exist yet using the
     * set of credentials provided.
     * @param newCredentials The database.DatabaseCredentials object to apply.
     * @return A reference to the database.DataSource object
     */
    public static DataSource getInstance(DatabaseCredentials newCredentials){
        if (instance == null) {
            instance = new DataSource(newCredentials);
        }
        return instance;
    }

    /**
     * Constructor. Establishes the connection to the database.
     */
    private DataSource(DatabaseCredentials credentials)  {
        this.credentials = credentials;
        int retryCounter = 0;
        boolean success = false;
        while(!success) {
            try {
                ++retryCounter;
                System.out.println("Connecting to database server (attempt " + retryCounter + ")");
                connection = DriverManager.getConnection(
                        credentials.getConnectionURL(),
                        credentials.getUser(), credentials.getPassword()
                );
                success = true;
            } catch (SQLException ex) {
                System.out.println("Connection failed. ");
                System.out.println(ex.getMessage());
                System.out.println("Retrying in 10 seconds...");
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException intEx) {
                    System.out.println("Aborted. Exiting.");
                    connection = null;
                    return;
                }

                if (retryCounter >= 10)  {
                    connection = null;
                    return;
                }
            }
        }
    }
    //---End of Singleton pattern---

    /**
     * Checks if the correct database schema exists by querying the information schema.
     *
     * @return {@code true} if the database exists.
     * @throws SQLException If an error occurs with the database connection.
     */
    private boolean databaseExists() throws DatabaseException {
        String query = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?;";
        boolean answer = false;
        try(PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, credentials.getDatabase());
            ResultSet result = statement.executeQuery();
            answer = result.next();
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while checking the database schema", ex);
        }
        return answer;
    }

    /**
     * Creates the required database schema
     *
     * @throws SQLException If an error occurs with the database connection.
     */
    private void createDatabase() throws DatabaseException {
        String query = "CREATE DATABASE " + credentials.getDatabase();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while creating database schema", ex);
        }
    }

    /**
     * Helper method. Creates the database schema if required and set it as default. Then creates and initialises the
     * books table if required.
     *
     * @throws SQLException If an error occurs with the database connection.
     */
    public void initialiseDatabase() throws DatabaseException {
        if (!databaseExists()) {
            createDatabase();   
        }

        //Set the default database.
        try(Statement statement = connection.createStatement()) {
           statement.executeUpdate("USE " + credentials.getDatabase());
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while setting the default schema", ex);
        }


        checkAndInitialiseTables();
    }

    /**
     * Brings the database into a valid state during the first run. Checks if each of the required tables exists
     * and creates them with their starting data.
     *
     * The tables are added in the correct order according to their dependency on one another.
     *
     * @throws DatabaseException If a database error occurs during the process.
     */
    private void checkAndInitialiseTables() throws DatabaseException{
        //The order of the tables in this method is important.
        // The Projects table must be last because it depends on the others.

        if (!tableExists(StatusTable.TABLE_NAME)){
            executeUpdate(StatusTable.getCreationQuery());
            executeBatchInsert(StatusTable.getInitialDataQueries());
        }

        if (!tableExists(PersonTable.TABLE_NAME)){
            executeUpdate(PersonTable.getCreationQuery());
            executeBatchInsert(PersonTable.getInitialDataQueries());
        }

        if (!tableExists(ProjectTypeTable.TABLE_NAME)){
            executeUpdate(ProjectTypeTable.getCreationQuery());
            executeBatchInsert(ProjectTypeTable.getInitialDataQueries());
        }

        if (!tableExists(ProjectTable.TABLE_NAME)){
            executeUpdate(ProjectTable.getCreationQuery());
            executeBatchInsert(ProjectTable.getInitialDataQueries());
        }
    }

    /**
     * Checks to see if a table of the given name exists in the current database. This method assumes the database
     * already exists. An SQL Exception will be thrown if the database doesn't exist yet.
     *
     * @return {@code true} if the database table exists.
     * @throws SQLException If an error occurs with the database connection or the database doesn't exist yet.
     */
    private boolean tableExists(String tableName) throws DatabaseException{
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM information_schema.tables WHERE table_schema = '")
                .append(credentials.getDatabase()).append("' AND table_name = '")
                .append(tableName).append("';");
        boolean success = false;
        try (Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(queryBuilder.toString());
            success = result.next();
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while checking if the required tables exist", ex);
        }
        return success;
    }

    private int executeUpdate(String sql) throws DatabaseException{
        int updateCount = 0;
        try(Statement statement = connection.createStatement()) {
            updateCount = statement.executeUpdate(sql);
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while executing update", ex);
        }
        return updateCount;
    }

    /**
     * Helper method for bulk execution of INSERT queries. Used for inserting initial data into the database.
     *
     * @param insertQueries A list of Strings (INSERT queries)
     * @throws DatabaseException If a database error occurs.
     */
    private void executeBatchInsert(List<String> insertQueries) throws DatabaseException{
        try(Statement statement = connection.createStatement()) {
            for (String insertQuery : insertQueries) {
                statement.addBatch(insertQuery);
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while doing batch insert", ex);
        }
    }

    /**
     * Helper method that searches for project with a custom WHERE clause that must have an unsafe String applied as a
     * parameter. Use this when handling a search term received from the user.
     *
     * @param whereClause The WHERE clause to use (should not end with a ';')
     * @param stringParameter A string parameter that may be unsafe in terms of SQL injection.
     * @return A list of result (Project objects)
     * @throws DatabaseException If a database error occurs.
     */
    private List<Project> getProjectsByStringSearch(String whereClause, String stringParameter) throws DatabaseException{
        ArrayList<Project> answer = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ").append(ProjectTable.TABLE_NAME);
        if (whereClause != null) {
            query.append(' ').append(whereClause);
        }
        try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
            statement.setString(1, stringParameter);
            ResultSet results = statement.executeQuery();
            answer.addAll(getListOfProjectsFromResultSet(results));
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while searching for projects", ex);
        }
        return answer;
    }

    /**
     * Helper method that searches for project with a custom WHERE clause.
     *
     * @param whereClause The WHERE clause to use (should not end with a ';')
     * @return A list of result (Project objects)
     * @throws DatabaseException If a database error occurs.
     */
    private List<Project> getProjectsBySearch(String whereClause) throws DatabaseException {
        ArrayList<Project> answer = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ").append(ProjectTable.TABLE_NAME);
        if (whereClause != null) {
            query.append(' ').append(whereClause);
        }
        try(Statement statement = connection.createStatement()) {
            ResultSet results = statement.executeQuery(query.toString());
            answer.addAll(getListOfProjectsFromResultSet(results));
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while searching for projects", ex);
        }

        return answer;
    }

    /**
     * Fetch a project from the database based on its project number.
     *
     * @param number The project number
     * @return A Project object
     * @throws DatabaseException If a Database error occurs.
     */
    public Project getProjectByNumber(long number) throws DatabaseException {
        StringBuilder whereClause  = new StringBuilder();
        whereClause.append("WHERE ").append(ProjectTable.COL_NUMBER).append(" = ").append(number);
        ArrayList<Project> output = new ArrayList<>(getProjectsBySearch(whereClause.toString()));
        if (output.size() > 1) {
            throw new DatabaseException("Could not find Project number " + number + ". Query returned multiple values.");
        } else if (output.isEmpty()) {
            throw new DatabaseException("Could not find Project number " + number + ".");
        } else {
            return output.get(0);
        }
    }

    /**
     * Returns a  list of all projects
     *
     * @return A list of all project.
     * @throws DatabaseException If a database error occurs.
     */
    private List<Project> getAllProjects() throws DatabaseException{
        return getProjectsBySearch(null);
    }

    /**
     * Returns a list of all projects that have not been finalised, whose deadline date is either NULL or in the future.
     * @return A list of currently active and not overdue projects.
     * @throws DatabaseException If a database error occurs
     */
    public List<Project>getCurrentProjects() throws DatabaseException{
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("WHERE (").append(ProjectTable.COL_DEADLINE).append(" IS NULL OR ")
                .append(ProjectTable.COL_DEADLINE).append(" > CURDATE()) AND ").append(ProjectTable.COL_STATUS)
                .append(" < ").append(ProjectStatus.FINAL.id());
        return getProjectsBySearch(whereClause.toString());
    }

    /**
     * Return a list of the projects that are not finalised and whose deadline date is before the current date.
     *
     * @return List of overdue projects
     * @throws DatabaseException If a database error occurs.
     */
    public List<Project>getOverdueProjects() throws DatabaseException{
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("WHERE ").append(ProjectTable.COL_DEADLINE).append(" < CURDATE() AND ")
                .append(ProjectTable.COL_STATUS).append(" < ").append(ProjectStatus.FINAL.id());
        return getProjectsBySearch(whereClause.toString());
    }

    public List<Project> getProjectsByName(String searchTerm) throws DatabaseException {
        return getProjectsByString(searchTerm, ProjectTable.COL_PROJECT_NAME);
    }

    public List<Project> getProjectsByAddress (String searchTerm) throws DatabaseException {
        return getProjectsByString(searchTerm, ProjectTable.COL_PHYS_ADDR);
    }

    /**
     * Helper method for finding project by searching for a search string in a specified column. The search is
     * hardened against SQL injection so this method may accept user input.
     * <p>
     * Used by {@code getProjectsByName} and {@code getProjectsByAddress} above.
     *
     * @param searchTerm The string to
     * @param column The name of the column to search in
     * @return A list of projects that match the search criteria.
     * @throws DatabaseException If a database error occurs or the search column supplied is not in the Projects table.
     */
    private List<Project> getProjectsByString (String searchTerm, String column) throws DatabaseException  {
        StringBuilder whereClause = new StringBuilder();
        ArrayList<Project> answer;
        //Do exact search first so that we show it at the top of the search results if the user typed a specific searchTerm
        whereClause.append("WHERE ").append(column).append(" = ?");
        answer = new ArrayList<>(getProjectsByStringSearch(whereClause.toString(), searchTerm));

        //Then do a fuzzy search for a word in the middle of the database value
        whereClause.setLength(0);
        whereClause.append("WHERE ").append(column).append(" LIKE ? ESCAPE '!' ");
        answer.addAll(getProjectsByStringSearch(whereClause.toString(), "%" + likeSanitize(searchTerm) + "%"));
        return answer;
    }

    /**
     * Queries the database for projects where the given person is involved (regardless of their role).
     *
     * @param personToSearch The person to search for. This object must have come from one of the search/creation
     *                       functions in this class to ensure that it has a valid database ID.
     * @return A list of Project objects. If the query return no results, the list will be empty.
     * @throws DatabaseException Some database error occurred (most likely an access error). Inspect the throwable
     * contained within for more details.
     */
    public List<Project> getProjectsByPerson (Person personToSearch) throws DatabaseException{
        final long ID = personToSearch.id;
        final String EQUALS = " = ";
        final String OR = " OR ";
        ArrayList<Project> answer;
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ").append(ProjectTable.TABLE_NAME).append(" WHERE ")
                .append(ProjectTable.COL_ENGINEER).append(EQUALS).append(ID).append(OR)
                .append(ProjectTable.COL_CUSTOMER).append(EQUALS).append(ID).append(OR)
                .append(ProjectTable.COL_PROJ_MANAGER).append(EQUALS).append(ID).append(OR)
                .append(ProjectTable.COL_ARCHITECT).append(EQUALS).append(ID).append(';');
        try (Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(query.toString());
            answer = new ArrayList<>(getListOfProjectsFromResultSet(result));
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while searching for projects", ex);
        }
        return answer;
    }

    /**
     * Iterates through a given ResultSet that came from the Projects table and creates a Project object from each
     * of the rows.
     *
     * @param resultSet The ResultSet as it was received from the Statement, with the cursor in the default position.
     * @return A list of project objects. If the query returned no results then the list returned will be empty.
     * @throws SQLException If some DB access error occurs or if the ResultSet is from the wrong table (not the Project
     * table) and thus the column names don't match.
     */
    private List<Project> getListOfProjectsFromResultSet(ResultSet resultSet) throws SQLException {
        ArrayList<Project> answer = new ArrayList<>();
        while (resultSet.next()) {
            Project newProject = getProjectFromResultSet(resultSet);
            answer.add(newProject);
        }
        return answer;
    }

    /**
     * Creates a Project object from the current row of the given ResultSet. <strong>Typically, you don't call this
     * method directly and rather call getListOfProjectsFromResultSet which in turn calls this method.</strong> This
     * method expects the cursor to be pointing at a valid row to be in the ResultSet (not before the first row or after
     * the last).
     *
     * @param resultSet A ResultSet from a query of the Projects table with the cursor pointing at a valid row.
     * @return A Project object created from the data in the row.
     * @throws SQLException If some DB access error occurs or if the ResultSet is from the wrong table (not the Project
     * table) and thus the column names don't match.
     */
    private Project getProjectFromResultSet(ResultSet resultSet) throws SQLException{
        Project answer = new Project(
                resultSet.getString(ProjectTable.COL_PROJECT_NAME),
                getProjectTypeByID(resultSet.getLong(ProjectTable.COL_TYPE)),
                getPersonByID(resultSet.getLong(ProjectTable.COL_CUSTOMER))
        );

        answer.number = resultSet.getLong(ProjectTable.COL_NUMBER);
        answer.erfNum = resultSet.getInt(ProjectTable.COL_ERF);
        answer.address = resultSet.getString(ProjectTable.COL_PHYS_ADDR);
        answer.totalFee = resultSet.getBigDecimal(ProjectTable.COL_TOTAL_FEE);
        answer.totalPaid = resultSet.getBigDecimal(ProjectTable.COL_TOTAL_PAID);
        answer.engineer = getPersonByID(resultSet.getLong(ProjectTable.COL_ENGINEER));
        answer.projectManager = getPersonByID(resultSet.getLong(ProjectTable.COL_PROJ_MANAGER));
        answer.architect = getPersonByID(resultSet.getLong(ProjectTable.COL_ARCHITECT));
        Date deadlineAsDate = resultSet.getDate(ProjectTable.COL_DEADLINE);
        answer.deadline = deadlineAsDate != null ? deadlineAsDate.toLocalDate() : null;
        answer.status = getProjectStatusByID(resultSet.getLong(ProjectTable.COL_STATUS));


        return answer;
    }

    private ProjectType getProjectTypeByID(long ID) {
        return ProjectType.get((int)ID);
    }

    private ProjectStatus getProjectStatusByID(long ID) {
        return ProjectStatus.get((int)ID);
    }

    private Person getPersonByID(long ID) throws SQLException{
        if (ID == 0) return null;

        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ").append(PersonTable.TABLE_NAME).append(" WHERE ")
                .append(PersonTable.COL_ID).append(" = ").append(ID).append(';');
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(query.toString());
        Person answer;
        if (result.next()) {
            answer = getPersonFromResultSet(result);
        } else {
            answer = null;
        }
        statement.close();
        return answer;
    }

    /**
     * Executes a query to add a new project to the database with the given parameters
     * <p><br/>
     * This method assumes that the correct default database has been  and that the book
     * table already exists. This method uses prepared statements to handle
     * sanitising of the user input to prevent SQL injection attacks.
     *
     * @param projectName   The name of the new project. (Does not need to be unique in the database)
     * @param type   The ProjectType enum value of the project.
     * @param customer A Person Object representing customer. This represent a person already in the
     *                 database (i.e. have a valid id number).
     *
     * @return The project number of the new record or -1 on failure
     * @throws SQLException If an error occurs with the database connection.
     */
    public long insertProject(String projectName, ProjectType type, Person customer) throws DatabaseException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("INSERT INTO ").append(ProjectTable.TABLE_NAME).append(" (")
                .append(ProjectTable.COL_PROJECT_NAME).append(", ")
                .append(ProjectTable.COL_TYPE).append(", ")
                .append(ProjectTable.COL_CUSTOMER).append(") VALUES (?, ?, ?);");

        int newID = -1;
        try (PreparedStatement statement = connection.prepareStatement(queryBuilder.toString(),Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, projectName);
            statement.setLong(2, type.id());
            statement.setLong(3, customer.id);
            final int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet keys = statement.getGeneratedKeys();
                keys.next();
                newID = keys.getInt(1);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while adding new project", ex);
        }
        return newID;
    }

    /**
     * Adds a project to the database
     *
     * @param projectToInsert The project to add.
     * @return The new project number. Should be applied to the object to maintain consistency.
     * @throws DatabaseException If a database error occurs.
     */
    public long insertProject(Project projectToInsert) throws DatabaseException {
        return insertProject(projectToInsert.name, projectToInsert.type, projectToInsert.customer);
    }

    /**
     * Adds a new Person to the database.
     *
     * @param firstName
     * @param surname
     * @param address
     * @param email
     * @return The ID of the new record. Should be applied to the in-memory object representing this Person.
     * @throws DatabaseException if a database error occurs.
     */
    public long insertPerson(String firstName, String surname, String address, String email) throws DatabaseException{
        StringBuilder query = new StringBuilder()
            .append("INSERT INTO ").append(PersonTable.TABLE_NAME).append(" (")
            .append(PersonTable.COL_FIRST_NAME).append(", ")
            .append(PersonTable.COL_SURNAME).append(", ")
            .append(PersonTable.COL_PHYS_ADDR).append(", ")
            .append(PersonTable.COL_EMAIL).append(") VALUES (?, ?, ?, ?);");
        long newKey;
        try (PreparedStatement statement = connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS)){
            statement.setString( 1, firstName);
            statement.setString( 2, surname);
            statement.setString( 3, address);
            statement.setString( 4, email);
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            keys.next();
            newKey = keys.getLong(1);
        } catch (SQLException ex) {
            throw new DatabaseException("Error while creating a new Person record.", ex);
        }
        return newKey;
    }

    public long insertPerson(Person newPerson) throws DatabaseException{
        return insertPerson(newPerson.firstName, newPerson.surname, newPerson.address, newPerson.email);
    }

    /**
     * Deletes a record in the Projects table of the given ID number.
     * 
     * @param projectNumberToDelete The Project number of the record to delete.
     *
     * @return {@code true} if the table is modified.
     * @throws DatabaseException If an error occurs with the database connection.
     */
    public boolean deleteProject(long projectNumberToDelete) throws DatabaseException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("DELETE FROM ").append(ProjectTable.TABLE_NAME)
                .append(" WHERE ").append(ProjectTable.COL_NUMBER).append(" = ")
                .append(projectNumberToDelete).append(';');
        boolean success = false;
        try (Statement statement = connection.createStatement()) {
            success = statement.executeUpdate(queryBuilder.toString()) > 0;
        } catch (SQLException ex) {
            throw new DatabaseException("Database error occurred while deleting a project.", ex);
        }
        return success;
    }

    /**
     * Deletes the record associated with the given {@link Project} object.
     *
     * @param projectToDelete A {@link Project} object representing the record to be deleted.
     * @return {@code true} if the database was modified.
     * @throws DatabaseException If an error occurs with the database connection.
     */
    public boolean deleteProject(Project projectToDelete) throws DatabaseException {
        return deleteProject(projectToDelete.number);
    }

    /**
     * Called from the update menu. Changes several fields of a project in the database at once.
     *
     * @param projectToChange The Project to change
     * @param changes A hash map of changes where the key is the column to change and the value is the new value.
     * @return {@code true} if the database is modified.
     * @throws DatabaseException if a database error occurs.
     */
    public boolean updateProject(Project projectToChange, HashMap<String, Object> changes) throws DatabaseException {
        StringBuilder query = new StringBuilder();
        final ArrayList<String> keys = new ArrayList<>(changes.keySet()); //Change the Set to a List to ensure the order of the keys doesn't change.

        query.append("UPDATE ").append(ProjectTable.TABLE_NAME).append(" SET ");
        for (int i = 0; i < keys.size(); ++i) {
            query.append(keys.get(i)).append(" = ? ");
            if (i <= keys.size() -2) {
                query.append(", ");
            }
        }
        query.append("WHERE ").append(ProjectTable.COL_NUMBER).append(" = ").append(projectToChange.number);
        final Set<String> stringColumns = new HashSet<>();
        stringColumns.add(ProjectTable.COL_PROJECT_NAME);
        stringColumns.add(ProjectTable.COL_PHYS_ADDR);

        final Set<String> decimalColumns = new HashSet<>();
        decimalColumns.add(ProjectTable.COL_TOTAL_FEE);
        decimalColumns.add(ProjectTable.COL_TOTAL_PAID);


        int updateCount = 0;

        try (PreparedStatement  statement = connection.prepareStatement(query.toString())) {
            for (int i = 1; i <= keys.size(); i++) {
                String currentKey = keys.get(i - 1);
                Object currentValue = changes.get(currentKey);
                if (currentKey.equals(ProjectTable.COL_ERF)) {
                    statement.setInt(i, (int)currentValue);
                } else if (stringColumns.contains(currentKey)) {
                    statement.setString(i, (String) currentValue);
                } else if (decimalColumns.contains(currentKey)) {
                    statement.setBigDecimal(i, (BigDecimal) currentValue);
                } else {
                    statement.setLong(i, (long) currentValue);
                }
            }
            updateCount = statement.executeUpdate();
        }  catch (SQLException ex) {
            throw new DatabaseException(" Database error while update project record.", ex);
        }

        return updateCount > 0;
    }

    /**
     * Changes the project status of the given project. The validation check of this change is done in the Project
     * object.
     *
     * @param projectNumber The ID of the project to change.
     * @param newStage The ID of the new project stage.
     * @return {@code true} if the database is changed
     * @throws DatabaseException If a database error occurs.
     */
    public boolean changeStage(long projectNumber, long newStage) throws DatabaseException {
        StringBuilder query = new StringBuilder()
                .append("UPDATE ").append(ProjectTable.TABLE_NAME).append(" SET ")
                .append(ProjectTable.COL_STATUS).append(" = ").append(newStage).append(';');
        int updateCount = 0;
        try(Statement statement = connection.createStatement()) {
            updateCount = statement.executeUpdate(query.toString());
        } catch (SQLException ex) {
            throw new DatabaseException("Error while trying to update a project stage", ex);
        }

        return updateCount > 0;
    }

    /**
     * Returns all people in the database as a list.
     *
     * @return A list of all the people in the database.
     * @throws DatabaseException If a database error occurs.
     */
    public List<Person> getAllPeople() throws DatabaseException{
        StringBuilder query = new StringBuilder()
                .append("SELECT * FROM ").append(PersonTable.TABLE_NAME).append(';');
        ArrayList<Person> answer = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(query.toString());
            answer = new ArrayList<>(getListOfPersonsFromResultSet(result));
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while fetching all person records.", ex);
        }
        return answer;
    }

    /**
     * Searches the database for the given name and returns a list of Persons that likely match it. Searches both first
     * name and surname fields and priorities results where the search term appears at the start of the field (i.e. the
     * user typed exactly want they wanted), thereafter returns results where the search term is somewhere in the
     * middle of the field (i.e. the user entered some approximate value).
     *
     * @param searchName The string to search for
     * @return A list of possible matches for the user to pick from. Returns an empty list if nothing was found.
     * @throws DatabaseException If a database error is encountered.
     */
    public List<Person> searchPeople(String searchName) throws DatabaseException {
        StringBuilder queryPrefix = new StringBuilder();
        queryPrefix.append("SELECT ");
        for (int i = 0; i <PersonTable.ALL_COLUMN_NAMES.length; i++) {
            queryPrefix.append(PersonTable.ALL_COLUMN_NAMES[i]);
            if (i < PersonTable.ALL_COLUMN_NAMES.length -1) {
                queryPrefix.append(", ");
            }
        }
        queryPrefix.append(" FROM ").append(PersonTable.TABLE_NAME).append(" WHERE ");

        StringBuilder firstNameExact = new StringBuilder(queryPrefix);
        StringBuilder firstNameFuzzy = new StringBuilder(queryPrefix);
        StringBuilder surnameExact = new StringBuilder(queryPrefix);
        StringBuilder surnameFuzzy = new StringBuilder(queryPrefix);

        firstNameExact.append(PersonTable.COL_FIRST_NAME).append(" LIKE ? ESCAPE '!';");
        surnameExact.append(PersonTable.COL_SURNAME).append(" LIKE ? ESCAPE '!';");
        firstNameFuzzy.append(PersonTable.COL_FIRST_NAME).append(" LIKE ? ESCAPE '!';");
        surnameFuzzy.append(PersonTable.COL_SURNAME).append(" LIKE ? ESCAPE '!';");

        ArrayList<Person> answer = new ArrayList<>();
        try(
            PreparedStatement firstNameExactStatement = connection.prepareStatement(firstNameExact.toString());
            PreparedStatement surnameExactStatement = connection.prepareStatement(surnameExact.toString());
            PreparedStatement firstNameFuzzyStatement = connection.prepareStatement(firstNameFuzzy.toString());
            PreparedStatement surnameFuzzyStatement = connection.prepareStatement(surnameFuzzy.toString());
        ) {
            firstNameExactStatement.setString(1, likeSanitize(searchName) + "%");
            surnameExactStatement.setString(1, likeSanitize(searchName) + "%");
            firstNameFuzzyStatement.setString(1, "_%" + likeSanitize(searchName) + "%");
            surnameFuzzyStatement.setString(1, "_%" + likeSanitize(searchName) + "%");

            ResultSet firstNameExactResult = firstNameExactStatement.executeQuery();
            ResultSet surnameExactResult = surnameExactStatement.executeQuery();
            ResultSet firstNameFuzzyResult = firstNameFuzzyStatement.executeQuery();
            ResultSet surnameFuzzyResult = surnameFuzzyStatement.executeQuery();

            answer.addAll(getListOfPersonsFromResultSet(firstNameExactResult));
            answer.addAll(getListOfPersonsFromResultSet(surnameExactResult));
            answer.addAll(getListOfPersonsFromResultSet(firstNameFuzzyResult));
            answer.addAll(getListOfPersonsFromResultSet(surnameFuzzyResult));

        } catch (SQLException ex) {
            throw new DatabaseException("Database error while searching people records.", ex);
        }
        return answer;
    }

    /**
     * Creates a list of Person object from the rows of a ResultSet returned from the Person table.
     *
     * @param resultSet A ResultSet from the Person table with the cursor in the default position.
     * @return A List of Person objects. Will return an empty list if the ResultSet is empty.
     * @throws SQLException If the ResultSet is not from the Person table (i.e. the column names don't match)
     */
    private List<Person> getListOfPersonsFromResultSet(ResultSet resultSet) throws SQLException {
        ArrayList<Person> answer = new ArrayList<>();
        while (resultSet.next()) {
            Person newPerson = getPersonFromResultSet(resultSet);
            answer.add(newPerson);
        }
        return answer;
    }

    /**
     * Helper method to create a Person object from a Result Set from the Person table. Normally you would not call
     * this method directly which is why it is private. This method gets called by the getListOfPersonsFromResultSet
     * method. This method expects the cursor of the ResultSet to be pointing at a valid row.
     *
     *
     * @param resultSet A ResultSet from the Person table pointing at a valid row.
     * @return A Person object created from the information returned.
     * @throws SQLException If the column names don't match (i.e. the ResultSet didn't come from the Person table.
     */
    private Person getPersonFromResultSet(ResultSet resultSet) throws SQLException{
        Person answer = new Person();
        answer.id = resultSet.getLong(PersonTable.COL_ID);
        answer.firstName = resultSet.getString(PersonTable.COL_FIRST_NAME);
        answer.surname = resultSet.getString(PersonTable.COL_SURNAME);
        answer.address = resultSet.getString(PersonTable.COL_PHYS_ADDR);
        answer.email = resultSet.getString(PersonTable.COL_EMAIL);

        return answer;
    }

    /**
     * Creates an UPDATE query to update one value in the record related to the given Person object
     *
     * @param personToUpdate The person being updated
     * @param column The database column name being updated
     * @param newValue The new value as a String (all the fields of the Person object are Strings)
     * @return {@code true} if the database is modified.
     * @throws DatabaseException If an error occurs with the database.
     */
    public boolean updatePerson(Person personToUpdate, String column, String newValue) throws DatabaseException {
        String query = new StringBuilder()
                .append("UPDATE ").append(PersonTable.TABLE_NAME).append(" SET ").append(column)
                .append(" = ? WHERE ").append(PersonTable.COL_ID)
                .append(" = ").append(personToUpdate.id).append(';').toString();
        int updateCount = 0;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, newValue);
            updateCount = statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while update a person record", ex);
        }
        return updateCount > 0;
    }

    /**
     * <p>
     * Helper method for escaping the wildcard characters used in the SQL LIKE clause from a string. Because the
     * search method uses Prepared Statements to avoid injection attacks, it would normally escape the wildcard
     * characters that we insert when doing the fuzzy search. So we escape them here with a '!' before putting them in
     * the Prepared Statement.
     * </p>
     * <p>
     * <strong>Note: This method only escapes the LIKE wildcard characters. All other special SQL syntax is not modified
     * and the resulting string should not be considered "safe" to be inserted into a raw SQL string. You still need to
     * use a PreparedStatement to escape any other special characters from the string.</string>
     * </p>
     * <p>
     * This technique is not  my design and was obtained from this
     * <a href="https://stackoverflow.com/questions/8247970/using-like-wildcard-in-prepared-statement">
     *     Stack Overflow Discussion</a>.
     * </p>
     * @param input The String to be sanitised
     * @return The (partially) sanitised string.
     */

    private static String likeSanitize(String input) {
        return input
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");
    }

    /**
     * Closes the connection to the database. Call this function at the end of the program
     * @throws SQLException If an error occurs with the database connection.
     */
    public void close() throws SQLException {
        connection.close();
    }
}
