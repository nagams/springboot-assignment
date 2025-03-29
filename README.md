# Notes

Some Instructions:

* Unzip SolventumDemo.zip file
* If maven is installed on the machine, then run `mvn clean package`
* To run the app, from the project folder run the command, `java -jar target/SolventumDemo-0.0.1-SNAPSHOT.jar`
* Java source is available under `src/main`
* Test source is available under `src/test`
* Once the app is running, we can test by hitting the apis at `http://localhost:8080/one`

### Design Points

* Spring Boot project added dependencies mainly for `spring-boot-starter-web` and `spring-boot-starter-test`
* To handle concurrent access requirement, I have made use of `Semaphore` from Java `java.util.concurrent` package
* Critical piece of the code for this exercise also lies in testing the concurrent hits. For this to simulate concurrent hits, I have leveraged `CountDownLatch` `ExecutorService` 

