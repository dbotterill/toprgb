package com.seekfirst.toprgb;

import com.seekfirst.toprgb.sorter.ExternalSorter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service finds the 3 most prevalent colors in images given a list of URLs to images and creates a CSV (Comma
 * Separated Variable) file with each row having:
 * <p>
 * URL,color,color,color
 * <p>
 * Note - white(#ffffff) will not be considered a color.
 * <p>
 * Calling Syntax:
 * <p>
 * java -jar TopRgbService.jar -t number of threads -f input filename -o output filename (optional, default=toprgb.csv"
 *
 * @author David Botterill
 */
public class TopRgbService {

  private static Logger logger = LoggerFactory.getLogger(TopRgbService.class);

  private final static int DEFAULT_THREADS = 8;
  private final static long DEFAULT_CHUNKSIZE = 1000000000; // 1GB
  private final static String DEFAULT_OUTPUTFILENAME = "toprgb.csv";
  private final Configuration config;

  /**
   * Standard Java command line entry point.
   *
   * @param args the command line arguments
   */
  public static void main(String[] args) {

    Configuration config = parseCommands(args);
    if (null == config) {
      System.exit(1);
    }
    TopRgbService service = new TopRgbService(config);
    service.start();

  }

  public TopRgbService(Configuration config) {
    this.config = config;
  }

  public void start() {
    long start = System.currentTimeMillis();

    ExecutorService executor = Executors.newFixedThreadPool(config.getThreads());
    AtomicLong totalTime = new AtomicLong();

    /**
     * Sort the input file.
     */
    logger.debug("------------------  Starting Sort Phase -------------------");
    File sortedFile = this.createSortedFile();
    logger.debug("------------------  Ending Sort Phase -------------------");
    logger.debug("------------------  Starting Image Scan Phase -------------------");
    BufferedWriter writer = this.createBufferedWriter();

    if (null == writer) {
      return;
    }

    long urlsProcessed = 0L;
    long urlsSkipped = 0L;

    try (BufferedReader reader = Files.newBufferedReader(Paths.get(sortedFile.getAbsolutePath()))) {

      /**
       * Now read through the sorted URL file and scan the images.
       */
      String urlLine = null;
      String previousLine = "";
      while ((urlLine = reader.readLine()) != null) {
        if (urlLine.compareTo(previousLine) == 0) {
          logger.trace("Skipping analysis - already analyzed: " + urlLine);
          urlsSkipped++;
          continue;
        } else {
          previousLine = urlLine;
        }
        TopRgbTask rgbTask = new TopRgbTask(urlLine, writer);
        executor.submit(rgbTask);
        urlsProcessed++;
      }
    } catch (IOException ex) {
      logger.error("IOException: " + ex.getLocalizedMessage(), ex);
    }

    try {
      /**
       * We'll shutdown the executor so we can go into a wait state waiting for all the worker threads to finish.
       */
      logger.info("Shutting down executor, waiting for threads to finish...");
      executor.shutdown();

      if (executor.awaitTermination(1L, TimeUnit.DAYS)) {
        logger.info("Executor is terminated: " + executor.isTerminated());
        logger.info("Executor terminated gracefully.");
        writer.flush();
        writer.close();
      }
    } catch (InterruptedException ex) {
      logger.error("InterruptedException: " + ex.getLocalizedMessage(), ex);

      System.err.println("Executor interupted!");
    } catch (IOException ex) {
      java.util.logging.Logger.getLogger(TopRgbService.class.getName()).log(Level.SEVERE, null, ex);
    }
    logger.debug("------------------  Ending Image Scan Phase -------------------");

    Double timeTaken = (System.currentTimeMillis() - start) / 1000.0;
    logger.info("total URLs read: " + urlsProcessed);
    logger.info("# of repeat URLs: " + urlsSkipped);
    logger.info("Total seconds to scan images: " + timeTaken);

  }

  private BufferedWriter createBufferedWriter() {
    BufferedWriter writer = null;
    try {
      Path outputPath = Paths.get(config.getOutputFilename());
      String outputFilename = "";
      if (Files.exists(outputPath, LinkOption.NOFOLLOW_LINKS)) {
        /**
         * Append a timestamp to the output file name preserving any previous output file that could take a VERY long
         * time to produce.
         */
        outputFilename = config.getOutputFilename() + "_" + new Date().getTime();
      } else {
        outputFilename = config.getOutputFilename();
      }
      logger.info("Writing to file: " + outputFilename);

      writer = Files.newBufferedWriter(Paths.get(outputFilename), StandardOpenOption.CREATE_NEW);
    } catch (IOException ex) {
      logger.error("Error creating writer to CSV file: " + ex.getLocalizedMessage(), ex);
      return null;
    }
    return writer;
  }

  private File createSortedFile() {
    long startSortTime = System.currentTimeMillis();

    File tempSortedFile = null;
    try {
      tempSortedFile = File.createTempFile("toprgb_", "_sortedinput");
    } catch (IOException ex) {
      logger.error("IOException: " + ex.getLocalizedMessage(), ex);
    }
    ExternalSorter sorter = new ExternalSorter(config.getChunkSize());
    sorter.sort(config.getInputFilename(), tempSortedFile.getAbsolutePath());
    Double sortTimeTaken = (System.currentTimeMillis() - startSortTime) / 1000.0;
    logger.debug("Total seconds to sort: " + sortTimeTaken);

    return tempSortedFile;

  }

  private static Configuration parseCommands(String[] args) {
    Configuration config = new Configuration();
    CommandLineParser parser = new DefaultParser();
    Options options = new Options();
    Option inputFileOption = Option.builder("i")
        .required()
        .hasArg()
        .numberOfArgs(1)
        .longOpt("inputfile")
        .type(String.class)
        .desc("The input file containing URLs to image files.")
        .build();
    options.addOption(inputFileOption);

    Option threadsOption = Option.builder("t")
        .hasArg()
        .numberOfArgs(1)
        .longOpt("threads")
        .type(Integer.class)
        .desc("Number of threads to use.  Default is " + DEFAULT_THREADS)
        .build();
    options.addOption(threadsOption);

    Option outputFileOption = Option.builder("o")
        .hasArg()
        .numberOfArgs(1)
        .longOpt("outputfile")
        .type(String.class)
        .desc("The name of the the output file.  Default is " + DEFAULT_OUTPUTFILENAME)
        .build();
    options.addOption(outputFileOption);

    Option chunkSizeOption = Option.builder("cs")
        .hasArg()
        .numberOfArgs(1)
        .longOpt("chunk-size")
        .type(Long.class)
        .desc("The size of the chunks in bytes to use for external sorting.  Default is " + DEFAULT_CHUNKSIZE)
        .build();
    options.addOption(chunkSizeOption);

    CommandLine commandLine;
    try {
      commandLine = parser.parse(options, args);
    } catch (ParseException ex) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("TopRgbService", options, true);
      return null;
    }

    if (commandLine.hasOption("t")) {
      int threads = Integer.parseInt(commandLine.getOptionValue("t"));
      if (0 != threads) {
        config.setThreads(threads);
      }
    } else {
      config.setThreads(DEFAULT_THREADS);
    }

    if (commandLine.hasOption("cs")) {
      long chunkSize = Long.parseLong(commandLine.getOptionValue("cs"));
      if (0L != chunkSize) {
        config.setChunkSize(chunkSize);
      }
    } else {
      config.setChunkSize(DEFAULT_CHUNKSIZE);
    }

    if (commandLine.hasOption("o")) {
      String outputFilename = commandLine.getOptionValue("o");
      if (!outputFilename.isBlank()) {
        config.setOutputFilename(outputFilename);
      }
    } else {
      config.setOutputFilename(DEFAULT_OUTPUTFILENAME);
    }

    String inputFilename = commandLine.getOptionValue("i");
    if (null == inputFilename) {
      throw new IllegalArgumentException("Inputfile name can not be null!");
    }
    File inputFile = new File(inputFilename);
    if (!inputFile.exists()) {
      throw new IllegalArgumentException("Inputfile does not exist!");
    }
    config.setInputFilename(inputFilename);

    return config;
  }

}
