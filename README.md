# StudentTester

[Binary releases here](https://drive.google.com/open?id=0B3eDkXG2UC2tS1JRUi1Dc1dmQ28)

The result of my thesis is a prototype codenamed StudentTester that uses the Java programming language to test code.

The testing framework of choice is TestNG, a highly customisable framework that can run unit tests written in JUnit as well. For code analysis, a popular tool called Checkstyle is used.

The default unit tests are extended using custom Java annotations, which define weights, comments, output verbosity etc.

The prototype scans directories given and recursively finds all files containing Java source code, compiles them together with the unit tests, invokes both Checkstyle and TestNG and finally prints the results on the screen.

Since the prototype is primarily intended to be used in conjunction with already implemented systems in the IT Faculty of Tallinn University of Technology, it can output compatible JSON-formatted results as well.

## How to run

Separate unit tests and testable code into different folders. The unit test folder must be passed as a command line argument ```-testroot``` and testable code as ```-contentroot```. Additional command line arguments can be found by running the jar without arguments.

## Some example usage inside unit tests:

```java

@TestContextConfiguration(mode = ReportMode.VERBOSE, identifier = 12, welcomeMessage = "Hello")
public class FooTestClass {
	...
}
```

The annotation above, when applied to a test class, will produce a test report that has an introductory message, prints some extra info (such as exception types) and, if JSON output is enabled, the test will have a identifying code 12.

```java

@Gradable(description = "A simple test", weight = 4, printExceptionMessage = true)
@Test
static void FooTestMethod {
	...
}
```

The code above will attach a description and the exception message (if exists) to the unit test FooTestMethod. If additional information is required, ```printStackTrace``` may replace ```printExceptionMessage```.

## To be continued...