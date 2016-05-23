# StudentTester

The result of my thesis is a prototype codenamed StudentTester that uses the Java programming language to test code.

The testing framework of choice is TestNG, a highly customisable framework that can run unit tests written in JUnit as well. For code analysis, a popular tool called Checkstyle is used.

The default unit tests are extended using custom Java annotations, which define weights, comments, output verbosity etc.

The prototype scans directories given and recursively finds all files containing Java source code, compiles them together with the unit tests, invokes both Checkstyle and TestNG and finally prints the results on the screen.

Since the prototype is primarily intended to be used in conjunction with already implemented systems in the IT Faculty of Tallinn University of Technology, it can output compatible JSON-formatted results as well.

For now the documentation will not be included, however, some functionality is described in the program's default output. Try running it with no arguments.