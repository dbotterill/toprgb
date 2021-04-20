package com.seekfirst.toprgb;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author David Botterill
 */
public class TopRgbServiceTest {

  @Test
  public void testFindTopRgbOneColor() throws Exception {

    TopRgbTask task = new TopRgbTask(null, null);

    List<CountPair> topCounts = new ArrayList(3);

    URL resource = getClass().getClassLoader().getResource("white.jpg");
    BufferedImage urlImage = ImageIO.read(resource);

    task.findTopRgb(topCounts, urlImage);

    assertTrue(!topCounts.isEmpty());
    assertEquals(1, topCounts.size());
    assertEquals("#ffffff", topCounts.get(0).getHexColor());
    assertEquals(new Long(691200), topCounts.get(0).getCount());

  }

  @Test
  public void testFindTopRgbProminentFirst2Color() throws Exception {

    TopRgbTask task = new TopRgbTask(null, null);

    List<CountPair> topCounts = new ArrayList(3);
    URL resource = getClass().getClassLoader().getResource("prominate_first_2_color.jpg");
    BufferedImage urlImage = ImageIO.read(resource);
    task.findTopRgb(topCounts, urlImage);

    assertTrue(!topCounts.isEmpty());
    assertEquals(3, topCounts.size());
    assertEquals("#ffffff", topCounts.get(0).getHexColor());
    assertEquals("#000000", topCounts.get(1).getHexColor());
    assertEquals("#fefefe", topCounts.get(2).getHexColor());

  }

  @Test
  public void testFindTopRgbProminentLast4Color() throws Exception {

    TopRgbTask task = new TopRgbTask(null, null);

    List<CountPair> topCounts = new ArrayList(3);
    URL resource = getClass().getClassLoader().getResource("test1_4_colors.jpg");
    BufferedImage urlImage = ImageIO.read(resource);
    task.findTopRgb(topCounts, urlImage);

    assertTrue(!topCounts.isEmpty());
    assertEquals(3, topCounts.size());
    assertEquals("#69a84f", topCounts.get(0).getHexColor());
    assertEquals("#ffff00", topCounts.get(1).getHexColor());
    assertEquals("#fe0000", topCounts.get(2).getHexColor());

  }

  @Test
  public void testFindTopRgbProminentFirst4Color() throws Exception {

    TopRgbTask task = new TopRgbTask(null, null);

    List<CountPair> topCounts = new ArrayList(3);
    URL resource = getClass().getClassLoader().getResource("test1_4_colors_large_first.jpg");
    BufferedImage urlImage = ImageIO.read(resource);
    task.findTopRgb(topCounts, urlImage);

    assertTrue(!topCounts.isEmpty());
    assertEquals(3, topCounts.size());
    assertEquals("#69a84f", topCounts.get(0).getHexColor());
    assertEquals("#ffff00", topCounts.get(1).getHexColor());
    assertEquals("#fe0000", topCounts.get(2).getHexColor());

  }

}
