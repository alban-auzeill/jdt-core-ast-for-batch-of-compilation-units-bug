package org.example;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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

  @Test
  void check_accept_files_with_valid_semantic() throws IOException {
    String[] classpathEntries = new String[] {
      Path.of("..", "module1", "target", "classes").toRealPath().toString(),
      Path.of("..", "module2", "target", "classes").toRealPath().toString()
    };
    check_accept_files(classpathEntries);
  }

  @Test
  void check_accept_files_with_missing_semantic() throws IOException {
    String[] classpathEntries = new String[] {
      Path.of("..", "module2", "target", "classes").toRealPath().toString()
    };
    check_accept_files(classpathEntries);
  }

  private static void check_accept_files(String[] classpathEntries) throws IOException {
    Path packageToParse = Path.of("..", "module3", "src", "main", "java", "org", "example").toRealPath();
    List<String> fileToParse = List.of("D.java", "E.java", "F.java");
    String[] encodings = new String[] {"UTF-8", "UTF-8", "UTF-8"};

    String[] sourceFilePaths = fileToParse.stream()
      .map(packageToParse::resolve).map(Path::toString)
      .toArray(String[]::new);

    int javaVersion = 17;
    ASTParser astParser = ASTParser.newParser(javaVersion);
    Map<String, String> options = new HashMap<>();
    options.put(JavaCore.COMPILER_COMPLIANCE, String.valueOf(javaVersion));
    options.put(JavaCore.COMPILER_SOURCE, String.valueOf(javaVersion));
    astParser.setCompilerOptions(options);

    astParser.setEnvironment(classpathEntries, new String[] {}, new String[] {}, true);
    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);

    List<String> acceptedPath = new ArrayList<>();
    FileASTRequestor callback = new FileASTRequestor() {
      @Override
      public void acceptAST(String sourceFilePath, CompilationUnit ast) {
        acceptedPath.add(Path.of(sourceFilePath).getFileName().toString());
      }
    };
    astParser.createASTs(sourceFilePaths, encodings, new String[0], callback, null);

    assertThat(acceptedPath).containsExactlyInAnyOrder("D.java", "E.java", "F.java");
  }

}
