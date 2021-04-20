package com.seekfirst.toprgb;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates all the necessary steps to calculate the top 3 RGB colors for a given image.
 *
 * @author David Botterill
 */
public class TopRgbTask implements Runnable {

  private final Logger logger = LoggerFactory.getLogger(TopRgbTask.class);
  private final int TOP_COUNT = 3;
  private final String imageFileURLString;
  private final BufferedWriter writer;
  private boolean halt;
  private int writeErrors; // circuit break for write errors.
  private final int WRITE_CIRCUIT_BREAKER = 5;
  private final Map<String, Long> pixelColorCountMap = new HashMap();
  private final int MAX_URLREAD_TRIES = 3;

  public TopRgbTask(String imageFileURLString, BufferedWriter writer) {
    this.imageFileURLString = imageFileURLString;
    this.writer = writer;
  }

  @Override
  public void run() {
    long start = System.currentTimeMillis();

    try {
      URL urlInput = new URL(imageFileURLString);
      File tempImageFile = null;
      InputStream urlInputStream = null;
      if (urlInput.getProtocol().equalsIgnoreCase("http") || urlInput.getProtocol().equalsIgnoreCase("https")) {
        /**
         * We'll copy the image from the URL to a "local" filesystem before processing. The ImageIO.read() method
         * silently dies when it has problems reading across the network. If we do the network reads, we can put in a
         * retry.
         */
        HttpURLConnection urlConnection = (HttpURLConnection) urlInput.openConnection();
        urlConnection.setInstanceFollowRedirects(false);
        urlConnection.connect();
        String location = urlConnection.getHeaderField("Location");
        /**
         * Check to see if the URL is redirected.
         */
        if (null != location) {
          urlConnection = (HttpURLConnection) new URL(location).openConnection();
          logger.debug("Followed redirected URL: " + location);
        }

        urlInputStream = urlConnection.getInputStream();
        ReadableByteChannel readChannel = Channels.newChannel(urlInputStream);
        tempImageFile = File.createTempFile("toprpg_", "_temp");
        urlInput = new URL("file:///" + tempImageFile.getAbsolutePath());
        FileOutputStream fileOS = new FileOutputStream(tempImageFile);
        FileChannel writeChannel = fileOS.getChannel();
        int urlTries = 1;
        while (urlTries < MAX_URLREAD_TRIES) {
          try {
            long bytesTransfered = writeChannel.transferFrom(readChannel, 0, Long.MAX_VALUE);
            urlTries = MAX_URLREAD_TRIES;
          } catch (IOException ex) {
            urlTries++;
            if (urlTries < MAX_URLREAD_TRIES) {
              logger.error("Error reading URL: " + imageFileURLString + ". Retrying...");
            } else {
              logger.error("Error reading URL: " + imageFileURLString + ". Aboring...");
              return;
            }
          }
        }
      }

      BufferedImage urlImage = ImageIO.read(urlInput);
      /**
       * Account for horrible ImageIO API that returns null without throwing an exception if an ImageReader can not be
       * found to read the image.
       */
      if (null == urlImage) {
        logger.error("Error reading URL into image for: " + imageFileURLString + ". Skipping...");
        return;
      }
      List<CountPair> topCounts = new ArrayList(TOP_COUNT);

      findTopRgb(topCounts, urlImage);
      writeTopRgb(topCounts);
      /**
       * Clean up the temp image file if it exists.
       */
      if (null != tempImageFile) {
        tempImageFile.delete();
      }
    } catch (MalformedURLException ex) {
      logger.error("Error creating URL: " + ex.getLocalizedMessage(), ex);
    } catch (IOException ex) {
      logger.error("Error reading URL: " + ex.getLocalizedMessage(), ex);
    } catch (RuntimeException ex) {
      /**
       * We need this to report all RunTimeExceptions in case we're running in an Executor that swallows these.
       */
      logger.error("Exception: " + ex.getLocalizedMessage(), ex);
    }

    Double timeTaken = (System.currentTimeMillis() - start) / 1000.0;
    logger.trace(imageFileURLString + " - time to process image:" + timeTaken);
  }

  private void halt() {
    this.halt = true;
  }

  private void writeToCSVFile(String outputString) {
    try {
      writer.write(outputString);
    } catch (IOException ex) {
      logger.error("Error writing to file: " + ex.getLocalizedMessage(), ex);
      this.writeErrors++;
      if (this.writeErrors > WRITE_CIRCUIT_BREAKER) {
        logger.error("Halting because write errors exceeded " + WRITE_CIRCUIT_BREAKER);
        this.halt();
      }

    }
  }

  protected void findTopRgb(List<CountPair> topCounts, BufferedImage urlImage) {
    /**
     * Go through the pixels of the image keeping count in the pixel hashmap.
     */
    logger.trace(this.imageFileURLString + " - width: " + urlImage.getWidth());
    logger.trace(this.imageFileURLString + " - height: " + urlImage.getHeight());

    for (int column = 0; column < urlImage.getWidth(); column++) {
      if (halt) {
        break;
      }
      for (int row = 0; row < urlImage.getHeight(); row++) {
        if (halt) {
          break;
        }

        int pixel = urlImage.getRGB(column, row);
        Color color = new Color(pixel, true);
        /**
         * convert to hex
         */
        String buf = Integer.toHexString(color.getRGB());
        String hex = "#" + buf.substring(buf.length() - 6);

        Long currentCount = pixelColorCountMap.get(hex);
        if (null != currentCount) {
          currentCount++;
          pixelColorCountMap.put(hex, currentCount);
        } else {
          currentCount = 1L;
          pixelColorCountMap.put(hex, currentCount);

        }

        /**
         * Check the current top counts and replace one if needed.
         */
        checkChangeTopRgb(topCounts, 0, new CountPair(hex, currentCount));
      }
    }

  }

  private void checkChangeTopRgb(List<CountPair> topCounts, int currentPosition, CountPair countPair) {

    /**
     * Walk through the current list of the top rgb and insert the new one if appropriate. Note - the lowest count will
     * drop out of the top list.
     * <p>
     * The highest count will be maintained in position 0 and the lowest count will be in the last position.
     */
    if (topCounts.isEmpty() || topCounts.size() - 1 < currentPosition) {
      /**
       * This position is empty so fill it in with the passed in color.
       */
      if (currentPosition < TOP_COUNT) {
        topCounts.add(currentPosition, countPair);
      }
    } else if (topCounts.get(currentPosition).getHexColor().equals(countPair.getHexColor())) {
      /**
       * The passed in color is already in the at this position sup update the
       */
      topCounts.set(currentPosition, countPair);
    } else if (countPair.getCount() > topCounts.get(currentPosition).getCount()) {

      /**
       * Replace the current position with the passed in color and shift the replaced color to a lower position (farther
       * back in the list).
       */
      if (currentPosition < TOP_COUNT) {
        CountPair tempPair = topCounts.get(currentPosition);
        topCounts.set(currentPosition, countPair);
        /**
         * At this point, we could have the same hex value later in the array with a lower value, We need to remove that
         * element.
         */
        int foundPosition = -1;
        for (int ii = 0; ii < topCounts.size(); ii++) {
          if (topCounts.get(ii).getHexColor().equals(countPair.hexColor) && topCounts.get(ii).getCount() < countPair.getCount()) {
            foundPosition = ii;
            break;
          }
        }
        if (-1 != foundPosition) {
          topCounts.remove(foundPosition);
        }
        checkChangeTopRgb(topCounts, currentPosition + 1, tempPair);
      }
    } else if (currentPosition + 1 < TOP_COUNT) {
      checkChangeTopRgb(topCounts, currentPosition + 1, countPair);
    }
  }

  private void writeTopRgb(List<CountPair> topCounts) {
    StringBuffer topString = new StringBuffer();
    topString.append(this.imageFileURLString);
    topString.append(",");
    for (int ii = 0; ii < topCounts.size(); ii++) {
      topString.append((String) (null != topCounts.get(ii) ? topCounts.get(ii).getHexColor() : ""));
      if (ii + 1 < topCounts.size()) {
        topString.append(",");
      }
    }
    writeToCSVFile(topString.toString() + "\n");

  }

}
