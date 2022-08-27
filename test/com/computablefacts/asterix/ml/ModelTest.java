package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.DocumentTest;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.google.common.base.Splitter;
import com.google.re2j.Pattern;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ModelTest {

  @Test
  public void testCallCommandLine() throws Exception {

    String path = Files.createTempDirectory("").toFile().getAbsolutePath();
    File documents = new File(path + File.separator + "papers.jsonl.gz");
    DocumentTest.papers().toFile(doc -> JsonCodec.asString(doc.json()), documents, false, true);
    File goldLabels = new File(path + File.separator + "gold-labels.jsonl.gz");
    DocumentTest.papers().index().flatten(
            doc -> View.of(Splitter.on('\f').trimResults().omitEmptyStrings().split((String) doc.getValue().text()))
                .map(page -> {
                  boolean isTruePositive = Pattern.matches("(?ms).*crowdsourcing.*", page.toLowerCase());
                  return new GoldLabel(doc.getKey().toString(), "crowdsourcing", (String) doc.getValue().text(),
                      !isTruePositive, isTruePositive, false, false);
                })).index()
        .filter(item -> item.getValue().isTruePositive() || item.getKey() % 4 == 0 /* speed-up the test */)
        .map(Map.Entry::getValue).toFile(goldLabel -> JsonCodec.asString(goldLabel.asMap()), goldLabels, false, true);

    // crowdsourcing
    String[] args = new String[]{documents.getAbsolutePath(), goldLabels.getAbsolutePath(), "crowdsourcing",
        "dt,svm,logit"};
    Model.main(args);

    Assert.assertTrue(new File(path + File.separator + "ensemble-model-crowdsourcing.xml.gz").exists());
    Assert.assertTrue(new File(path + File.separator + "vocabulary-unigrams.tsv.gz").exists());
    Assert.assertTrue(new File(path + File.separator + "vocabulary-bigrams.tsv.gz").exists());
    Assert.assertTrue(new File(path + File.separator + "vocabulary-trigrams.tsv.gz").exists());
    Assert.assertTrue(new File(path + File.separator + "vocabulary-quadgrams.tsv.gz").exists());

    // TODO
  }
}
