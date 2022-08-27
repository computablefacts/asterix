package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.DocumentTest;
import com.computablefacts.asterix.View;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ShufflerTest {

  @Test
  public void testShuffleTexts() {

    List<String> textsOrdered = DocumentTest.papers().map(doc -> (String) doc.text()).toList();

    Shuffler<String> shuffler = new Shuffler<>();
    List<String> textsShuffled = DocumentTest.papers().map(doc -> (String) doc.text()).partition(100).map(shuffler)
        .flatten(View::of).toList();

    Assert.assertEquals(textsOrdered, textsOrdered);
    Assert.assertEquals(textsShuffled, textsShuffled);
    Assert.assertNotEquals(textsShuffled, textsOrdered);
  }
}
