package com.computablefacts.asterix.nlp;

import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.asterix.ml.ConfusionMatrix;
import com.computablefacts.junon.Fact;
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

    String path = java.nio.file.Files.createTempDirectory("test-").toFile().getPath();
    File file = new File(path + File.separator + "gls.jsonl.gz");

    Assert.assertEquals(0, file.length());

    List<GoldLabel> gls = goldLabels();
    GoldLabel.save(file, View.of(gls));

    Assert.assertTrue(file.length() > 0);
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

    Assert.assertEquals(0, file.length());

    List<GoldLabel> gls = goldLabels();
    GoldLabel.save(file, View.of(gls));

    Assert.assertTrue(file.length() > 0);

    List<GoldLabel> allGls = GoldLabel.load(file).toList();

    Assert.assertEquals(gls, allGls);

    List<GoldLabel> test1Gls = GoldLabel.load(file, "test1").toList();

    Assert.assertEquals(test1Gls, gls.stream().filter(gl -> "test1".equals(gl.label())).collect(Collectors.toList()));

    List<GoldLabel> test2Gls = GoldLabel.load(file, "test2").toList();

    Assert.assertEquals(test2Gls, gls.stream().filter(gl -> "test2".equals(gl.label())).collect(Collectors.toList()));
  }

  @Test
  public void testFromFact() throws Exception {

    String json = "{\n" + "  \"id\": 706396,\n" + "  \"type\": \"evenements_garantis_degats_eaux_limites_v2\",\n"
        + "  \"values\": [\n" + "    \"3032-0006\",\n" + "    \"35594Z\",\n" + "    \"3\"\n" + "  ],\n"
        + "  \"is_valid\": false,\n" + "  \"confidence_score\": 0.71073985303466,\n"
        + "  \"external_id\": \"PizD9|2021-11-22T08:41:24.096Z\",\n" + "  \"start_date\": \"2021-11-22T08:41:24Z\",\n"
        + "  \"end_date\": \"2021-11-22T08:41:40Z\",\n" + "  \"metadata\": [\n" + "    {\n" + "      \"id\": 192517,\n"
        + "      \"type\": \"ExtractionTool\",\n" + "      \"key\": \"extracted_by_user_name\",\n"
        + "      \"value\": \"John DOE\"\n" + "    },\n" + "    {\n" + "      \"id\": 192518,\n"
        + "      \"type\": \"ExtractionTool\",\n" + "      \"key\": \"extracted_by_user_email\",\n"
        + "      \"value\": \"jdoe@example.com\"\n" + "    },\n" + "    {\n" + "      \"id\": 193366,\n"
        + "      \"type\": \"Comment\",\n" + "      \"key\": \"extracted_with\",\n" + "      \"value\": \"morta\"\n"
        + "    },\n" + "    {\n" + "      \"id\": 193367,\n" + "      \"type\": \"Comment\",\n"
        + "      \"key\": \"extracted_by\",\n" + "      \"value\": \"CoreApi\"\n" + "    },\n" + "    {\n"
        + "      \"id\": 212231,\n" + "      \"type\": \"Comment\",\n" + "      \"key\": \"extraction_date\",\n"
        + "      \"value\": \"2021-11-22T08:41:24.096Z\"\n" + "    }\n" + "  ],\n" + "  \"provenances\": [\n"
        + "    {\n" + "      \"source_store\": \"ACCUMULO/client_prod/dab/gcqcl|2021-01-27T23:23:45.006Z\",\n"
        + "      \"source_type\": \"STORAGE/ROOT/DATASET/DOC_ID\",\n" + "      \"string_span\": \"charges ﬁscales\",\n"
        + "      \"string_span_hash\": \"46df8445df964303047b8b5089e498d9\",\n" + "      \"start_index\": 0,\n"
        + "      \"end_index\": 15,\n" + "      \"source\": {\n" + "        \"storage\": \"ACCUMULO\",\n"
        + "        \"root\": \"client_prod\",\n" + "        \"dataset\": \"dab\",\n"
        + "        \"doc_id\": \"gcqcl|2021-01-27T23:23:45.006Z\",\n" + "        \"resource\": \"resource\",\n"
        + "        \"resource_link\": \"https://www.client.computablefacts.com/search/document/gcqcl|2021-01-27T23:23:45.006Z\"\n"
        + "      }\n" + "    }\n" + "  ],\n" + "  \"updated_at\": \"22-11-2021\",\n" + "  \"value_0\": \"3032-0006\"\n"
        + "}\n";

    Map<String, Object> object = JsonCodec.asObject(json);
    ((List<Map<String, Object>>) object.get("provenances")).get(0).put("string", "Mes charges ﬁscales.");

    Fact fact = Fact.fromLegacy(object);
    GoldLabel goldLabel = GoldLabel.fromFact(fact);

    Assert.assertNotNull(goldLabel);
    Assert.assertEquals("evenements_garantis_degats_eaux_limites_v2", goldLabel.label());
    Assert.assertEquals("Mes charges ﬁscales.", goldLabel.data());
    Assert.assertFalse(goldLabel.isTruePositive());
    Assert.assertTrue(goldLabel.isTrueNegative());
    Assert.assertFalse(goldLabel.isFalsePositive());
    Assert.assertFalse(goldLabel.isFalseNegative());
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
