# Capstone project - Project database CRUD App

## Summary

This project is an exercise in creating a terminal based, database CRUD app in Java. The app expects to have access
to a MySQL or MariaDB database where it will store the data and retrieve it during execution.

The premise is that you have been approached to create a project management system by a small structural engineering
firm. The project being tracked are therefore construction projects by nature and follow stages like "Conceptualisation",
"Bankable Feasibility", "Construction" etc.

## How to use the program

**TLDR;**

- Just use the `run.bat` or the `run.sh` scripts. 

Or...

Make sure of the following:

- Use the following classpath:
```
"./lib/mysql-connector-j-8.2.0.zip:./out/production/task2:./lib/mariadb-java-client-3.3.2.jar:./lib/mysql-connector-j-8.2.0.jar"
```
  - The jar files in the `lib` folder must be included because these are the database drivers.
  - The main class and entry point to the program is `ProgramPMS` in the `MainProgram` package. You should therefore 
    target `MainProgram.PmsProgram` when you invoke the java command.


- When you initiate the program, it will ask you for database connection details. Make sure you provide
  a user that has privileges to create and modify the chosen database.
