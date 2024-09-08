package smarticulous;

import smarticulous.db.Exercise;
import smarticulous.db.Submission;
import smarticulous.db.User;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The Smarticulous class, implementing a grading system.
 */
public class Smarticulous {

    /**
     * The connection to the underlying DB.
     * <p>
     * null if the db has not yet been opened.
     */
    Connection db;

    /**
     * Open the {@link Smarticulous} SQLite database.
     * <p>
     * This should open the database, creating a new one if necessary, and set the {@link #db} field
     * to the new connection.
     * <p>
     * The open method should make sure the database contains the following tables, creating them if necessary:
     *
     * <table>
     *   <caption><em>Table name: <strong>User</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>UserId</td><td>Integer (Primary Key)</td></tr>
     *   <tr><td>Username</td><td>Text</td></tr>
     *   <tr><td>Firstname</td><td>Text</td></tr>
     *   <tr><td>Lastname</td><td>Text</td></tr>
     *   <tr><td>Password</td><td>Text</td></tr>
     * </table>
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>Exercise</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>ExerciseId</td><td>Integer (Primary Key)</td></tr>
     *   <tr><td>Name</td><td>Text</td></tr>
     *   <tr><td>DueDate</td><td>Integer</td></tr>
     * </table>
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>Question</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>ExerciseId</td><td>Integer</td></tr>
     *   <tr><td>QuestionId</td><td>Integer</td></tr>
     *   <tr><td>Name</td><td>Text</td></tr>
     *   <tr><td>Desc</td><td>Text</td></tr>
     *   <tr><td>Points</td><td>Integer</td></tr>
     * </table>
     * In this table the combination of ExerciseId and QuestionId together comprise the primary key.
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>Submission</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>SubmissionId</td><td>Integer (Primary Key)</td></tr>
     *   <tr><td>UserId</td><td>Integer</td></tr>
     *   <tr><td>ExerciseId</td><td>Integer</td></tr>
     *   <tr><td>SubmissionTime</td><td>Integer</td></tr>
     * </table>
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>QuestionGrade</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>SubmissionId</td><td>Integer</td></tr>
     *   <tr><td>QuestionId</td><td>Integer</td></tr>
     *   <tr><td>Grade</td><td>Real</td></tr>
     * </table>
     * In this table the combination of SubmissionId and QuestionId together comprise the primary key.
     *
     * @param dburl The JDBC url of the database to open (will be of the form "jdbc:sqlite:...")
     * @return the new connection
     * @throws SQLException
     */
    public Connection openDB(String dburl) throws SQLException {
        // Connection to the database using the provided URL
        this.db = DriverManager.getConnection(dburl);
        // Create tables if they don't exist
        Statement statement = this.db.createStatement();
        //Create User table
        statement.execute("CREATE TABLE IF NOT EXISTS User (UserId INTEGER PRIMARY KEY, Username TEXT UNIQUE ,Firstname TEXT, Lastname TEXT, Password TEXT);");
        //Create Exercise table
        statement.execute("CREATE TABLE IF NOT EXISTS Exercise (ExerciseId INTEGER PRIMARY KEY, Name TEXT, DueDate INTEGER);");
        // Create Question table
        statement.execute("CREATE TABLE IF NOT EXISTS Question (ExerciseId INTEGER, QuestionId INTEGER, Name TEXT, Desc TEXT, Points INTEGER ,PRIMARY KEY(ExerciseId,QuestionId));");
        // Create Submission table
        statement.execute("CREATE TABLE IF NOT EXISTS Submission (SubmissionId INTEGER PRIMARY KEY, UserId INTEGER, ExerciseId INTEGER, SubmissionTime INTEGER);");
        // Create QuestionGrade table
        statement.execute("CREATE TABLE IF NOT EXISTS QuestionGrade (SubmissionId INTEGER, QuestionId INTEGER, Grade Real, PRIMARY KEY (SubmissionId, QuestionId));");

        return this.db;
}
    /**
     * Close the DB if it is open.
     *
     * @throws SQLException
     */
    public void closeDB() throws SQLException {
        if (db != null) {
            db.close();
            db = null;
        }
    }

    // =========== User Management =============

    /**
     * Add a user to the database / modify an existing user.
     * <p>
     * Add the user to the database if they don't exist. If a user with user.username does exist,
     * update their password and firstname/lastname in the database.
     *
     * @param user
     * @param password
     * @return the userid.
     * @throws SQLException
     */
    public int addOrUpdateUser(User user, String password) throws SQLException {
        // SELECT query to check if the user already exists
        String sqlQueryCheck = "SELECT UserId FROM User WHERE Username = ?";
        PreparedStatement psCheck = db.prepareStatement(sqlQueryCheck);
        psCheck.setString(1, user.username);
        ResultSet rs = psCheck.executeQuery();

        if(rs.next()){
            // If the user already exists in the database, update
            int userId = rs.getInt(1);
            String sqlQueryUpdate = "Update User SET Firstname = ?, Lastname = ? , Password = ? ";
            PreparedStatement psUpdate = db.prepareStatement(sqlQueryUpdate);
            // Set the password and firstname/lastname in the database
            psUpdate.setString(1, user.firstname);
            psUpdate.setString(2, user.lastname);
            psUpdate.setString(3, password);
            psUpdate.execute();
            return userId;
        }
        else{
            // If user does not exist in the database, add
            String sqlQueryAdd = "INSERT INTO User (Username, Firstname, Lastname, Password) VALUES (?,?,?,?)";
            PreparedStatement psAdd = db.prepareStatement(sqlQueryAdd, Statement.RETURN_GENERATED_KEYS);
            // Set the user name, firstname, lastname and password in the database
            psAdd.setString(1, user.username);
            psAdd.setString(2, user.firstname);
            psAdd.setString(3, user.lastname);
            psAdd.setString(4, password);
            psAdd.execute();
            ResultSet generatedKey = psAdd.getGeneratedKeys();
            return generatedKey.getInt(1);
        }
    }


    /**
     * Verify a user's login credentials.
     *
     * @param username
     * @param password
     * @return true if the user exists in the database and the password matches; false otherwise.
     * @throws SQLException
     * <p>
     * Note: this is totally insecure. For real-life password checking, it's important to store only
     * a password hash
     * @see <a href="https://crackstation.net/hashing-security.htm">How to Hash Passwords Properly</a>
     */
    public boolean verifyLogin(String username, String password) throws SQLException {
        // SELECT query to check if the user already exists in the database
        String sqlQueryName = "SELECT UserId FROM User WHERE Username = ?";
        PreparedStatement psName = db.prepareStatement(sqlQueryName);
        psName.setString(1, username);
        ResultSet rs = psName.executeQuery();

        if(rs.next()){
            // SELECT query to check if the password matches
            String sqlQueryPass = "SELECT UserId FROM User WHERE Password = ?";
            PreparedStatement psPass = db.prepareStatement(sqlQueryPass);
            psPass.setString(1, password);
            ResultSet rsPass = psPass.executeQuery();
            // If the user exist and the password matches
            if(rsPass.next()){
                return true;
            }
            // If the user exist and the password matches
            else{
                return false;
            }
        }
        // If the user not exist return false
        else {
            return false;
        }
    }

    // =========== Exercise Management =============

    /**
     * Add an exercise to the database.
     *
     * @param exercise
     * @return the new exercise id, or -1 if an exercise with this id already existed in the database.
     * @throws SQLException
     */
    public int addExercise(Exercise exercise) throws SQLException {
        // SELECT query to check if the exercise already exists in the database
        String sqlQueryCheck = "SELECT ExerciseId FROM Exercise WHERE ExerciseId = ?";
        PreparedStatement psCheck = db.prepareStatement(sqlQueryCheck);
        psCheck.setInt(1, exercise.id);
        ResultSet rs = psCheck.executeQuery();
        // If the exercise already exists, return -1
        if(rs.next()){
            return -1;
        }
        // If the exercise doesn't exist, insert it into the database
        else{
            String sqlQueryAdd = "INSERT INTO EXERCISE (ExerciseId, Name, DueDate) VALUES (?,?,?)";
            PreparedStatement psAdd = db.prepareStatement(sqlQueryAdd, Statement.RETURN_GENERATED_KEYS);
            psAdd.setInt(1, exercise.id);
            psAdd.setString(2,exercise.name);
            java.sql.Date sqlDueDate = new java.sql.Date(exercise.dueDate.getTime());
            psAdd.setDate(3, sqlDueDate);
            psAdd.execute();

            // Insert the questions of the current exercise into the database
            for(Exercise.Question q :exercise.questions) {
                addQuestion(q,exercise.id);
            }
            ResultSet generatedKey = psAdd.getGeneratedKeys();
            return generatedKey.getInt(1);
        }
    }

    // Helper function that add question of some exercise to the Question table
    public void addQuestion(Exercise.Question q, int exerciseId) throws SQLException {
        String sqlQueryAdd = "INSERT INTO Question (ExerciseId, Name, Desc, Points) VALUES (?,?,?,?)";
        PreparedStatement psAdd = db.prepareStatement(sqlQueryAdd);
        psAdd.setInt(1,exerciseId);
        psAdd.setString(2,q.name);
        psAdd.setString(3,q.desc);
        psAdd.setInt(4,q.points);
        psAdd.execute();
    }

    /**
     * Return a list of all the exercises in the database.
     * <p>
     * The list should be sorted by exercise id.
     *
     * @return list of all exercises.
     * @throws SQLException
     */
    public List<Exercise> loadExercises() throws SQLException {
        // Initialize an empty list to store Exercise objects
        List <Exercise> resList = new ArrayList<>();
        // SELECT query to retrieve all exercises from the database, ordered by ExerciseId
        String sqlQueryRes = "SELECT * FROM Exercise ORDER BY ExerciseId";
        Statement statement = this.db.createStatement();
        ResultSet rsEx  = statement.executeQuery(sqlQueryRes);
        // Iterate through the result set of exercises
        while(rsEx.next()){
            // Retrieve exercise details from the result set
            int idEx = rsEx.getInt(1);
            String nameEx = rsEx.getString(2);
            Date dueDateEx = rsEx.getDate(3);
            // Create a new Exercise object with the retrieved details
            Exercise ex = new Exercise(idEx,nameEx,dueDateEx);

            //SELECT query to retrieve questions of the current exercise
            String sqlQueryQ = "SELECT * FROM Question WHERE ExerciseId = ?";
            PreparedStatement ps = db.prepareStatement(sqlQueryQ);
            ps.setInt(1, ex.id);
            ResultSet rsQ = ps.executeQuery();
            // Iterate through the result set of questions
            while (rsQ.next()){
                // Retrieve question details from the result set
                String nameQ = rsQ.getString(3);
                String descQ = rsQ.getString(4);
                int pointsQ = rsQ.getInt(5);
                // Add the question to the current exercise
                ex.addQuestion(nameQ,descQ,pointsQ);
            }
            // Add the Exercise to the resList
            resList.add(ex);
        }
        return resList;
    }

    // ========== Submission Storage ===============

    /**
     * Store a submission in the database.
     * The id field of the submission will be ignored if it is -1.
     * <p>
     * Return -1 if the corresponding user doesn't exist in the database.
     *
     * @param submission
     * @return the submission id.
     * @throws SQLException
     */
    public int storeSubmission(Submission submission) throws SQLException {
        // SELECT query to check if the user already exists in the database
        String sqlQueryCheck = "SELECT UserId FROM User WHERE Username = ?";
        PreparedStatement psCheck = db.prepareStatement(sqlQueryCheck);
        psCheck.setString(1, submission.user.username);
        ResultSet rsCheck = psCheck.executeQuery();

        // If the user exists in the database
        if (rsCheck.next()) {
            // Get the user id from the result set
            int userId = rsCheck.getInt(1);
            PreparedStatement psStore;

            // If the submission has a valid id
            if (submission.id != -1) {
                // SQL query to insert submission without specifying the submission id
                String sqlQueryStore = "INSERT INTO Submission (UserId, ExerciseId, SubmissionTime) VALUES (?,?,?)";
                psStore = db.prepareStatement(sqlQueryStore, Statement.RETURN_GENERATED_KEYS);
                psStore.setInt(1, userId);
                psStore.setInt(2, submission.exercise.id);
                java.sql.Date sqlTime = new java.sql.Date(submission.submissionTime.getTime());
                psStore.setDate(3, sqlTime);
            } else {
                // SQL query to insert submission with specified submission id
                String sqlQueryStore = "INSERT INTO Submission (SubmissionId, UserId, ExerciseId, SubmissionTime) VALUES (?,?,?,?)";
                psStore = db.prepareStatement(sqlQueryStore);
                psStore.setInt(1, submission.id);
                psStore.setInt(2, userId);
                psStore.setInt(3, submission.exercise.id);
                java.sql.Date sqlTime = new java.sql.Date(submission.submissionTime.getTime());
                psStore.setDate(4, sqlTime);
            }
            // Execute the SQL statement to insert the submission
            psStore.execute();

            ResultSet generatedKeys = psStore.getGeneratedKeys();
            return generatedKeys.getInt(1);
        } else {
            return -1;
        }
    }

        public void addGrade(Submission submission) throws SQLException {
            for (int i = 1; i <= submission.exercise.questions.size(); i++) {
                String sqlQueryAdd = "INSERT INTO QuestionGrade (SubmissionId, QuestionId, Grade) VALUES (?,?,?)";
                PreparedStatement psAdd = db.prepareStatement(sqlQueryAdd);
                psAdd.setInt(1, submission.id);
                psAdd.setInt(2, i);
                float grade = submission.questionGrades[i] / submission.exercise.questions.get(i).points;
                psAdd.setFloat(3, grade);
                psAdd.execute();

            }
        }

    // ============= Submission Query ===============


    /**
     * Return a prepared SQL statement that, when executed, will
     * return one row for every question of the latest submission for the given exercise by the given user.
     * <p>
     * The rows should be sorted by QuestionId, and each row should contain:
     * - A column named "SubmissionId" with the submission id.
     * - A column named "QuestionId" with the question id,
     * - A column named "Grade" with the grade for that question.
     * - A column named "SubmissionTime" with the time of submission.
     * <p>
     * Parameter 1 of the prepared statement will be set to the User's username, Parameter 2 to the Exercise Id, and
     * Parameter 3 to the number of questions in the given exercise.
     * <p>
     * This will be used by {@link #getLastSubmission(User, Exercise)}
     *
     * @return
     */
    PreparedStatement getLastSubmissionGradesStatement() throws SQLException {
        String sqlQuery =
                // Joining the tables: User, Submission, Question, and QuestionGrade
                "SELECT Submission.SubmissionId, Question.QuestionId, QuestionGrade.Grade, Submission.SubmissionTime FROM " +
                "User INNER JOIN Submission ON User.UserId = Submission.UserId " +
                "INNER JOIN Question ON Submission.ExerciseId = Question.ExerciseId " +
                "INNER JOIN QuestionGrade ON (Submission.SubmissionId = QuestionGrade.submissionId AND Question.QuestionId = QuestionGrade.QuestionId) " +
                // Filtering the data
                "WHERE User.UserName = ? AND Submission.ExerciseId = ? " +
                // Grouping and filtering by maximum submission time
                "GROUP BY QuestionGrade.QuestionId HAVING MAX (Submission.SubmissionTime) " +
                // Sorting and limiting the results
                "ORDER BY Question.QuestionId LIMIT ? ";
        PreparedStatement ps = db.prepareStatement(sqlQuery);
        return ps;
    }

    /**
     * Return a prepared SQL statement that, when executed, will
     * return one row for every question of the <i>best</i> submission for the given exercise by the given user.
     * The best submission is the one whose point total is maximal.
     * <p>
     * The rows should be sorted by QuestionId, and each row should contain:
     * - A column named "SubmissionId" with the submission id.
     * - A column named "QuestionId" with the question id,
     * - A column named "Grade" with the grade for that question.
     * - A column named "SubmissionTime" with the time of submission.
     * <p>
     * Parameter 1 of the prepared statement will be set to the User's username, Parameter 2 to the Exercise Id, and
     * Parameter 3 to the number of questions in the given exercise.
     * <p>
     * This will be used by {@link #getBestSubmission(User, Exercise)}
     *
     */
    PreparedStatement getBestSubmissionGradesStatement() throws SQLException {
        // TODO: Implement
        return null;
    }

    /**
     * Return a submission for the given exercise by the given user that satisfies
     * some condition (as defined by an SQL prepared statement).
     * <p>
     * The prepared statement should accept the user name as parameter 1, the exercise id as parameter 2 and a limit on the
     * number of rows returned as parameter 3, and return a row for each question corresponding to the submission, sorted by questionId.
     * <p>
     * Return null if the user has not submitted the exercise (or is not in the database).
     *
     * @param user
     * @param exercise
     * @param stmt
     * @return
     * @throws SQLException
     */
    Submission getSubmission(User user, Exercise exercise, PreparedStatement stmt) throws SQLException {
        stmt.setString(1, user.username);
        stmt.setInt(2, exercise.id);
        stmt.setInt(3, exercise.questions.size());

        ResultSet res = stmt.executeQuery();

        boolean hasNext = res.next();
        if (!hasNext)
            return null;

        int sid = res.getInt("SubmissionId");
        Date submissionTime = new Date(res.getLong("SubmissionTime"));

        float[] grades = new float[exercise.questions.size()];

        for (int i = 0; hasNext; ++i, hasNext = res.next()) {
            grades[i] = res.getFloat("Grade");
        }

        return new Submission(sid, user, exercise, submissionTime, (float[]) grades);
    }

    /**
     * Return the latest submission for the given exercise by the given user.
     * <p>
     * Return null if the user has not submitted the exercise (or is not in the database).
     *
     * @param user
     * @param exercise
     * @return
     * @throws SQLException
     */
    public Submission getLastSubmission(User user, Exercise exercise) throws SQLException {
        return getSubmission(user, exercise, getLastSubmissionGradesStatement());
    }


    /**
     * Return the submission with the highest total grade
     *
     * @param user the user for which we retrieve the best submission
     * @param exercise the exercise for which we retrieve the best submission
     * @return
     * @throws SQLException
     */
    public Submission getBestSubmission(User user, Exercise exercise) throws SQLException {
        return getSubmission(user, exercise, getBestSubmissionGradesStatement());
    }
}
