package com.seekfirst.toprgb.sorter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Botterill
 */
public class ExternalSorterTest {

  @Test
  public void testBreakDownFileLessThanChunkSize() throws Exception {

    ExternalSorter sorter = new ExternalSorter(1000);

    File testUnsorted = File.createTempFile("toprgb_", "_testsort");
    testUnsorted.deleteOnExit();

    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(testUnsorted.getAbsolutePath()), WRITE)) {
      writer.write("123456789");
      writer.newLine();
      writer.write("000012345");
      writer.newLine();
      writer.write("123456789");
      writer.newLine();
      writer.write("000012345");
      writer.newLine();
    }

    List<File> sorted = sorter.breakDownFile(testUnsorted.getAbsolutePath(), 10000);

    assertNotNull(sorted);
    assertEquals(1, sorted.size());

  }

  @Test
  public void testBreakDownFileGreaterThanChunkSize() throws Exception {

    ExternalSorter sorter = new ExternalSorter(50);

    File testUnsorted = File.createTempFile("toprgb_", "_testsort");
    testUnsorted.deleteOnExit();

    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(testUnsorted.getAbsolutePath()), WRITE)) {
      writer.write("123456789");
      writer.newLine();
      writer.write("000012345");
      writer.newLine();
      writer.write("123456789");
      writer.newLine();
      writer.write("000012345");
      writer.newLine();
      writer.write("999999999");
      writer.newLine();
    }

    List<File> sorted = sorter.breakDownFile(testUnsorted.getAbsolutePath(), 50);

    assertNotNull(sorted);
    assertEquals(3, sorted.size());

    List<String> sortedList1 = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(Paths.get(sorted.get(0).getAbsolutePath()))) {
      String urlLine = null;
      while ((urlLine = reader.readLine()) != null) {
        sortedList1.add(urlLine);
      }
    }
    List<String> sortedList2 = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(Paths.get(sorted.get(1).getAbsolutePath()))) {
      String urlLine = null;
      while ((urlLine = reader.readLine()) != null) {
        sortedList2.add(urlLine);
      }
    }
    List<String> sortedList3 = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(Paths.get(sorted.get(2).getAbsolutePath()))) {
      String urlLine = null;
      while ((urlLine = reader.readLine()) != null) {
        sortedList3.add(urlLine);
      }
    }
    assertEquals(2, sortedList1.size());
    assertEquals(2, sortedList2.size());
    assertEquals(1, sortedList3.size());

  }

  @Test
  public void testExternalSortOneFile() throws Exception {

    ExternalSorter sorter = new ExternalSorter(1000);

    File testUnsorted = File.createTempFile("toprgb_", "_testsort");
    testUnsorted.deleteOnExit();

    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(testUnsorted.getAbsolutePath()), WRITE)) {
      writer.write("123456789");
      writer.newLine();
      writer.write("000012345");
      writer.newLine();
      writer.write("123456789");
      writer.newLine();
      writer.write("000012345");
      writer.newLine();
      writer.write("999999999");
      writer.newLine();
    }
    List<File> unsorted = sorter.breakDownFile(testUnsorted.getAbsolutePath(), 1000);

    File sorted = File.createTempFile("toprgb_", "_testsort");
    sorted.deleteOnExit();

    sorter.externalSort(unsorted, sorted.getAbsolutePath());

    List<String> sortedList = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(Paths.get(sorted.getAbsolutePath()))) {
      String urlLine = null;
      while ((urlLine = reader.readLine()) != null) {
        sortedList.add(urlLine);
      }
    }

    assertEquals(5, sortedList.size());
    assertEquals("000012345", sortedList.get(0));
    assertEquals("000012345", sortedList.get(1));
    assertEquals("123456789", sortedList.get(2));
    assertEquals("123456789", sortedList.get(3));
    assertEquals("999999999", sortedList.get(4));

  }

  @Test
  public void testExternalSortManyFiles() throws Exception {

    ExternalSorter sorter = new ExternalSorter(50);

    File testUnsorted = File.createTempFile("toprgb_", "_testsort");
    testUnsorted.deleteOnExit();

    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(testUnsorted.getAbsolutePath()), WRITE)) {
      writer.write("123456789");
      writer.newLine();
      writer.write("000012345");
      writer.newLine();
      writer.write("123456789");
      writer.newLine();
      writer.write("000012345");
      writer.newLine();
      writer.write("999999999");
      writer.newLine();
    }
    List<File> unsorted = sorter.breakDownFile(testUnsorted.getAbsolutePath(), 50);

    File sorted = File.createTempFile("toprgb_", "_testsort");
    sorted.deleteOnExit();

    sorter.externalSort(unsorted, sorted.getAbsolutePath());

    List<String> sortedList = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(Paths.get(sorted.getAbsolutePath()))) {
      String urlLine = null;
      while ((urlLine = reader.readLine()) != null) {
        sortedList.add(urlLine);
      }
    }

    assertEquals(5, sortedList.size());
    assertEquals("000012345", sortedList.get(0));
    assertEquals("000012345", sortedList.get(1));
    assertEquals("123456789", sortedList.get(2));
    assertEquals("123456789", sortedList.get(3));
    assertEquals("999999999", sortedList.get(4));

  }
}
