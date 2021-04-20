
package com.seekfirst.toprgb;

/**
 * This POJO encapsulates the TopRgb configuration.
 * @author David Botterill
 */
public class Configuration {
  private int threads;
  private String inputFilename;
  private String outputFilename;
  private long chunkSize;

  public Configuration() {
  }
  
  public int getThreads() {
    return threads;
  }

  public void setThreads(int threads) {
    this.threads = threads;
  }

  public String getInputFilename() {
    return inputFilename;
  }

  public void setInputFilename(String inputFilename) {
    this.inputFilename = inputFilename;
  }

  public String getOutputFilename() {
    return outputFilename;
  }

  public void setOutputFilename(String outputFilename) {
    this.outputFilename = outputFilename;
  }

  public long getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(long chunkSize) {
    this.chunkSize = chunkSize;
  }
  
}
