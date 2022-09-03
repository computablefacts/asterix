package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.DocumentTest;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.re2j.Pattern;
import java.io.File;
import java.nio.file.Files;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ModelTest {

  private static File documents_;
  private static File goldLabels_;

  @BeforeClass
  public static void beforeAll() throws Exception {

    String path = Files.createTempDirectory("").toFile().getAbsolutePath();

    // Build dataset (documents)
    documents_ = documents(path);
    Assert.assertTrue(documents_.exists());

    // Build vocabulary
    Assert.assertTrue(vocabulary1(documents_).exists());
    Assert.assertTrue(vocabulary2(documents_).exists());
    Assert.assertTrue(vocabulary3(documents_).exists());
    Assert.assertTrue(vocabulary4(documents_).exists());
    Assert.assertTrue(vocabulary5(documents_).exists());
    Assert.assertTrue(vocabulary6(documents_).exists());

    // Build dataset (gold labels)
    goldLabels_ = goldLabels(path);
    Assert.assertTrue(goldLabels_.exists());
  }

  @AfterClass
  public static void afterAll() {
    if (documents_ != null) {
      documents_.delete();
    }
    if (goldLabels_ != null) {
      goldLabels_.delete();
    }
  }

  private static File documents(String path) {
    File documents = new File(path + File.separator + "papers.jsonl.gz");
    DocumentTest.papers().toFile(doc -> JsonCodec.asString(doc.json()), documents, false, true);
    return documents;
  }

  private static File vocabulary1(File file) {
    Vocabulary.main(new String[]{file.getAbsolutePath(), "0.01", "0.99", "1000", "WORD,NUMBER,TERMINAL_MARK", "1"});
    return new File(file.getParent() + File.separator + "vocabulary-1grams.tsv.gz");
  }

  private static File vocabulary2(File file) {
    Vocabulary.main(new String[]{file.getAbsolutePath(), "0.01", "0.99", "1000", "WORD,NUMBER,TERMINAL_MARK", "2"});
    return new File(file.getParent() + File.separator + "vocabulary-2grams.tsv.gz");
  }

  private static File vocabulary3(File file) {
    Vocabulary.main(new String[]{file.getAbsolutePath(), "0.01", "0.99", "1000", "WORD,NUMBER,TERMINAL_MARK", "3"});
    return new File(file.getParent() + File.separator + "vocabulary-3grams.tsv.gz");
  }

  private static File vocabulary4(File file) {
    Vocabulary.main(new String[]{file.getAbsolutePath(), "0.01", "0.99", "1000", "WORD,NUMBER,TERMINAL_MARK", "4"});
    return new File(file.getParent() + File.separator + "vocabulary-4grams.tsv.gz");
  }

  private static File vocabulary5(File file) {
    Vocabulary.main(new String[]{file.getAbsolutePath(), "0.01", "0.99", "1000", "WORD,NUMBER,TERMINAL_MARK", "5"});
    return new File(file.getParent() + File.separator + "vocabulary-5grams.tsv.gz");
  }

  private static File vocabulary6(File file) {
    Vocabulary.main(new String[]{file.getAbsolutePath(), "0.01", "0.99", "1000", "WORD,NUMBER,TERMINAL_MARK", "6"});
    return new File(file.getParent() + File.separator + "vocabulary-6grams.tsv.gz");
  }

  private static File goldLabels(String path) {
    Pattern pattern = Pattern.compile(".*crowdsourcing.*",
        Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    File goldLabels = new File(path + File.separator + "gold-labels.jsonl.gz");
    DocumentTest.papers().map(doc -> (String) doc.text()).flatten(text -> View.of(Splitter.on('\f').split(text)))
        .filter(page -> !Strings.isNullOrEmpty(page)).index().map(page -> {
          boolean isTruePositive = pattern.matches(page.getValue());
          return new GoldLabel(page.getKey().toString(), "crowdsourcing", page.getValue(), !isTruePositive, isTruePositive,
              false, false);
        }).index().filter(
            goldLabel -> goldLabel.getValue().isTruePositive() || goldLabel.getKey() % 10 == 0 /* speed-up the test */)
        .toFile(goldLabel -> JsonCodec.asString(goldLabel.getValue().asMap()), goldLabels, false, true);
    return goldLabels;
  }

  @Test
  public void testTrainAllClassifiers() {

    // Do not test FLD because "covariance matrix (column 23) is close to singular."
    Model.main(new String[]{documents_.getAbsolutePath(), goldLabels_.getAbsolutePath(), "crowdsourcing",
        "ab,dt,dnb,gbt,knn,logit,mlp,rf,svm"});
    File file = new File(documents_.getParent() + File.separator + "ensemble-model-crowdsourcing.xml.gz");

    Assert.assertTrue(file.exists());

    Model model = Model.load(file);

    Assert.assertNotNull(model);

    ConfusionMatrix confusionMatrix = model.confusionMatrix();

    Assert.assertEquals(1.0, confusionMatrix.matthewsCorrelationCoefficient(), 0.000001);
    Assert.assertEquals(1.0, confusionMatrix.f1Score(), 0.000001);
    Assert.assertEquals(1.0, confusionMatrix.precision(), 0.000001);
    Assert.assertEquals(1.0, confusionMatrix.recall(), 0.000001);
    Assert.assertEquals(1.0, confusionMatrix.accuracy(), 0.000001);
  }
}
