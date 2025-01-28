# WEBAPP

This repository is a Webapp repository. Its designed to provide a health check API endpoint (/healthcheck) for checking the status of the database connection.


# Commands I Used to commit and push:

- git init (Even if there is an hidden files I have initializes just for best practice)

- git add .

- git add filename.extension (used to add separate files in the commit list)

- git commit -m "commit message"

- git push origin main


# Pre-requisites

Before you begin, ensure you have met the following requirements:

- Java 17 or higher
- Maven
- MySQL Database

# Setup and Installation
Clone the repository:

```
git clone <your-repository-url>
cd <your-project-directory>
```


Install dependencies:

This project uses Maven to manage dependencies. To install them, run:

```
mvn clean install
```

Configure the database:

Ensure you have a MySQL database running. Update your application.properties or application.yml file with the appropriate database connection details.

Example application.properties:
```
spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

Run the application:
```
mvn spring-boot:run
```

- mvn clean install :- Deletes the target directory. It Ensures that our code is complied fresh and is ready for execution.

- mvn spring-boot:run :- This commands starts our Spring Boot Application. It allows you to run your Spring Boot application directly from the terminal without needing to package it into a JAR or WAR file first.

 
# Check API
To check the API in the postman for Application status and database connection:

```
http://localhost:8080/healthz
```

# Login and display database using terminal: 
(Each time you login, Time stamps and Date should be uploaded)
- mysql -u your_username -p
  
- USE your_database_name;
  
- SELECT * FROM your_table_name;



# Contributions

Want to learn with my code by editing:

It's pretty straingforward :

<img align="left" alt="Java" width="30px" style="padding-right:10px;" src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/git/git-original.svg" />


```git clone <repository url>```

## BUILT WITH

* A lot of interest and respect towards Knowledge.
* An International Student @NORTHEASTERN_UNIVERSITY

## MADE BY BURRA SAI KALYAN

