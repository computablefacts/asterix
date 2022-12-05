package com.computablefacts.asterix.nlp;

import com.computablefacts.asterix.DocumentTest;
import com.computablefacts.asterix.IO;
import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Assert;
import org.junit.Test;

public class DocSetLabelerTest {

  @Test
  public void testCommandLine() throws Exception {

    File documents = DocumentTest.documents();
    File facts = DocumentTest.facts();
    File vocabularyCompressed = new File(String.format("%s/vocabulary-1grams.tsv.gz", documents.getParent()));
    File goldLabelsCompressed = new File(String.format("%s/gold-labels.jsonl.gz", facts.getParent()));
    File keywordsCompressed = new File(String.format("%s/keywords-1grams.tsv.gz", facts.getParent()));
    File keywordsDecompressed = new File(String.format("%s/keywords-1grams.tsv", facts.getParent()));
    String[] args = new String[]{documents.getAbsolutePath(), facts.getAbsolutePath(), "1", "0.01", "0.99", "1000",
        "WORD,NUMBER"};
    DocSetLabeler.main(args);

    Assert.assertTrue(vocabularyCompressed.exists());
    Assert.assertTrue(goldLabelsCompressed.exists());

    Assert.assertTrue(keywordsCompressed.exists());
    Assert.assertFalse(keywordsDecompressed.exists());
    Assert.assertTrue(IO.gunzip(keywordsCompressed, keywordsDecompressed));
    Assert.assertTrue(keywordsCompressed.exists());
    Assert.assertTrue(keywordsDecompressed.exists());

    List<String> lines = IO.readLines(keywordsDecompressed);

    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i);
      Assert.assertTrue(line.matches("^[^ ]+\t[^ ]+\t\\d+\\.\\d+(?:[Ee][-+]\\d+)?$"));
    }

    List<Entry<String, Double>> loadOneLabel = DocSetLabeler.load(keywordsCompressed, "contains_crowdsourcing")
        .toList();
    List<Entry<String, Double>> loadAllLabels = DocSetLabeler.load(keywordsCompressed).map(Entry::getValue).toList();

    Assert.assertEquals(loadOneLabel, loadAllLabels);
  }
}
