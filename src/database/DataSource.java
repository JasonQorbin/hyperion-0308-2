package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class DataSource {
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
     * @param newCredentials The DatabaseCredentials object to apply.
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


    //Person table paramaters
    //======================
    private static final String BOOK_TABLE_NAME = "books";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "Title";
    public static final int COLUMN_TITLE_SIZE = 80;
    public static final String COLUMN_AUTHOR = "Author";
    public static final int COLUMN_AUTHOR_SIZE = 80;
    public static final String COLUMN_QTY = "Qty";

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
     * Checks to see if the Books table exists. This method assumes the database already exists.
     * A SQL Exception will be thrown if the database doesn't exist yet.
     *
     * @return {@code true} if the database table exists.
     * @throws SQLException If an error occurs with the database connection or the database doesn't exist yet.
     */
    public boolean bookTableExists() throws SQLException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM information_schema.tables WHERE table_schema = '")
                .append(credentials.getDatabase()).append("' AND table_name = '")
                .append(BOOK_TABLE_NAME).append("';");
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(queryBuilder.toString());
        boolean success = result.next();
        statement.close();
        return success;
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

    //Initial Data
    private final String STARTING_TITLES [] = {
        "A Tale of Two Cities",
        "Harry Potter and the Philosophers Stone",
        "The Lion, the With & the Wardrobe",
        "The Lord of the Rings",
        "Alice in Wonderland",
        "Queen of Shadows",
        "Iron Flame",
        "Throne of Glass",
        "Heir of Fire",
        "The Lion: Son of the Forest",
        "Courage and Honour",
        "The Queen of Nothing",
        "The Wicked King",
        "Everless",
        "Evermore",
        "The Cruel Prince",
        "Empire of Storms",
        "Harry Potter and the Prisoner of Azkaban",
        "Harry Potter and the Chamber of Secrets"
    };

    private final String STARTING_AUTHORS [] = {
        "Charles Dickens",
        "J.K. Rowling",
        "C.S. Lewis",
        "J.R.R Tolkien",
        "Lewis Carrol",
        "Sarah J Maas",
        "Rebecca Yarros",
        "Sarah J Maas",
        "Sarah J Maas",
        "Mike Brooks",
        "Graham McNeill",
        "Holly Black",
        "Holly Black",
        "Sara Holland",
        "Sara Holland",
        "Holly Black",
        "Sarah J Maas",
        "J.K. Rowling",
        "J.K. Rowling"
    };

    private final int STARTING_QTY [] = {
        30,
        40,
        25,
        37,
        12,
        154,
        122,
        87,
        164,
        11,
        11,
        39,
        29,
        56,
        57,
        30,
        734,
        41,
        61
    };

    /**
     * Inserts the initial data into the books table.
     *
     * The insertion query is built here again so that a PreparedStatement can be used in a batch execution.
     *
     * @throws SQLException If an error occurs with the database connection.
     */
    public void insertInitialData() throws SQLException {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ").append(BOOK_TABLE_NAME).append(" (")
                .append(COLUMN_TITLE).append(", ")
                .append(COLUMN_AUTHOR).append(", ")
                .append(COLUMN_QTY)
                .append(") VALUES ( ?, ?, ? );");

        PreparedStatement statement = connection.prepareStatement(query.toString());
        for (int index = 0; index < STARTING_TITLES.length; ++index) {
            statement.setString(1,STARTING_TITLES[index]);
            statement.setString(2,STARTING_AUTHORS[index]);
            statement.setInt(3,STARTING_QTY[index]);
            statement.addBatch();
            statement.clearParameters();
        }
        statement.executeBatch();
        statement.close();
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
