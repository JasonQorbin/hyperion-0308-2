# Capstone project - Project database CRUD App

> This project was a bootcamp assignment (capstone project) hence the arbitrary setup and background but is 
posted here for my own future reference and the educational benefit of others. I don't claim this to represent the
standard of how database apps should be written. This was just an exercise done on a short time limit.

## Summary

This project is an exercise in creating a terminal based, database CRUD app in Java. The app expects to have access
to a MySQL or MariaDB database where it will store the data and retrieve it during execution.

**Brief:**
>You have been approached to create a project management system by a small structural engineering firm. Write a
>terminal based program to help them track their active project, the stakeholders involved and planned and actual
>budget amounts.

## How to use the program

The program can be run from class files or packaged in a jar file.

Make sure of the the following:

- The jar files in the `lib` folder must be included in the classpath as these are the database drivers.
  For convenience there are scripts provided to build and run the program. If in doubt, just use those.
- The main class and entry point to the program is `ProgramPMS` in the `MainProgram` package.
- When you initiate the program, it will ask you for database connection details. Make sure you you provide
  a user that has privileges to create and modify the chosen database.
- On the first run the program will create the database and required structure if it doesn't exist yet.
