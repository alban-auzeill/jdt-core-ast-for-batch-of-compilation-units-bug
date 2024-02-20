package org.example;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParserTest {

  static final int JAVA_VERSION = 17;
  static final Path MODULE3_PACKAGE = Path.of("..", "module3", "src", "main", "java", "org", "example");
  static final List<String> FILES_TO_PARSE = List.of("A.java", "B.java", "C.java");

  @Test
  void check_accept_files_with_valid_semantic() throws IOException {
    String[] classpath = new String[] {
      Path.of("..", "module1", "target", "classes").toRealPath().toString(),
      Path.of("..", "module2", "target", "classes").toRealPath().toString()
    };
    assertThat(getParsedFileNames(classpath)).containsExactlyInAnyOrder("A.java", "B.java", "C.java");
  }

  @Test
  void check_accept_files_with_missing_semantic() throws IOException {
    String[] classpath = new String[] {
      // Intentionally remove module1 to simulate missing semantic
      Path.of("..", "module2", "target", "classes").toRealPath().toString()
    };
    assertThat(getParsedFileNames(classpath))
      .containsExactlyInAnyOrder("A.java"); // FIXME missing "B.java" and "C.java" !!!
  }

  private static List<String> getParsedFileNames(String[] classpathEntries) throws IOException {
    Path packageToParse = MODULE3_PACKAGE.toRealPath();
    String[] sourceFilePaths = FILES_TO_PARSE.stream()
      .map(packageToParse::resolve).map(Path::toString)
      .toArray(String[]::new);

    String[] encodings = new String[sourceFilePaths.length];
    Arrays.fill(encodings, "UTF-8");

    ASTParser astParser = ASTParser.newParser(JAVA_VERSION);
    Map<String, String> options = new HashMap<>();
    options.put(JavaCore.COMPILER_COMPLIANCE, String.valueOf(JAVA_VERSION));
    options.put(JavaCore.COMPILER_SOURCE, String.valueOf(JAVA_VERSION));
    astParser.setCompilerOptions(options);

    astParser.setEnvironment(classpathEntries, new String[] {}, new String[] {}, true);
    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);

    List<String> parsedFileNames = new ArrayList<>();
    FileASTRequestor callback = new FileASTRequestor() {
      @Override
      public void acceptAST(String sourceFilePath, CompilationUnit ast) {
        parsedFileNames.add(Path.of(sourceFilePath).getFileName().toString());
      }
    };
    astParser.createASTs(sourceFilePaths, encodings, new String[0], callback, null);
    return parsedFileNames;
  }

}
