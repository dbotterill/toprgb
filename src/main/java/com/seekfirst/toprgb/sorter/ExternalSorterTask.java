package com.seekfirst.toprgb.sorter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.*;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class sorts a given array and writes the array out to disk.
 *
 * @author David Botterill
 */
public class ExternalSorterTask implements Runnable {

  private static Logger logger = LoggerFactory.getLogger(ExternalSorterTask.class);

  private List<String> urlChunk;
  private File outputFile;

  public ExternalSorterTask(List<String> urlChunk, File outputFile) {
    this.urlChunk = urlChunk;
    this.outputFile = outputFile;
  }

  @Override
  public void run() {

    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(this.outputFile.getAbsolutePath()), WRITE)) {
      Collections.sort(this.urlChunk);

      for(String urlString : this.urlChunk) {
        writer.write(urlString);
        writer.newLine();
      }
    } catch (IOException ex) {
      logger.error("IOException: " + ex.getLocalizedMessage(), ex);
    } catch (RuntimeException ex) {
       logger.error("RuntimeException: " + ex.getLocalizedMessage(), ex);     
    }

  }

}
