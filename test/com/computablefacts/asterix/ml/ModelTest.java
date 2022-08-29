package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.DocumentTest;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.re2j.Pattern;
import java.io.File;
import java.nio.file.Files;
import org.junit.Assert;
import org.junit.Test;

public class ModelTest {

  @Test
  public void testCallCommandLine() throws Exception {

    Pattern pattern = Pattern.compile(".*crowdsourcing.*",
        Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    String path = Files.createTempDirectory("").toFile().getAbsolutePath();

    // Build dataset (documents)
    File documents = new File(path + File.separator + "papers.jsonl.gz");
    DocumentTest.papers().toFile(doc -> JsonCodec.asString(doc.json()), documents, false, true);

    // Build vocabulary
    Vocabulary.main(
        new String[]{documents.getAbsolutePath(), "0.01", "0.99", "1000", "WORD,NUMBER,TERMINAL_MARK", "1"});

    Assert.assertTrue(new File(path + File.separator + "vocabulary-1grams.tsv.gz").exists());

    // Build dataset (gold labels)
    File goldLabels = new File(path + File.separator + "gold-labels.jsonl.gz");
    DocumentTest.papers().map(doc -> (String) doc.text()).flatten(text -> View.of(Splitter.on('\f').split(text)))
        .filter(page -> !Strings.isNullOrEmpty(page)).index().map(page -> {
          boolean isTruePositive = pattern.matches(page.getValue());
          return new GoldLabel(page.getKey().toString(), "crowdsourcing", page.getValue(), !isTruePositive, isTruePositive,
              false, false);
        }).index().filter(
            goldLabel -> goldLabel.getValue().isTruePositive() || goldLabel.getKey() % 5 == 0 /* speed-up the test */)
        .toFile(goldLabel -> JsonCodec.asString(goldLabel.getValue().asMap()), goldLabels, false, true);

    // Train model
    Model.main(
        new String[]{documents.getAbsolutePath(), goldLabels.getAbsolutePath(), "crowdsourcing", "dt,svm,logit"});

    Assert.assertTrue(new File(path + File.separator + "ensemble-model-crowdsourcing.xml.gz").exists());
    // TODO
  }
}
