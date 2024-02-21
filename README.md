# Reproduce a bug in jdt-core when parsing a batch of compilation units

Reproduce the [eclipse-jdt/eclipse.jdt.core issues/2050](https://github.com/eclipse-jdt/eclipse.jdt.core/issues/2050)

## Goal

Parsing a batch of 3 java files, `A.java`, `B.java`, `C.java`, using ASTParser.createASTs() method with a classpath
that contains one missing class file to fully resolve `B.java` semantics.

## Expected output

The createASTs() method should callback the FileASTRequestor 3 times, one for each file.
Problems in the semantics should not prevent `B.java` to be parsed and the following files should also be parsed.

## Actual output

The createASTs() method calls the FileASTRequestor only for `A.java` and silently ignore `B.java` and `C.java` files.

## Steps to reproduce

Run:
```bash
mvn clean package
```

This will:

* prepare the binary files to reproduce the bug
  * module1/src/main/java/org/example/Module1.java -> module1/target/classes/org/example/Module1.class and Module1$Inner.class
  * module2/src/main/java/org/example/Module2.java -> module2/target/classes/org/example/Module2.class
* run `module4-parser/src/test/java/org/example/ParserTest.java` that reproduces the bug
  * check_accept_files_with_valid_semantic use a full classpath to resolve `B.java` semantics and is able to parse all 3 files
  * check_accept_files_with_missing_semantic use a classpath with a missing class file and is able to parse only `A.java`

The fact that the `check_accept_files_with_missing_semantic` test pass highlights the bug because of [this assertion](module4-parser/src/test/java/org/example/ParserTest.java#L40)
```java
containsExactlyInAnyOrder("A.java");
```
should be
```java
containsExactlyInAnyOrder("A.java", "B.java", "C.java");
```

## Workaround

Using `org.eclipse.jdt:org.eclipse.jdt.core:3.35.0` instead of `org.eclipse.jdt:org.eclipse.jdt.core:3.36.0` [here](module4-parser/pom.xml#L34) fixes the issue.
Running `mvn clean package`, the `check_accept_files_with_missing_semantic` will fail as expected.

# Investigation

During `B.java` semantic resolution, the `ProblemHandler` is called while `referenceContext` is `null` which cause a `AbortCompilation` exception line 161.
This exception may be related to [issues/1915](https://github.com/eclipse-jdt/eclipse.jdt.core/issues/1915) but it's not
because of an exception during semantic resolution that the parsing of the batch should silently stop.
Stopping in the middle of a batch should not append, or should append by throwing an exception during the `ASTParser.createASTs()` call.
