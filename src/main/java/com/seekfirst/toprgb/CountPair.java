
package com.seekfirst.toprgb;

import java.util.Objects;

/**
 * POJO for holding a hex value and it's total count.
 * 
 * @author David Botterill
 */
class CountPair {
  
  String hexColor;
  private Long count;

  public CountPair(String hexColor, Long count) {
    this.hexColor = hexColor;
    this.count = count;
  }

  public String getHexColor() {
    return hexColor;
  }

  public void setHexColor(String hexColor) {
    this.hexColor = hexColor;
  }

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 83 * hash + Objects.hashCode(this.hexColor);
    hash = 83 * hash + Objects.hashCode(this.count);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final CountPair other = (CountPair) obj;
    if (!Objects.equals(this.hexColor, other.hexColor)) {
      return false;
    }
    if (!Objects.equals(this.count, other.count)) {
      return false;
    }
    return true;
  }
  
  
}
