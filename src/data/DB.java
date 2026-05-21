package data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Central database helper.
 * Provides:
 *  - getConnection()       – returns an open JDBC connection (caller must close)
 *  - sha256(plain)         – SHA-256 hex digest (replaces BCrypt)
 *  - initializeSchema()    – creates tables and seed data if they don't exist
 */
public class DB {

    private static final String URL      = "jdbc:mysql://localhost:3306/ExamPlatformDB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    // ── Connection ────────────────────────────────────────────────────────────

    /**
     * Returns an open Connection. Caller is responsible for closing it.
     * Throws DatabaseException (unchecked) if the connection cannot be opened.
     */
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("MySQL JDBC driver not found. Make sure mysql-connector-j is on the classpath.", e);
        } catch (SQLException e) {
            throw new DatabaseException(
                "Could not connect to the database. Please make sure MySQL is running and try again.\n" + e.getMessage(), e);
        }
    }

    // ── Password hashing ──────────────────────────────────────────────────────

    /**
     * Returns the SHA-256 hex string of the given plain-text password.
     * Replaces BCrypt used in the original C# application.
     */
    public static String sha256(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // ── Schema initialisation ─────────────────────────────────────────────────

    public static void initializeSchema() {
        // First connect without a database to create it if needed
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("MySQL JDBC driver not found.", e);
        }

        String baseUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try (Connection conn = DriverManager.getConnection(baseUrl, USER, PASSWORD);
             Statement st = conn.createStatement()) {

            st.executeUpdate("CREATE DATABASE IF NOT EXISTS ExamPlatformDB");

        } catch (SQLException e) {
            throw new DatabaseException("Could not create database: " + e.getMessage(), e);
        }

        // Now connect to the database and create tables
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Roles (" +
                "  RoleID INT AUTO_INCREMENT PRIMARY KEY," +
                "  RoleName VARCHAR(50) NOT NULL" +
                ")"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Users (" +
                "  UserID INT AUTO_INCREMENT PRIMARY KEY," +
                "  FullName VARCHAR(100) NOT NULL," +
                "  Username VARCHAR(50) UNIQUE NOT NULL," +
                "  PasswordHash VARCHAR(64) NOT NULL," +
                "  RoleID INT NOT NULL," +
                "  IsActive TINYINT(1) DEFAULT 1," +
                "  MustChangePassword TINYINT(1) DEFAULT 1," +
                "  CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (RoleID) REFERENCES Roles(RoleID)" +
                ")"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Courses (" +
                "  CourseID INT AUTO_INCREMENT PRIMARY KEY," +
                "  CourseName VARCHAR(200) NOT NULL," +
                "  CourseCode VARCHAR(20) NOT NULL," +
                "  TeacherID INT NOT NULL," +
                "  CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (TeacherID) REFERENCES Users(UserID)" +
                ")"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Enrollments (" +
                "  StudentID INT NOT NULL," +
                "  CourseID INT NOT NULL," +
                "  PRIMARY KEY (StudentID, CourseID)," +
                "  FOREIGN KEY (StudentID) REFERENCES Users(UserID)," +
                "  FOREIGN KEY (CourseID) REFERENCES Courses(CourseID)" +
                ")"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Exams (" +
                "  ExamID INT AUTO_INCREMENT PRIMARY KEY," +
                "  Title VARCHAR(200) NOT NULL," +
                "  CourseID INT NOT NULL," +
                "  CreatedBy INT NOT NULL," +
                "  DurationMins INT NOT NULL," +
                "  StartDateTime DATETIME NOT NULL," +
                "  EndDateTime DATETIME NOT NULL," +
                "  Instructions TEXT," +
                "  IsPublished TINYINT(1) DEFAULT 0," +
                "  CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (CourseID) REFERENCES Courses(CourseID)," +
                "  FOREIGN KEY (CreatedBy) REFERENCES Users(UserID)" +
                ")"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Questions (" +
                "  QuestionID INT AUTO_INCREMENT PRIMARY KEY," +
                "  ExamID INT NOT NULL," +
                "  QuestionText TEXT NOT NULL," +
                "  QuestionType VARCHAR(10) NOT NULL," +
                "  Marks DECIMAL(5,2) NOT NULL," +
                "  OrderIndex INT NOT NULL," +
                "  FOREIGN KEY (ExamID) REFERENCES Exams(ExamID)" +
                ")"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Options (" +
                "  OptionID INT AUTO_INCREMENT PRIMARY KEY," +
                "  QuestionID INT NOT NULL," +
                "  OptionText TEXT NOT NULL," +
                "  IsCorrect TINYINT(1) DEFAULT 0," +
                "  FOREIGN KEY (QuestionID) REFERENCES Questions(QuestionID)" +
                ")"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS ExamSessions (" +
                "  SessionID INT AUTO_INCREMENT PRIMARY KEY," +
                "  StudentID INT NOT NULL," +
                "  ExamID INT NOT NULL," +
                "  StartTime DATETIME NOT NULL," +
                "  EndTime DATETIME," +
                "  IsSubmitted TINYINT(1) DEFAULT 0," +
                "  TotalScore DECIMAL(8,2)," +
                "  FOREIGN KEY (StudentID) REFERENCES Users(UserID)," +
                "  FOREIGN KEY (ExamID) REFERENCES Exams(ExamID)" +
                ")"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Answers (" +
                "  AnswerID INT AUTO_INCREMENT PRIMARY KEY," +
                "  SessionID INT NOT NULL," +
                "  QuestionID INT NOT NULL," +
                "  SelectedOptionID INT," +
                "  ShortAnswerText TEXT," +
                "  ManualScore DECIMAL(5,2)," +
                "  IsGraded TINYINT(1) DEFAULT 0," +
                "  FOREIGN KEY (SessionID) REFERENCES ExamSessions(SessionID)," +
                "  FOREIGN KEY (QuestionID) REFERENCES Questions(QuestionID)," +
                "  FOREIGN KEY (SelectedOptionID) REFERENCES Options(OptionID)" +
                ")"
            );

            // ── Seed roles ────────────────────────────────────────────────
            st.executeUpdate(
                "INSERT IGNORE INTO Roles (RoleID, RoleName) VALUES (1,'Admin'),(2,'Teacher'),(3,'Student')"
            );

            // ── Seed users ────────────────────────────────────────────────
            // All passwords = Pass@1234  (SHA-256 hex)
            String adminHash   = sha256("Admin@123");
            String commonHash  = sha256("Pass@1234");

            // Admin
            st.executeUpdate(
                "INSERT IGNORE INTO Users (UserID,FullName,Username,PasswordHash,RoleID,IsActive,MustChangePassword) " +
                "VALUES (1,'System Administrator','admin','" + adminHash + "',1,1,0)"
            );

            // Teachers
            st.executeUpdate(
                "INSERT IGNORE INTO Users (UserID,FullName,Username,PasswordHash,RoleID,IsActive,MustChangePassword) VALUES " +
                "(2,'Dr. Sarah Johnson','sarah','"   + commonHash + "',2,1,0)," +
                "(3,'Prof. Michael Chen','michael','"+ commonHash + "',2,1,0)," +
                "(4,'Ms. Emily Davis','emily','"     + commonHash + "',2,1,0)"
            );

            // Students
            st.executeUpdate(
                "INSERT IGNORE INTO Users (UserID,FullName,Username,PasswordHash,RoleID,IsActive,MustChangePassword) VALUES " +
                "(5,'Alice Thompson','alice','"  + commonHash + "',3,1,0)," +
                "(6,'Bob Martinez','bob','"      + commonHash + "',3,1,0)," +
                "(7,'Carol White','carol','"     + commonHash + "',3,1,0)," +
                "(8,'David Brown','david','"     + commonHash + "',3,1,0)," +
                "(9,'Eva Wilson','eva','"        + commonHash + "',3,1,0)," +
                "(10,'Frank Lee','frank','"      + commonHash + "',3,1,0)"
            );

            // ── Seed courses ──────────────────────────────────────────────
            st.executeUpdate(
                "INSERT IGNORE INTO Courses (CourseID,CourseName,CourseCode,TeacherID) VALUES " +
                "(1,'Introduction to Java Programming','CS101',2)," +
                "(2,'Database Management Systems','CS201',3)," +
                "(3,'Web Development Fundamentals','CS301',4)"
            );

            // ── Seed enrollments ──────────────────────────────────────────
            // Students 5-8 in course 1, students 5,7,9,10 in course 2, students 6,8,9,10 in course 3
            st.executeUpdate(
                "INSERT IGNORE INTO Enrollments (StudentID,CourseID) VALUES " +
                "(5,1),(6,1),(7,1),(8,1)," +
                "(5,2),(7,2),(9,2),(10,2)," +
                "(6,3),(8,3),(9,3),(10,3)"
            );

            // ── Seed exams ────────────────────────────────────────────────
            st.executeUpdate(
                "INSERT IGNORE INTO Exams (ExamID,Title,CourseID,CreatedBy,DurationMins," +
                "StartDateTime,EndDateTime,Instructions,IsPublished) VALUES " +
                "(1,'Java Basics Mid-Term',1,2,60," +
                " '2026-01-01 08:00:00','2027-12-31 23:59:59'," +
                " 'Answer all questions. No external resources allowed.',1)," +
                "(2,'SQL Fundamentals Quiz',2,3,45," +
                " '2026-01-01 08:00:00','2027-12-31 23:59:59'," +
                " 'Read each question carefully before answering.',1)"
            );

            // ── Seed questions — Exam 1 (Java Basics) ────────────────────
            st.executeUpdate(
                "INSERT IGNORE INTO Questions (QuestionID,ExamID,QuestionText,QuestionType,Marks,OrderIndex) VALUES " +
                "(1,1,'What is the correct way to declare a variable in Java?','MCQ',2,1)," +
                "(2,1,'Which keyword is used to create a class in Java?','MCQ',2,2)," +
                "(3,1,'Java is a platform-independent language.','TF',1,3)," +
                "(4,1,'The JVM stands for Java Virtual Machine.','TF',1,4)," +
                "(5,1,'What does OOP stand for in Java?','MCQ',2,5)," +
                "(6,1,'Which method is the entry point of a Java program?','MCQ',2,6)," +
                "(7,1,'In Java, arrays are objects.','TF',1,7)," +
                "(8,1,'Briefly explain the concept of inheritance in Java.','SHORT',3,8)"
            );

            // Options for Q1 — correct: int x = 5;
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(1,1,'int x = 5;',1)," +
                "(2,1,'variable int x = 5;',0)," +
                "(3,1,'x := 5;',0)," +
                "(4,1,'declare x = 5;',0)"
            );
            // Options for Q2 — correct: class
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(5,2,'class',1)," +
                "(6,2,'struct',0)," +
                "(7,2,'object',0)," +
                "(8,2,'define',0)"
            );
            // Options for Q3 (TF) — correct: True
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(9,3,'True',1)," +
                "(10,3,'False',0)"
            );
            // Options for Q4 (TF) — correct: True
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(11,4,'True',1)," +
                "(12,4,'False',0)"
            );
            // Options for Q5 — correct: Object-Oriented Programming
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(13,5,'Object-Oriented Programming',1)," +
                "(14,5,'Object-Oriented Processing',0)," +
                "(15,5,'Open-Oriented Programming',0)," +
                "(16,5,'Ordered Object Programming',0)"
            );
            // Options for Q6 — correct: public static void main(String[] args)
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(17,6,'public static void main(String[] args)',1)," +
                "(18,6,'public void start()',0)," +
                "(19,6,'static void run()',0)," +
                "(20,6,'void main()',0)"
            );
            // Options for Q7 (TF) — correct: True
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(21,7,'True',1)," +
                "(22,7,'False',0)"
            );

            // ── Seed questions — Exam 2 (SQL Fundamentals) ───────────────
            st.executeUpdate(
                "INSERT IGNORE INTO Questions (QuestionID,ExamID,QuestionText,QuestionType,Marks,OrderIndex) VALUES " +
                "(9,2,'Which SQL command is used to retrieve data from a database?','MCQ',2,1)," +
                "(10,2,'What does SQL stand for?','MCQ',2,2)," +
                "(11,2,'A PRIMARY KEY can contain NULL values.','TF',1,3)," +
                "(12,2,'The WHERE clause is used to filter records.','TF',1,4)," +
                "(13,2,'Which SQL clause is used to sort results?','MCQ',2,5)," +
                "(14,2,'Explain the difference between DELETE and TRUNCATE in SQL.','SHORT',3,6)"
            );

            // Options for Q9 — correct: SELECT
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(23,9,'SELECT',1)," +
                "(24,9,'GET',0)," +
                "(25,9,'FETCH',0)," +
                "(26,9,'RETRIEVE',0)"
            );
            // Options for Q10 — correct: Structured Query Language
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(27,10,'Structured Query Language',1)," +
                "(28,10,'Simple Query Language',0)," +
                "(29,10,'Standard Query Logic',0)," +
                "(30,10,'Sequential Query Language',0)"
            );
            // Options for Q11 (TF) — correct: False
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(31,11,'True',0)," +
                "(32,11,'False',1)"
            );
            // Options for Q12 (TF) — correct: True
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(33,12,'True',1)," +
                "(34,12,'False',0)"
            );
            // Options for Q13 — correct: ORDER BY
            st.executeUpdate(
                "INSERT IGNORE INTO Options (OptionID,QuestionID,OptionText,IsCorrect) VALUES " +
                "(35,13,'ORDER BY',1)," +
                "(36,13,'SORT BY',0)," +
                "(37,13,'GROUP BY',0)," +
                "(38,13,'ARRANGE BY',0)"
            );

            // ── Seed completed exam sessions (Alice and Bob took Exam 1) ──
            st.executeUpdate(
                "INSERT IGNORE INTO ExamSessions (SessionID,StudentID,ExamID,StartTime,EndTime,IsSubmitted,TotalScore) VALUES " +
                "(1,5,1,'2026-05-01 09:00:00','2026-05-01 09:55:00',1,9.00)," +
                "(2,6,1,'2026-05-01 09:05:00','2026-05-01 09:58:00',1,7.00)"
            );

            // Alice's answers (session 1) — got Q1,Q2,Q3,Q4,Q5,Q6,Q7 correct
            st.executeUpdate(
                "INSERT IGNORE INTO Answers (AnswerID,SessionID,QuestionID,SelectedOptionID,ShortAnswerText,ManualScore,IsGraded) VALUES " +
                "(1,1,1,1,NULL,2.00,1),"  + // Q1 correct
                "(2,1,2,5,NULL,2.00,1),"  + // Q2 correct
                "(3,1,3,9,NULL,1.00,1),"  + // Q3 correct (True)
                "(4,1,4,11,NULL,1.00,1)," + // Q4 correct (True)
                "(5,1,5,13,NULL,2.00,1)," + // Q5 correct
                "(6,1,6,17,NULL,2.00,1)," + // Q6 correct
                "(7,1,7,21,NULL,1.00,1)," + // Q7 correct (True)
                "(8,1,8,NULL,'Inheritance allows a class to acquire properties and methods of another class using the extends keyword.',NULL,0)" // SHORT - not graded
            );

            // Bob's answers (session 2) — got some wrong
            st.executeUpdate(
                "INSERT IGNORE INTO Answers (AnswerID,SessionID,QuestionID,SelectedOptionID,ShortAnswerText,ManualScore,IsGraded) VALUES " +
                "(9,2,1,1,NULL,2.00,1),"   + // Q1 correct
                "(10,2,2,6,NULL,0.00,1),"  + // Q2 wrong (struct)
                "(11,2,3,9,NULL,1.00,1),"  + // Q3 correct
                "(12,2,4,12,NULL,0.00,1)," + // Q4 wrong (False)
                "(13,2,5,13,NULL,2.00,1)," + // Q5 correct
                "(14,2,6,18,NULL,0.00,1)," + // Q6 wrong
                "(15,2,7,21,NULL,1.00,1)," + // Q7 correct
                "(16,2,8,NULL,'Inheritance is when one class gets the features of another class.',1.00,1)" // SHORT graded
            );

        } catch (SQLException e) {
            throw new DatabaseException("Schema initialisation failed: " + e.getMessage(), e);
        }
    }

    // ── DatabaseException ─────────────────────────────────────────────────────

    public static class DatabaseException extends RuntimeException {
        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
