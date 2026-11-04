package org.sitenetsoft.quarkus.seo.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class SchemaPackageIsolationTest {

    @Test
    void schemaPackageImportsOnlyTheJdk() throws IOException {
        Path dir = Path.of("src/main/java/org/sitenetsoft/quarkus/seo/schema");
        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream.filter(p -> p.toString().endsWith(".java")).toList();
        }
        assertThat(files).isNotEmpty();
        for (Path file : files) {
            for (String line : Files.readAllLines(file)) {
                if (line.startsWith("import ")) {
                    assertThat(line).as(file.toString())
                            .matches("import (static )?java\\..*");
                }
            }
        }
    }
}
