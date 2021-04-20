package com.seekfirst.toprgb.sorter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements an external "merge" sort to handle files that will potentially be up to 1 billion records.
 *
 * @author David Botterill
 */
public class ExternalSorter {

  private static Logger logger = LoggerFactory.getLogger(ExternalSorter.class);

  private long chunkSize;

  public ExternalSorter(long chunkSize) {
    if (0L == chunkSize) {
      throw new IllegalArgumentException("Chunk size can not be 0!");
    }
    this.chunkSize = chunkSize;
  }

  /**
   * This method sorts the given largeInputFilename into a new file sortedFilename.
   *
   * @param largeInputFilename Name of the large file to be sorted.
   * @param sortedFilename Name of the new file to created for the sorted file.
   */
  public void sort(String largeInputFilename, String sortedFilename) {
    Objects.requireNonNull(largeInputFilename, "Input file name is required!");
    Objects.requireNonNull(sortedFilename, "Output sorted file name is required!");

    try {
      List<File> sortedChunkFiles = breakDownFile(largeInputFilename, this.chunkSize);
      externalSort(sortedChunkFiles, sortedFilename);
    } catch (Exception ex) {
      logger.error("Exception: " + ex.getLocalizedMessage(), ex);
    }

  }

  protected List<File> breakDownFile(String inputFile, long chunkSize) {
    ExecutorService executor = Executors.newCachedThreadPool();
    List<File> returnFiles = new ArrayList<>();

    /**
     * Break the large file into chunks that will be sorted internally then written to a separate file.
     */
    try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile))) {
      String urlLine = null;
      long byteCount = 0;
      long lineCount = 0L;
      List<String> urlChunk = new ArrayList<>();
      while ((urlLine = reader.readLine()) != null) {
        lineCount++;
        /**
         * While we can't get an exact byte count from a string, will assume a character takes up 2 bytes and use that
         * as an approximation.
         */
        byteCount += urlLine.length() * 2;
        if (byteCount < chunkSize) {
          /**
           * If we're less than the byte count, add the URL line to the chunk.
           */
          urlChunk.add(urlLine);
        } else {
          this.createChunk(executor, returnFiles, urlChunk);
          /**
           * Create a new chunk
           */
          urlChunk = new ArrayList<>();
          urlChunk.add(urlLine);
          /**
           * Reset byte count to the line just added.
           */
          byteCount = urlLine.length() * 2;
        }
      }
      /**
       * Make sure we write the last chunk.
       */
      if (null == urlLine && byteCount < chunkSize) {
        this.createChunk(executor, returnFiles, urlChunk);
      }
      logger.debug("Lines read from large file: " + lineCount);
    } catch (IOException ex) {
      logger.error("IOException: " + ex.getLocalizedMessage(), ex);
    }
    /**
     * We'll shutdown the executor so we can go into a wait state waiting for all the worker threads to finish.
     */

    try {
      logger.info("Shutting down sorter executor, waiting for threads to finish...");
      executor.shutdown();

      if (executor.awaitTermination(1L, TimeUnit.DAYS)) {
        logger.info("Sorter Executor is terminated: " + executor.isTerminated());
        logger.info("Sorter Executor terminated gracefully.");

      }
    } catch (InterruptedException ex) {
      logger.error("InterruptedException: " + ex.getLocalizedMessage(), ex);

      System.err.println("Executor interupted!");
    }

    return returnFiles;
  }

  private void createChunk(ExecutorService executor, List<File> returnFiles, List<String> urlChunk) throws IOException {
    File tempChunk = File.createTempFile("toprgb_", "_tempchunk");
    returnFiles.add(tempChunk);
    if (returnFiles.size() % 10 == 0) {
      logger.debug(returnFiles.size() + " Chunks created...");
    }
    ExternalSorterTask sorterTask = new ExternalSorterTask(urlChunk, tempChunk);
    executor.submit(sorterTask);
  }

  protected void externalSort(List<File> sortedChunkFiles, String sortedFilename) {

    if (1 == sortedChunkFiles.size()) {
      try {
        /**
         * Shortcut for the case where there is only one chunk file. We simply copy that file into the given sorted
         * file.
         */
        ReadableByteChannel readChannel = Channels.newChannel(Files.newInputStream(Paths.get(sortedChunkFiles.get(0).getAbsolutePath())));
        FileOutputStream fileOS = new FileOutputStream(new File(sortedFilename));
        FileChannel writeChannel = fileOS.getChannel();
        writeChannel.transferFrom(readChannel, 0, Long.MAX_VALUE);
      } catch (IOException ex) {
        logger.error("IOException: " + ex.getLocalizedMessage(), ex);
      }
      return;
    }

    boolean allRead = false;

    /**
     * First establish FileReader instances for each file chunk;
     */
    List<FileReader> readers = new ArrayList<>();
    logger.debug("Sorted filename: " + sortedFilename);
    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(sortedFilename), WRITE)) {

      for (File currentFile : sortedChunkFiles) {
        try {
          BufferedReader reader = Files.newBufferedReader(Paths.get(currentFile.getAbsolutePath()));
          readers.add(new FileReader(reader, currentFile.getAbsolutePath()));
          logger.debug("sorted Chunk filename: " + currentFile.getAbsolutePath());
        } catch (IOException ex) {
          logger.error("IOException: " + ex.getLocalizedMessage(), ex);
        }

      }

      /**
       * Read the first line of every file.
       */
      for (FileReader reader : readers) {
        reader.read();
      }

      String lowestString = "";
      while (!allRead) {
        /**
         * Read through all the file chunks getting the next lowest which will be the next file chunk that is NOT at
         * EOF.
         */
        for (FileReader reader : readers) {
          if (!reader.getEOF()) {
            lowestString = reader.getCurrentLine();
            break;
          }
        }
        /**
         * Read through each file comparing each row.
         */
        for (FileReader reader : readers) {
          if (!reader.getEOF()) {
            allRead = false;
            String currentLine = reader.getCurrentLine();
            if (currentLine.compareTo(lowestString) <= 0) {
              writer.write(lowestString);
              writer.newLine();
              lowestString = currentLine;
              reader.read();
            }
          } else {
            allRead = true;
          }

        }
      }

      /**
       * Close the readers.
       */
      for (FileReader reader : readers) {
        reader.getReader().close();
      }

    } catch (IOException ex) {
      logger.error("IOException: " + ex.getLocalizedMessage(), ex);
    }
  }

}
