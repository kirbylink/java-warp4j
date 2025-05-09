package de.dddns.kirbylink.warp4j.utilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class JdepAnalyzerTest {

    private JdepAnalyzer jdepAnalyzer;
    private ProcessRunner processRunner;
    private JarFile mockedJarFile;
    private static MockedStatic<FileUtilities> mockedFileUtils;

    @BeforeEach
    void setUp() {
        processRunner = mock(ProcessRunner.class);
        jdepAnalyzer = new JdepAnalyzer(new ProcessExecutor(processRunner));
        mockedJarFile = mock(JarFile.class);
        mockedFileUtils = mockStatic(FileUtilities.class);
        mockedFileUtils.when(() -> FileUtilities.createJarFile(any(Path.class))).thenReturn(mockedJarFile);
    }

    @AfterEach
    void tearDown() {
        mockedFileUtils.close();
    }

    @Test
    void testAnalyzeDependencies_withMultiReleaseAndJava17() throws IOException, InterruptedException {
        testJdepsCommand("17", true, List.of(Path.of("/mock/classpath")), List.of(
            "--ignore-missing-deps",
            "--multi-release", "17",
            "--class-path", "/mock/classpath"
        ));
    }

    @Test
    void testAnalyzeDependencies_withoutMultiRelease() throws IOException, InterruptedException {
        testJdepsCommand("17", false, List.of(Path.of("/mock/classpath")), List.of(
            "--ignore-missing-deps",
            "--class-path", "/mock/classpath"
        ));
    }

    @Test
    void testAnalyzeDependencies_withOlderJavaVersion() throws IOException, InterruptedException {
        testJdepsCommand("9", false, List.of(Path.of("/mock/classpath")), List.of(
            "--class-path", "/mock/classpath"
        ));
    }

    @Test
    void testAnalyzeDependencies_withEmptyClassPath() throws IOException, InterruptedException {
        testJdepsCommand("17", true, List.of(), List.of(
            "--ignore-missing-deps",
            "--multi-release", "17"
        ));
    }

    @SuppressWarnings("unchecked")
    private void testJdepsCommand(String javaVersion, boolean isMultiRelease, List<Path> classPaths, List<String> expectedArgs)
            throws IOException, InterruptedException {

        var mockJarFilePath = Path.of("/mock/jar.jar");
        when(mockedJarFile.isMultiRelease()).thenReturn(isMultiRelease);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);

        when(processRunner.executeAndParse(captor.capture(), any()))
                .thenAnswer(invocation -> {
                    var parser = invocation.getArgument(1, Function.class);
                    return parser.apply(List.of());
                });

        jdepAnalyzer.analyzeDependencies("jdeps", classPaths, mockJarFilePath, javaVersion);

        var actualCommand = captor.getValue();
        assertThat(actualCommand).contains("jdeps", "/mock/jar.jar");
        assertThat(actualCommand).containsSequence(expectedArgs);
    }
}
