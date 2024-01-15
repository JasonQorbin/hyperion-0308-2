package database;

import MainProgram.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
    public static DataSource getInstance() throws SQLException{
        return instance;
    }

    /**
     * Returns the static instance of the singleton database.DataSource, instantiating one if it doesn't exist yet using the
     * set of credentials provided.
     * @param newCredentials The database.DatabaseCredentials object to apply.
     * @return A reference to the database.DataSource object
     * @throws SQLException If a connection could not be established. Ensure that the connection parameters are correct
     * and the database is reachable and running.
     */
    public static DataSource getInstance(DatabaseCredentials newCredentials) throws SQLException{
        if (instance == null) {
            instance = new DataSource(newCredentials);
        }
        return instance;
    }

    /**
     * Constructor. Establishes the connection to the database.
     * @throws SQLException If a connection could not be established. Ensure that the connection parameters are correct
     * and the database is reachable and running.
     */
    private DataSource(DatabaseCredentials credentials) throws SQLException {
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
        initialiseDatabase();
    }
    //------

    /**
     * Checks if the correct database schema exists by querying the information schema.
     *
     * @return {@code true} if the database exists.
     * @throws SQLException If an error occurs with the database connection.
     */
    private boolean databaseExists() throws SQLException {
        Statement statement = connection.createStatement();
        StringBuilder query = new StringBuilder();
        query.append("SELECT schema_name FROM information_schema.schemata WHERE schema_name = '")
            .append(credentials.getDatabase()).append("';");
        ResultSet result = statement.executeQuery(query.toString());
        boolean answer = result.next();
        statement.close();
        return answer;
    }

    /**
     * Creates the required database schema
     *
     * @throws SQLException If an error occurs with the database connection.
     */
    private void createDatabase() throws SQLException {
        Statement statement = connection.createStatement();
        StringBuilder query = new StringBuilder();
        query.append("CREATE DATABASE ")
            .append(credentials.getDatabase()).append(";");
        statement.executeUpdate(query.toString());
        statement.close();
    }

    /**
     * Helper method. Creates the database schema if required and set it as default. Then creates and initialises the
     * books table if required.
     *
     * @throws SQLException If an error occurs with the database connection.
     */
    public void initialiseDatabase() throws SQLException {
        if (!databaseExists()) {
            createDatabase();   
        }

        //Set the default database.
        connection.setCatalog(credentials.getDatabase());

        checkAndInitialiseTables();
    }

    private void checkAndInitialiseTables() throws SQLException{
        //The order of the tables in this method is important.
        // The Projects table must be last because it depends on the others.

        ArrayList<DatabaseTable> tablesToMake = new ArrayList<>();
        tablesToMake.add(new StatusTable());
        tablesToMake.add(new PersonTable());
        tablesToMake.add(new ProjectTypeTable());
        tablesToMake.add(new ProjectTable());

        for (DatabaseTable table : tablesToMake) {
            if (!tableExists(table.TABLE_NAME)) {
                executeUpdate(table.getCreationQuery());
                executeBatchInsert(table.getInitialDataQueries());
            }
        }

    }

    /**
     * Checks to see if a table of the given name exists in the current database. This method assumes the database
     * already exists. An SQL Exception will be thrown if the database doesn't exist yet.
     *
     * @return {@code true} if the database table exists.
     * @throws SQLException If an error occurs with the database connection or the database doesn't exist yet.
     */
    private boolean tableExists(String tableName) throws SQLException{
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM information_schema.tables WHERE table_schema = '")
                .append(credentials.getDatabase()).append("' AND table_name = '")
                .append(tableName).append("';");
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(queryBuilder.toString());
        boolean success = result.next();
        statement.close();
        return success;
    }

    private int executeUpdate(String sql) throws SQLException{
        Statement statement = connection.createStatement();
        int updateCount = statement.executeUpdate(sql);
        statement.close();
        return updateCount;
    }

    private void executeBatchInsert(List<String> insertQueries) throws SQLException{
        Statement statement = connection.createStatement();
        for (String insertQuery : insertQueries) {
            statement.addBatch(insertQuery);
        }
        statement.executeBatch();
        statement.close();
    }

    private List<Pickable> getProjects(String whereClause) throws DatabaseException{
        ArrayList<Pickable> answer = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM ").append(ProjectTable.TABLE_NAME);
            if (whereClause != null) {
                query.append(' ').append(whereClause);
            }
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query.toString());
            answer.addAll(getListOfProjectsFromResultSet(results));
            statement.close();
        } catch (SQLException ex) {
            throw new DatabaseException("Database error while searching for projects", ex);
        }
        return answer;
    }

    private List<Pickable> getAllProjects() throws DatabaseException{
        return getProjects(null);
    }

    public List<Pickable>getCurrentProjects() throws DatabaseException{
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("WHERE ").append(ProjectTable.COL_DEADLINE).append(" IS NULL OR ")
                .append(ProjectTable.COL_DEADLINE).append(" > CURDATE();");
        return getProjects(whereClause.toString());
    }

    public List<Pickable>getOverdueProjects() throws DatabaseException{
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("WHERE ").append(ProjectTable.COL_DEADLINE).append(" < CURDATE();");
        return getProjects(whereClause.toString());
    }

    private List<Project> getListOfProjectsFromResultSet(ResultSet resultSet) throws SQLException {
        ArrayList<Project> answer = new ArrayList<>();
        while (resultSet.next()) {
            Project newProject = getProjectFromResultSet(resultSet);
            answer.add(newProject);
        }
        return answer;
    }

    private Project getProjectFromResultSet(ResultSet resultSet) throws SQLException{
        Project answer = new Project(
                resultSet.getString(ProjectTable.COL_PROJECT_NAME),
                getProjectTypeByID(resultSet.getLong(ProjectTable.COL_PROJECT_NAME)),
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
        answer.deadline = resultSet.getDate(ProjectTable.COL_DEADLINE).toLocalDate();
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
     * Executes a query  to cleanly (re)create the table in the Books table in the database.
     * Can safely be call even if the table already exists but existing data will be destroyed.
     * This method assumes that the correct default database has been set.
     *
     * @throws SQLException If an error occurs with the database connection.
     */
    private void createBookTable() throws SQLException {
        Statement statement = connection.createStatement();
        StringBuilder queryBuilder = new StringBuilder();

        //Remove the table first if it exists to prevent a SQL error if it's already there.
        queryBuilder.append("DROP TABLE IF EXISTS ").append(BOOK_TABLE_NAME).append(";");
        statement.executeUpdate(queryBuilder.toString());
        queryBuilder.setLength(0);

        queryBuilder.append("CREATE TABLE ").append(BOOK_TABLE_NAME).append("(\n")
                    .append("\t").append(COLUMN_ID)    .append(" INT NOT NULL UNIQUE AUTO_INCREMENT PRIMARY KEY,\n")
                    .append("\t").append(COLUMN_TITLE) .append(" VARCHAR(").append(COLUMN_TITLE_SIZE).append(") NOT NULL,\n")
                    .append("\t").append(COLUMN_AUTHOR).append(" VARCHAR(").append(COLUMN_AUTHOR_SIZE).append(") NOT NULL,\n")
                    .append("\t").append(COLUMN_QTY)   .append(" INT DEFAULT 0\n")
                    .append(");");
        statement.addBatch(queryBuilder.toString());
        queryBuilder.setLength(0);

        //The task stipulates that the ID numbers should start from 3001;
        queryBuilder.append("ALTER TABLE ").append(BOOK_TABLE_NAME).append(" AUTO_INCREMENT = 3001;");
        statement.addBatch(queryBuilder.toString());

        statement.executeBatch();
        statement.close();
    }

    /**
     * Convenience method to be used after the database is created initially.
     * Creates the books table and inserts the initial data.
     *
     * @throws SQLException If an error occurs with the database connection.
     */
    public void initialiseBookTable() throws SQLException {
        createBookTable();
        insertInitialData();
    }
    
    /**
     * Executes a query to add a new book to the database with the given parameters
     *
     * This method assumes that the correct default database has been  and that the book
     * table already exists. This method uses prepared statements to handle
     * sanitising of the user input to prevent SQL injection attacks.
     *
     * @param title      The title of the new book. (Does not need to be unique in the database)
     * @param author     The author of the book
     * @param qty        The initial quantity of the book in stock.
     *
     * @return The ID of the new record or -1 on failure
     * @throws SQLException If an error occurs with the database connection.
     */
    public int insertBook(String title, String author, int qty) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("INSERT INTO ").append(BOOK_TABLE_NAME).append(" (")
                .append(COLUMN_TITLE).append(", ")
                .append(COLUMN_AUTHOR).append(", ")
                .append(COLUMN_QTY).append(") VALUES (?, ?, ?);");
        PreparedStatement statement = connection.prepareStatement(queryBuilder.toString(),Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, title);
        statement.setString(2, author);
        statement.setInt(3, qty);
        int rowsAffected = statement.executeUpdate();
        int newID = -1;
        if (rowsAffected > 0) {
            ResultSet keys = statement.getGeneratedKeys();
            keys.next();
            newID = keys.getInt(1);
            keys.close();
        }
        statement.close();
        return newID;
    }

    /**
     * Inserts a new record into the books table using the data provided.
     *
     * @param newBook    A {@link Book} object that will be inserted.
     *
     * @return The id number of the new record or -1 on failure.
     * @throws SQLException If an error occurs with the database connection.
     */
    public int insertBook( Book newBook) throws SQLException {
        return insertBook(newBook.title, newBook.author, newBook.qty);
    }

    /**
     * Deletes a record in the books table of the given ID number.
     * 
     * @param idToDelete The ID number of the record to delete.
     *
     * @return {@code true} on success
     * @throws SQLException If an error occurs with the database connection.
     */
    public boolean deleteBook(int idToDelete) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("DELETE FROM ").append(BOOK_TABLE_NAME)
                .append(" WHERE ").append(COLUMN_ID).append(" = ")
                .append(idToDelete).append(';');
        Statement statement = connection.createStatement();
        boolean success = statement.executeUpdate(queryBuilder.toString()) > 0;
        statement.close();
        return success;
    }

    /**
     * Deletes the record associated with the given {@link Book} object.
     *
     * @param bookToDelete A {@link Book} object representing the record to be deleted.
     * @return {@code true} if the database was modified.
     * @throws SQLException If an error occurs with the database connection.
     */
    public boolean deleteBook(Book bookToDelete) throws SQLException {
        return deleteBook(bookToDelete.id);
    }

    /**
     * Helper function for reading query results. Parses the current row of the given ResultSet object and creates a
     * {@link Book} object for that. The method expects the ID, Title, Author and Qty columns to be present in the
     * ResultSet.
     *
     * @param resultSet  The ResultSet to read.
     * @return A {@link Book} object representing the information read.
     * @throws SQLException If an error occurs with the database connection or if one of the expected columns is not
     * present.
     */
    private Book getBookFromResultSet (ResultSet resultSet) throws SQLException{
        Book answer = new Book();
        answer.id = resultSet.getInt(COLUMN_ID);
        answer.title = resultSet.getString(COLUMN_TITLE);
        answer.author = resultSet.getString(COLUMN_AUTHOR);
        answer.qty = resultSet.getInt(COLUMN_QTY);
        return answer;
    }

    public List<Person> searchPeople(String searchName) throws SQLException{
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

        firstNameExact.append(PersonTable.COL_FIRST_NAME).append(" = ?;");
        surnameExact.append(PersonTable.COL_SURNAME).append(" = ?;");
        firstNameFuzzy.append(PersonTable.COL_FIRST_NAME).append(" LIKE ? ESCAPE '!';");
        surnameFuzzy.append(PersonTable.COL_SURNAME).append(" LIKE ? ESCAPE '!';");

        PreparedStatement firstNameExactStatement = connection.prepareStatement(firstNameExact.toString());
        PreparedStatement surnameExactStatement = connection.prepareStatement(surnameExact.toString());
        PreparedStatement firstNameFuzzyStatement = connection.prepareStatement(firstNameFuzzy.toString());
        PreparedStatement surnameFuzzyStatement = connection.prepareStatement(surnameFuzzy.toString());

        firstNameExactStatement.setString(1, searchName);
        surnameExactStatement.setString(1, searchName);
        firstNameFuzzyStatement.setString(1, "%" + likeSanitise(searchName) + "%");
        surnameFuzzyStatement.setString(1, "%" + likeSanitise(searchName) + "%");

        ResultSet firstNameExactResult = firstNameExactStatement.executeQuery();
        ResultSet surnameExactResult = surnameExactStatement.executeQuery();
        ResultSet firstNameFuzzyResult = firstNameFuzzyStatement.executeQuery();
        ResultSet surnameFuzzyResult = surnameFuzzyStatement.executeQuery();

        ArrayList<Person> answer = new ArrayList<>();

        answer.addAll(getListOfPersonsFromResultSet(firstNameExactResult));
        answer.addAll(getListOfPersonsFromResultSet(surnameExactResult));
        answer.addAll(getListOfPersonsFromResultSet(firstNameFuzzyResult));
        answer.addAll(getListOfPersonsFromResultSet(surnameFuzzyResult));

        firstNameExactResult.close();
        surnameExactResult.close();
        firstNameFuzzyResult.close();
        surnameFuzzyResult.close();

        return answer;
    }

    private List<Person> getListOfPersonsFromResultSet(ResultSet resultSet) throws SQLException {
        ArrayList<Person> answer = new ArrayList<>();
        while (resultSet.next()) {
            Person newPerson = getPersonFromResultSet(resultSet);
            answer.add(newPerson);
        }
        return answer;
    }

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
     * Searches the database for records that matches the given search term and returns the results as a List of Book
     * objects.
     *
     * This method performs two searches, one for an exact match as the user entered the search term and a second one
     * that checks if the search term is somewhere in the middle of the search column's values. This allows the user to
     * an approximate string or a single word from the book they are looking for and still find it.
     *
     * Because the search terms come from user input, this method uses PreparedStatements to escape any special
     * characters and avoid injection attacks.
     *
     * @param criteria An enum value that specifies if the method must search through the titles or the authors.
     * @param searchTerm The search term from the user.
     * @return A list of the search results. The list may be empty if no positive matches were found.
     * @throws SQLException If an error occurs with the database connection.
     */
    public List<Book> searchBooks(CliHandler.SearchCriteria criteria, String searchTerm) throws SQLException {
        StringBuilder queryPrefix = new StringBuilder();
        queryPrefix.append("SELECT ")
            .append(COLUMN_ID).append(", ")
            .append(COLUMN_TITLE).append(", ")
            .append(COLUMN_AUTHOR).append(", ")
            .append(COLUMN_QTY).append(" FROM ").append(BOOK_TABLE_NAME)
            .append(" WHERE ");
        switch (criteria) {
            case ByTitle:
                queryPrefix.append(COLUMN_TITLE);
                break;
            case ByAuthor:
                queryPrefix.append(COLUMN_AUTHOR);
                break;
            default:
                throw new AssertionError("Invalid search criteria in searchBooks method");
        }

        StringBuilder query = new StringBuilder();

        query.append(queryPrefix)
                .append(" = ?;");
        PreparedStatement exactStatement = connection.prepareStatement(query.toString());
        query.setLength(0);

        query.append(queryPrefix)
             .append(" LIKE ? ESCAPE '!';");
        PreparedStatement fuzzyStatement = connection.prepareStatement(query.toString());

        ArrayList<Book> answer = new ArrayList<>();
        //Exact search
        exactStatement.setString(1, searchTerm);
        ResultSet exactResult = exactStatement.executeQuery();

        //Search term as a prefix/suffix
        fuzzyStatement.setString(1, "%" + likeSanitize(searchTerm) + "_%");
        ResultSet fuzzyResult = fuzzyStatement.executeQuery();

        answer.addAll(addBookToListFromResultSet(exactResult));
        answer.addAll(addBookToListFromResultSet(fuzzyResult));

        exactStatement.close();
        fuzzyStatement.close();

        return answer;
    }

    /**
     * Helper method to read through all the rows of a result set, create Book objects from them, and returning them
     * in a List Object
     *
     * @param resultSet The ResultSet to loop through
     * @return The resulting List object
     * @throws SQLException If an error occurs with the database connection.
     */
    private List<Book> addBookToListFromResultSet(ResultSet resultSet) throws SQLException {
        ArrayList<Book> answer = new ArrayList<>();
        while (resultSet.next()) {
            Book newBook = getBookFromResultSet(resultSet);
            answer.add(newBook);
        }
        return answer;
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
     * Helper method to create a template for UPDATE query Statements. The result should be used in a PreparedStatement.
     *
     * @param column The name of the column being updated
     * @return A parameterized update statement.
     */
    private String getUpdateQuery(String column) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("UPDATE ").append(BOOK_TABLE_NAME).append(" SET ").append(column)
                .append(" = ? WHERE ").append(COLUMN_ID).append(" = ?;");
        return queryBuilder.toString();
    }


    /**
     * Updates a record in the books table of the given ID number by changing the
     * title.
     *
     * @param idToChange The ID number of the record to update.
     * @param newTitle   The new book title.
     *
     * @return {@code true} if the database was modified
     * @throws SQLException If an error occurs with the database connection.
     */
    private boolean updateTitle(int idToChange, String newTitle) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(getUpdateQuery(COLUMN_TITLE));
        statement.setString(1, newTitle);
        statement.setInt(2, idToChange);
        return statement.executeUpdate() > 0;
    }

    /**
     * Updates a record in the books table of the Book object by changing the
     * title.
     *
     * @param bookToChange The Book object of the record to update.
     * @param newTitle   The new book title.
     *
     * @return {@code true} if the database was modified
     * @throws SQLException If an error occurs with the database connection.
     */
    public boolean updateTitle(Book bookToChange, String newTitle) throws SQLException {
        return updateTitle(bookToChange.id, newTitle);
    }

    /**
     * Updates a record in the books table of the given ID number by changing the
     * author.
     *
     * @param idToChange The ID number of the record to update.
     * @param newAuthor   The new book author.
     *
     * @return {@code true} if the database was modified
     * @throws SQLException If an error occurs with the database connection.
     */
    public boolean updateAuthor( int idToChange, String newAuthor) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(getUpdateQuery(COLUMN_AUTHOR));
        statement.setString(1, newAuthor);
        statement.setInt(2, idToChange);
        return statement.executeUpdate() > 0;
    }

    /**
     * Updates a record in the books table of the Book object by changing the
     * author.
     *
     * @param bookToChange The Book object of the record to update.
     * @param newAuthor   The new book title.
     *
     * @return {@code true} if the database was modified
     * @throws SQLException If an error occurs with the database connection.
     */
    public boolean updateAuthor(Book bookToChange, String newAuthor) throws SQLException {
        return updateAuthor(bookToChange.id, newAuthor);
    }

    /**
     * Updates a record in the books table of the given ID number by changing the
     * quantity.
     *
     * @param idToChange The ID number of the record to update.
     * @param newQty   The new book quantity.
     *
     * @return {@code true} if the database was modified
     * @throws SQLException If an error occurs with the database connection.
     */
    public boolean updateQty( int idToChange, int newQty) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(getUpdateQuery(COLUMN_QTY));
        statement.setInt(1, newQty);
        statement.setInt(2, idToChange);
        return statement.executeUpdate() > 0;
    }

    /**
     * Updates a record in the books table of the Book object by changing the
     * quantity.
     *
     * @param bookToChange The Book object of the record to update.
     * @param newQty   The new book quantity.
     *
     * @return {@code true} if the database was modified
     * @throws SQLException If an error occurs with the database connection.
     */
    public boolean updateQty(Book bookToChange, int newQty) throws SQLException {
        return updateQty(bookToChange.id, newQty);
    }

    /**
     * Closes the connection to the database. Call this function at the end of the program
     * @throws SQLException If an error occurs with the database connection.
     */
    public void close() throws SQLException {
        connection.close();
    }
}
