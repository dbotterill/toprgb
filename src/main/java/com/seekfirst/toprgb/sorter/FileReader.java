
package com.seekfirst.toprgb.sorter;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This class encapsulates a BufferedReader for a file and the EOF state.
 * 
 * @author David Botterill
 */
public class FileReader {
  private BufferedReader reader;
  private Boolean EOF;
  private String currentLine;
  private String filename;

  public FileReader(BufferedReader reader, String filename) {
    this.reader = reader;
    this.EOF = false;
    this.filename = filename;
  }

  public BufferedReader getReader() {
    return reader;
  }

  public void setReader(BufferedReader reader) {
    this.reader = reader;
  }

  public Boolean getEOF() {
    return EOF;
  }

  public void setEOF(Boolean EOF) {
    this.EOF = EOF;
  }  
  
  /**
   * This method reads the next line and stores it in "currentLine".
   * 
   * @return currentLIne the line just read.
   * @throws IOException
   */
  public String read() throws IOException {
    this.currentLine = this.reader.readLine();
    if(null == this.currentLine) {
      this.setEOF(true);
    }
    return this.currentLine;
  }

  public String getCurrentLine() {
    return currentLine;
  }

  public String getFilename() {
    return filename;
  }
  
  
}
