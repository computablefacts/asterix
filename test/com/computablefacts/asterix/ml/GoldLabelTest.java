package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.View;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;

public class GoldLabelTest {

  @Test
  public void testEqualsAndHashcode() {
    EqualsVerifier.forClass(GoldLabel.class).verify();
  }

  @Test
  public void testFillFromJsonObject() {

    Map<String, Object> json = new HashMap<>();
    json.put("id", "1");
    json.put("label", "json");
    json.put("data", "test");
    json.put("is_true_positive", true);
    json.put("is_false_positive", false);
    json.put("is_true_negative", false);
    json.put("is_false_negative", false);

    GoldLabel goldLabel = new GoldLabel(json);

    Assert.assertEquals("1", goldLabel.id());
    Assert.assertEquals("json", goldLabel.label());
    Assert.assertEquals("test", goldLabel.data());
    Assert.assertTrue(goldLabel.isTruePositive());
    Assert.assertFalse(goldLabel.isFalsePositive());
    Assert.assertFalse(goldLabel.isTrueNegative());
    Assert.assertFalse(goldLabel.isFalseNegative());
  }

  @Test
  public void testAsMap() {

    Map<String, Object> json = new HashMap<>();
    json.put("id", "1");
    json.put("label", "json");
    json.put("data", "test");
    json.put("is_true_positive", true);
    json.put("is_false_positive", false);
    json.put("is_true_negative", false);
    json.put("is_false_negative", false);

    GoldLabel goldLabel = new GoldLabel(json);

    Assert.assertEquals(json, goldLabel.asMap());
  }

  @Test
  public void testToString() {

    GoldLabel gl = goldLabels().get(0);

    Assert.assertEquals(
        "GoldLabel{id=1, label=test1, data=test1, is_true_negative=false, is_true_positive=true, is_false_negative=false, is_false_positive=false}",
        gl.toString());
  }

  @Test
  public void testSplit_75_25() {

    Map.Entry<List<GoldLabel>, List<GoldLabel>> goldLabels = GoldLabel.split(goldLabels(), false, 0.75);

    Assert.assertEquals(6, goldLabels.getKey().size());
    Assert.assertEquals(2, goldLabels.getValue().size());
  }

  @Test
  public void testProportionalSplit_75_25() {

    Map.Entry<List<GoldLabel>, List<GoldLabel>> goldLabels = GoldLabel.split(goldLabels());

    Assert.assertEquals(6, goldLabels.getKey().size());
    Assert.assertEquals(2, goldLabels.getValue().size());
  }

  @Test
  public void testConfusionMatrix() {

    ConfusionMatrix confusionMatrix = GoldLabel.confusionMatrix(
        goldLabels().stream().filter(gl -> gl.label().equals("test1")).collect(Collectors.toList()));

    Assert.assertEquals(4, confusionMatrix.nbTruePositives());
    Assert.assertEquals(0, confusionMatrix.nbTrueNegatives());
    Assert.assertEquals(0, confusionMatrix.nbFalsePositives());
    Assert.assertEquals(0, confusionMatrix.nbFalseNegatives());
  }

  @Test
  public void testCopyConstructor() {

    List<GoldLabel> gls = goldLabels().stream().map(gl -> new GoldLabel(gl)).collect(Collectors.toList());

    Assert.assertEquals(gls, goldLabels());
  }

  @Test
  public void testSavingToAnExistingFileReturnsFalse() throws Exception {

    File file = java.nio.file.Files.createTempFile("test-", ".jsonl.gz").toFile();
    List<GoldLabel> gls = goldLabels();

    Assert.assertFalse(GoldLabel.save(file, View.of(gls)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLoadingAnUnknownFileThrowsAnException() {

    File file = new File("/tmp/gls.jsonl.gz");
    View<GoldLabel> gls = GoldLabel.load(file);
  }

  @Test
  public void testSaveThenLoad() throws Exception {

    String path = java.nio.file.Files.createTempDirectory("test-").toFile().getPath();
    File file = new File(path + File.separator + "gls.jsonl.gz");
    List<GoldLabel> gls = goldLabels();

    Assert.assertTrue(GoldLabel.save(file, View.of(gls)));

    List<GoldLabel> allGls = GoldLabel.load(file).toList();

    Assert.assertEquals(gls, allGls);

    List<GoldLabel> test1Gls = GoldLabel.load(file, "test1").toList();

    Assert.assertEquals(test1Gls, gls.stream().filter(gl -> "test1".equals(gl.label())).collect(Collectors.toList()));

    List<GoldLabel> test2Gls = GoldLabel.load(file, "test2").toList();

    Assert.assertEquals(test2Gls, gls.stream().filter(gl -> "test2".equals(gl.label())).collect(Collectors.toList()));
  }

  private List<GoldLabel> goldLabels() {
    return Lists.newArrayList(new GoldLabel(Integer.toString(1, 10), "test1", "test1", false, true, false, false),
        new GoldLabel(Integer.toString(2, 10), "test1", "test2", false, true, false, false),
        new GoldLabel(Integer.toString(3, 10), "test1", "test3", false, true, false, false),
        new GoldLabel(Integer.toString(4, 10), "test1", "test4", false, true, false, false),
        new GoldLabel(Integer.toString(5, 10), "test2", "test1", false, true, false, false),
        new GoldLabel(Integer.toString(6, 10), "test2", "test2", false, true, false, false),
        new GoldLabel(Integer.toString(7, 10), "test2", "test3", false, true, false, false),
        new GoldLabel(Integer.toString(8, 10), "test2", "test4", false, true, false, false));
  }
}
