package com.adaptris.pdf.test;

import com.adaptris.testing.SingleAdapterFunctionalTest;
import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.CompareResultWithMemoryOverflow;
import de.redsix.pdfcompare.PdfComparator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fop.pdf.PDFDocument;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DefaultFunctionalTest extends SingleAdapterFunctionalTest {
    PDFTextStripper stripper = new PDFTextStripper();

    static class TestCaseData {
        Path directoryIn;
        Path directoryOut;
        String input;
        String expected;

        TestCaseData(Path directoryIn, Path directoryOut, String resource, String expected) {
            this.directoryIn = directoryIn;
            this.directoryOut = directoryOut;
            this.input = resource;
            this.expected = expected;
        }
    }

    List<TestCaseData> default_test_case_data() {
        return List.of(
            new TestCaseData(Path.of("messages", "create-pdf-data-in"), Path.of("messages", "create-pdf-data-out"), "example-input.fo", "example-output.pdf"),
            new TestCaseData(Path.of("messages", "create-html-data-in"), Path.of("messages", "create-html-data-out"), "example-output.pdf", "example-output.html"),
            new TestCaseData(Path.of("messages", "create-text-data-in"), Path.of("messages", "create-text-data-out"), "example-output.pdf", "example-output.txt")
        );
    }

    @ParameterizedTest
    @MethodSource("default_test_case_data")
    void test(TestCaseData testCaseData) throws Exception {
        Path outputPath = testCaseData.directoryOut;
        outputPath.toFile().mkdir();
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Assertions.assertTrue(outputPath.toFile().exists());
            WatchKey key = outputPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            File tempInputFile = File.createTempFile("testing_pdf_input", testCaseData.input.substring(testCaseData.input.lastIndexOf('.') - 1));
            File tempExpectedFile = File.createTempFile("testing_pdf_expected", testCaseData.expected.substring(testCaseData.input.lastIndexOf('.') - 1));
            tempInputFile.deleteOnExit();
            tempExpectedFile.deleteOnExit();
            ClassLoader loader = getClass().getClassLoader();
            try (InputStream is = loader.getResourceAsStream(testCaseData.input);
                 InputStream is1 = loader.getResourceAsStream(testCaseData.expected)
            ) {
                byte[] input = is.readAllBytes();
                byte[] expected = is1.readAllBytes();

                FileUtils.writeByteArrayToFile(tempInputFile, input);
                FileUtils.writeByteArrayToFile(tempExpectedFile, expected);

                FileUtils.moveToDirectory(tempInputFile, testCaseData.directoryIn.toFile(), false);
                WatchKey key1 = watchService.poll(30, TimeUnit.SECONDS);
                Assertions.assertNotNull(key1);

                key1.pollEvents().forEach(event -> {
                    Path createdPath = (Path)event.context();
                    File outputFile = outputPath.resolve(createdPath).toFile();
                    Assertions.assertTrue(outputFile.exists());
                    outputFile.deleteOnExit();
                    Assertions.assertDoesNotThrow(() -> {
                        if (tempExpectedFile.getName().endsWith(".pdf")) {
                            PDDocument outputPdf = Loader.loadPDF(outputFile);
                            PDDocument expectedPdf = Loader.loadPDF(tempExpectedFile);
                            String outputText = stripper.getText(outputPdf);
                            String expectedText = stripper.getText(expectedPdf);
                            Assertions.assertEquals(expectedText, outputText);
                        } else if (tempExpectedFile.getName().endsWith("html")) {
                            Assertions.assertEquals(
                                StringUtils.deleteWhitespace(Jsoup.parse(tempExpectedFile).body().text()).replaceAll("\\P{Print}", ""),
                                StringUtils.deleteWhitespace(Jsoup.parse(outputFile).body().text()).replaceAll("\\P{Print}", "")
                            );
                        } else {
                            Assertions.assertEquals(
                                StringUtils.deleteWhitespace(FileUtils.readFileToString(tempExpectedFile)).replaceAll("\\P{Print}", ""),
                                StringUtils.deleteWhitespace(FileUtils.readFileToString(outputFile)).replaceAll("\\P{Print}", "")
                            );
                        }
                    });
                });
            }
        } finally {
            outputPath.toFile().deleteOnExit();
        }
    }

}
