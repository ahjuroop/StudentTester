# StudentTester

[Binary releases here](https://drive.google.com/open?id=0B3eDkXG2UC2tS1JRUi1Dc1dmQ28) (old releases, do not use)

The result of my thesis is a prototype codenamed StudentTester that uses the Java programming language to test code.

The testing framework of choice is TestNG, a highly customisable framework that can run unit tests written in JUnit as well. For code analysis, a popular tool called Checkstyle is used.

The default unit tests are extended using custom Java annotations, which define weights, comments, output verbosity etc.

The prototype scans directories given and recursively finds all files containing Java source code, compiles them together with the unit tests, invokes both Checkstyle and TestNG and finally prints the results on the screen.

Since the prototype is primarily intended to be used in conjunction with already implemented systems in the IT Faculty of Tallinn University of Technology, it can output compatible JSON-formatted results as well.

## Requirements

- JDK 9
- newer Gradle with Java 9 support

## How to build/run

Separate unit tests and testable code into different folders. The unit test folder must be passed as a command line argument ```-testroot``` and testable code as ```-contentroot```. Additional command line arguments can be found by running the jar without arguments.

Use common Gradle commands such as ```gradle build```, ```gradle test``` and ```gradle run```. To pass arguments to main(), use a command such as ```gradle run -Pconf='-contentroot examples/normal/contentRoot -testroot examples/normal/testRoot'```. As this generates some overhead, you can also build a JAR file with all dependencies (```gradle jar```) and use it directly in your JDK 9 environment. If you get an error about a missing compiler, you might want to ensure ```JAVA_HOME``` environment variable points to JDK 9 installation folder. You might also need to launch the java executable using its absolute path.

## Some example usage inside unit tests:

```java

@TestContextConfiguration(mode = ReportMode.VERBOSE, identifier = 12, welcomeMessage = "Hello")
public class FooTestClass {
	...
}
```

The annotation above, when applied to a test class, will produce a test report that has an introductory message, prints some extra info (such as exception types) and, if JSON output is enabled, the test will have a identifying code 12.

```java

@Gradeable(description = "A simple test", weight = 4, printExceptionMessage = true)
@Test
static void FooTestMethod {
	...
}
```

The code above will attach a description and the exception message (if exists) to the unit test FooTestMethod. If additional information is required, ```printStackTrace``` may replace ```printExceptionMessage```.

There is a minimal API available for interacting with the tester during unit testing. Basic usage is as follows:

```java

public class SampleTest {

    StudentTesterAPI api = StudentTesterAPI.getInstance(getClass());

    @Test
    public void someTest() {

        // by default all classes from student code are "blacklisted", which means some actions are restricted
        Example ex = new Example();
        System.err.println(api.getClassBlacklist()); // [class Example]

        // one can add or remove policies via the API, here we are disabling access to network sockets
        api.addSecurityPolicy(StudentPolicy.DISABLE_SOCKETS);

        int answer = ex.someMethod();
        if (answer < 20 || answer > 30) {
            api.logMessagePublic("The answer is almost correct, keep trying!"); // is visible in the report
        } else if (answer == 25) {
            api.logMessagePrivate("A new Einstein must have been born, but don't tell them."); // is visible only in JSON
            api.removeClassFromBlacklist(Example.class); // Example class can now do anything
        }
    }
}

}
```

Refer to the source code for more documentation.

## To be continued...