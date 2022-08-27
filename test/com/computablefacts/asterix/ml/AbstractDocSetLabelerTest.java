package com.computablefacts.asterix.ml;

import static com.computablefacts.asterix.ml.AbstractDocSetLabeler.counts;

import com.computablefacts.asterix.DocumentTest;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.re2j.Pattern;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class AbstractDocSetLabelerTest {

  @Test
  public void testLog2() {
    Assert.assertEquals(0, (int) AbstractDocSetLabeler.log2(1));
    Assert.assertEquals(1, (int) AbstractDocSetLabeler.log2(2));
    Assert.assertEquals(3, (int) AbstractDocSetLabeler.log2(8));
    Assert.assertEquals(4, (int) AbstractDocSetLabeler.log2(16));
    Assert.assertEquals(5, (int) AbstractDocSetLabeler.log2(32));
    Assert.assertEquals(6, (int) AbstractDocSetLabeler.log2(64));
    Assert.assertEquals(7, (int) AbstractDocSetLabeler.log2(128));
    Assert.assertEquals(8, (int) AbstractDocSetLabeler.log2(256));
    Assert.assertEquals(9, (int) AbstractDocSetLabeler.log2(512));
    Assert.assertEquals(10, (int) AbstractDocSetLabeler.log2(1024));
  }

  @Test
  public void testEntropy() {

    Assert.assertEquals(0, AbstractDocSetLabeler.entropy(0, 0, 0), 0.00001);
    Assert.assertEquals(0, AbstractDocSetLabeler.entropy(1, 1, 0), 0.00001);
    Assert.assertEquals(0, AbstractDocSetLabeler.entropy(1, 0, 1), 0.00001);

    Assert.assertEquals(0.97095, AbstractDocSetLabeler.entropy(5, 2, 3), 0.00001);
    Assert.assertEquals(0.0, AbstractDocSetLabeler.entropy(4, 4, 0), 0.00001);
    Assert.assertEquals(0.97095, AbstractDocSetLabeler.entropy(5, 3, 2), 0.00001);

    Assert.assertEquals(1.0, AbstractDocSetLabeler.entropy(4, 2, 2), 0.00001);
    Assert.assertEquals(0.91829, AbstractDocSetLabeler.entropy(6, 4, 2), 0.00001);
    Assert.assertEquals(0.81127, AbstractDocSetLabeler.entropy(4, 3, 1), 0.00001);

    Assert.assertEquals(0.98522, AbstractDocSetLabeler.entropy(7, 3, 4), 0.00001);
    Assert.assertEquals(0.59167, AbstractDocSetLabeler.entropy(7, 6, 1), 0.00001);
  }

  /**
   * See https://homes.cs.washington.edu/~shapiro/EE596/notes/InfoGain.pdf for details.
   */
  @Test
  public void testInformationGain() {

    Map<String, Set<String>> pos = new HashMap<>();
    pos.put("1", Sets.newHashSet("yes"));
    pos.put("2", Sets.newHashSet("yes"));
    pos.put("3", Sets.newHashSet("yes"));
    pos.put("4", Sets.newHashSet("yes"));
    pos.put("5", Sets.newHashSet("no"));
    pos.put("6", Sets.newHashSet("no"));
    pos.put("7", Sets.newHashSet("no"));
    pos.put("8", Sets.newHashSet("no"));
    pos.put("9", Sets.newHashSet("no"));
    pos.put("10", Sets.newHashSet("no"));
    pos.put("11", Sets.newHashSet("no"));
    pos.put("12", Sets.newHashSet("no"));
    pos.put("13", Sets.newHashSet("no"));
    pos.put("14", Sets.newHashSet("no"));
    pos.put("15", Sets.newHashSet("no"));
    pos.put("16", Sets.newHashSet("no"));
    pos.put("17", Sets.newHashSet("no"));

    Map<String, Set<String>> neg = new HashMap<>();
    neg.put("18", Sets.newHashSet("yes"));
    neg.put("19", Sets.newHashSet("yes"));
    neg.put("20", Sets.newHashSet("yes"));
    neg.put("21", Sets.newHashSet("yes"));
    neg.put("22", Sets.newHashSet("yes"));
    neg.put("23", Sets.newHashSet("yes"));
    neg.put("24", Sets.newHashSet("yes"));
    neg.put("25", Sets.newHashSet("yes"));
    neg.put("26", Sets.newHashSet("yes"));
    neg.put("27", Sets.newHashSet("yes"));
    neg.put("28", Sets.newHashSet("yes"));
    neg.put("29", Sets.newHashSet("yes"));
    neg.put("30", Sets.newHashSet("no"));

    Map<String, Map.Entry<Double, Double>> counts = counts(pos, neg);

    double informationGainYes = AbstractDocSetLabeler.informationGain(pos.size(), counts.get("yes").getKey(),
        neg.size(), counts.get("yes").getValue());
    double informationGainNo = AbstractDocSetLabeler.informationGain(pos.size(), counts.get("no").getKey(), neg.size(),
        counts.get("no").getValue());

    Assert.assertEquals(0.38121, informationGainYes, 0.00001);
    Assert.assertEquals(0.38121, informationGainNo, 0.00001);
  }

  @Test
  public void testIntrinsicValue() {

    Map<String, Set<String>> pos = new HashMap<>();
    pos.put("1", Sets.newHashSet("yes"));
    pos.put("2", Sets.newHashSet("yes"));
    pos.put("3", Sets.newHashSet("yes"));
    pos.put("4", Sets.newHashSet("yes"));
    pos.put("5", Sets.newHashSet("no"));
    pos.put("6", Sets.newHashSet("no"));
    pos.put("7", Sets.newHashSet("no"));
    pos.put("8", Sets.newHashSet("no"));
    pos.put("9", Sets.newHashSet("no"));
    pos.put("10", Sets.newHashSet("no"));
    pos.put("11", Sets.newHashSet("no"));
    pos.put("12", Sets.newHashSet("no"));
    pos.put("13", Sets.newHashSet("no"));
    pos.put("14", Sets.newHashSet("no"));
    pos.put("15", Sets.newHashSet("no"));
    pos.put("16", Sets.newHashSet("no"));
    pos.put("17", Sets.newHashSet("no"));

    Map<String, Set<String>> neg = new HashMap<>();
    neg.put("18", Sets.newHashSet("yes"));
    neg.put("19", Sets.newHashSet("yes"));
    neg.put("20", Sets.newHashSet("yes"));
    neg.put("21", Sets.newHashSet("yes"));
    neg.put("22", Sets.newHashSet("yes"));
    neg.put("23", Sets.newHashSet("yes"));
    neg.put("24", Sets.newHashSet("yes"));
    neg.put("25", Sets.newHashSet("yes"));
    neg.put("26", Sets.newHashSet("yes"));
    neg.put("27", Sets.newHashSet("yes"));
    neg.put("28", Sets.newHashSet("yes"));
    neg.put("29", Sets.newHashSet("yes"));
    neg.put("30", Sets.newHashSet("no"));

    Map<String, Map.Entry<Double, Double>> counts = counts(pos, neg);

    double intrinsicValueYes = AbstractDocSetLabeler.intrinsicValue(pos.size(), counts.get("yes").getKey(), neg.size(),
        counts.get("yes").getValue());
    double intrinsicValueNo = AbstractDocSetLabeler.intrinsicValue(pos.size(), counts.get("no").getKey(), neg.size(),
        counts.get("no").getValue());

    Assert.assertEquals(0.48367, intrinsicValueYes, 0.00001);
    Assert.assertEquals(0.51311, intrinsicValueNo, 0.00001);
  }

  @Test
  public void testInformationGainRatio() {

    Map<String, Set<String>> pos = new HashMap<>();
    pos.put("1", Sets.newHashSet("yes"));
    pos.put("2", Sets.newHashSet("yes"));
    pos.put("3", Sets.newHashSet("yes"));
    pos.put("4", Sets.newHashSet("yes"));
    pos.put("5", Sets.newHashSet("no"));
    pos.put("6", Sets.newHashSet("no"));
    pos.put("7", Sets.newHashSet("no"));
    pos.put("8", Sets.newHashSet("no"));
    pos.put("9", Sets.newHashSet("no"));
    pos.put("10", Sets.newHashSet("no"));
    pos.put("11", Sets.newHashSet("no"));
    pos.put("12", Sets.newHashSet("no"));
    pos.put("13", Sets.newHashSet("no"));
    pos.put("14", Sets.newHashSet("no"));
    pos.put("15", Sets.newHashSet("no"));
    pos.put("16", Sets.newHashSet("no"));
    pos.put("17", Sets.newHashSet("no"));

    Map<String, Set<String>> neg = new HashMap<>();
    neg.put("18", Sets.newHashSet("yes"));
    neg.put("19", Sets.newHashSet("yes"));
    neg.put("20", Sets.newHashSet("yes"));
    neg.put("21", Sets.newHashSet("yes"));
    neg.put("22", Sets.newHashSet("yes"));
    neg.put("23", Sets.newHashSet("yes"));
    neg.put("24", Sets.newHashSet("yes"));
    neg.put("25", Sets.newHashSet("yes"));
    neg.put("26", Sets.newHashSet("yes"));
    neg.put("27", Sets.newHashSet("yes"));
    neg.put("28", Sets.newHashSet("yes"));
    neg.put("29", Sets.newHashSet("yes"));
    neg.put("30", Sets.newHashSet("no"));

    Map<String, Map.Entry<Double, Double>> counts = counts(pos, neg);

    double informationGainRatioYes = AbstractDocSetLabeler.informationGainRatio("yes", Sets.newHashSet("yes", "no"),
        counts, pos.size(), neg.size());
    double informationGainRatioNo = AbstractDocSetLabeler.informationGainRatio("no", Sets.newHashSet("yes", "no"),
        counts, pos.size(), neg.size());

    Assert.assertEquals(0.38244, informationGainRatioYes, 0.00001);
    Assert.assertEquals(0.38244, informationGainRatioNo, 0.00001);
  }

  /**
   * See https://en.wikipedia.org/wiki/Information_gain_ratio#Information_gain_calculation and
   * https://www.saedsayad.com/decision_tree.htm for details.
   */
  @Test
  public void testInformationGain2() {

    Map<String, Set<String>> pos = new HashMap<>();
    pos.put("1", Sets.newHashSet("overcast", "hot", "high", "false"));
    pos.put("2", Sets.newHashSet("sunny", "mild", "high", "false"));
    pos.put("3", Sets.newHashSet("sunny", "cool", "normal", "false"));
    pos.put("4", Sets.newHashSet("overcast", "cool", "normal", "true"));
    pos.put("5", Sets.newHashSet("rainy", "cool", "normal", "false"));
    pos.put("7", Sets.newHashSet("sunny", "mild", "normal", "false"));
    pos.put("6", Sets.newHashSet("rainy", "mild", "normal", "true"));
    pos.put("8", Sets.newHashSet("overcast", "mild", "high", "true"));
    pos.put("9", Sets.newHashSet("overcast", "hot", "normal", "false"));

    Map<String, Set<String>> neg = new HashMap<>();
    neg.put("10", Sets.newHashSet("rainy", "hot", "high", "false"));
    neg.put("11", Sets.newHashSet("rainy", "hot", "high", "true"));
    neg.put("12", Sets.newHashSet("sunny", "cool", "normal", "true"));
    neg.put("13", Sets.newHashSet("rainy", "mild", "high", "false"));
    neg.put("14", Sets.newHashSet("sunny", "mild", "high", "true"));

    Map<String, Map.Entry<Double, Double>> counts = counts(pos, neg);

    double informationGainHumidityHigh = AbstractDocSetLabeler.informationGain(pos.size(), counts.get("high").getKey(),
        neg.size(), counts.get("high").getValue());
    double informationGainHumidityNormal = AbstractDocSetLabeler.informationGain(pos.size(),
        counts.get("normal").getKey(), neg.size(), counts.get("normal").getValue());

    Assert.assertEquals(0.15183, informationGainHumidityHigh, 0.00001);
    Assert.assertEquals(0.15183, informationGainHumidityNormal, 0.00001);

    double informationGainWindyTrue = AbstractDocSetLabeler.informationGain(pos.size(), counts.get("true").getKey(),
        neg.size(), counts.get("true").getValue());
    double informationGainWindyFalse = AbstractDocSetLabeler.informationGain(pos.size(), counts.get("false").getKey(),
        neg.size(), counts.get("false").getValue());

    Assert.assertEquals(0.04812, informationGainWindyTrue, 0.00001);
    Assert.assertEquals(0.04812, informationGainWindyFalse, 0.00001);
  }

  @Test
  public void testIntrinsicValue2() {

    Map<String, Set<String>> pos = new HashMap<>();
    pos.put("1", Sets.newHashSet("overcast", "hot", "high", "false"));
    pos.put("2", Sets.newHashSet("sunny", "mild", "high", "false"));
    pos.put("3", Sets.newHashSet("sunny", "cool", "normal", "false"));
    pos.put("4", Sets.newHashSet("overcast", "cool", "normal", "true"));
    pos.put("5", Sets.newHashSet("rainy", "cool", "normal", "false"));
    pos.put("7", Sets.newHashSet("sunny", "mild", "normal", "false"));
    pos.put("6", Sets.newHashSet("rainy", "mild", "normal", "true"));
    pos.put("8", Sets.newHashSet("overcast", "mild", "high", "true"));
    pos.put("9", Sets.newHashSet("overcast", "hot", "normal", "false"));

    Map<String, Set<String>> neg = new HashMap<>();
    neg.put("10", Sets.newHashSet("rainy", "hot", "high", "false"));
    neg.put("11", Sets.newHashSet("rainy", "hot", "high", "true"));
    neg.put("12", Sets.newHashSet("sunny", "cool", "normal", "true"));
    neg.put("13", Sets.newHashSet("rainy", "mild", "high", "false"));
    neg.put("14", Sets.newHashSet("sunny", "mild", "high", "true"));

    Map<String, Map.Entry<Double, Double>> counts = counts(pos, neg);

    double intrinsicValueHumidityHigh = AbstractDocSetLabeler.intrinsicValue(pos.size(), counts.get("high").getKey(),
        neg.size(), counts.get("high").getValue());
    double intrinsicValueHumidityNormal = AbstractDocSetLabeler.intrinsicValue(pos.size(),
        counts.get("normal").getKey(), neg.size(), counts.get("normal").getValue());

    Assert.assertEquals(0.5, intrinsicValueHumidityHigh, 0.00001);
    Assert.assertEquals(0.5, intrinsicValueHumidityNormal, 0.00001);

    double intrinsicValueWindyTrue = AbstractDocSetLabeler.intrinsicValue(pos.size(), counts.get("true").getKey(),
        neg.size(), counts.get("true").getValue());
    double intrinsicValueWindyFalse = AbstractDocSetLabeler.intrinsicValue(pos.size(), counts.get("false").getKey(),
        neg.size(), counts.get("false").getValue());

    Assert.assertEquals(0.52388, intrinsicValueWindyTrue, 0.00001);
    Assert.assertEquals(0.46134, intrinsicValueWindyFalse, 0.00001);
  }

  @Test
  public void testInformationGainRatio2() {

    Map<String, Set<String>> pos = new HashMap<>();
    pos.put("1", Sets.newHashSet("overcast", "hot", "high", "false"));
    pos.put("2", Sets.newHashSet("sunny", "mild", "high", "false"));
    pos.put("3", Sets.newHashSet("sunny", "cool", "normal", "false"));
    pos.put("4", Sets.newHashSet("overcast", "cool", "normal", "true"));
    pos.put("5", Sets.newHashSet("rainy", "cool", "normal", "false"));
    pos.put("7", Sets.newHashSet("sunny", "mild", "normal", "false"));
    pos.put("6", Sets.newHashSet("rainy", "mild", "normal", "true"));
    pos.put("8", Sets.newHashSet("overcast", "mild", "high", "true"));
    pos.put("9", Sets.newHashSet("overcast", "hot", "normal", "false"));

    Map<String, Set<String>> neg = new HashMap<>();
    neg.put("10", Sets.newHashSet("rainy", "hot", "high", "false"));
    neg.put("11", Sets.newHashSet("rainy", "hot", "high", "true"));
    neg.put("12", Sets.newHashSet("sunny", "cool", "normal", "true"));
    neg.put("13", Sets.newHashSet("rainy", "mild", "high", "false"));
    neg.put("14", Sets.newHashSet("sunny", "mild", "high", "true"));

    Map<String, Map.Entry<Double, Double>> counts = counts(pos, neg);

    double informationGainRatioHumidityHigh = AbstractDocSetLabeler.informationGainRatio("high",
        Sets.newHashSet("high", "normal"), counts, pos.size(), neg.size());
    double informationGainRatioHumidityNormal = AbstractDocSetLabeler.informationGainRatio("normal",
        Sets.newHashSet("high", "normal"), counts, pos.size(), neg.size());

    Assert.assertEquals(0.15183, informationGainRatioHumidityHigh, 0.00001);
    Assert.assertEquals(0.15183, informationGainRatioHumidityNormal, 0.00001);

    double informationGainRatioWindyTrue = AbstractDocSetLabeler.informationGainRatio("true",
        Sets.newHashSet("true", "false"), counts, pos.size(), neg.size());
    double informationGainRatioWindyFalse = AbstractDocSetLabeler.informationGainRatio("false",
        Sets.newHashSet("true", "false"), counts, pos.size(), neg.size());

    Assert.assertEquals(0.04884, informationGainRatioWindyTrue, 0.00001);
    Assert.assertEquals(0.04884, informationGainRatioWindyFalse, 0.00001);
  }

  @Test
  public void testFindInterestingNGrams() throws Exception {

    String path = Files.createTempDirectory("").toFile().getAbsolutePath();
    File file = new File(path + File.separator + "papers.jsonl.gz");
    DocumentTest.papers().toFile(doc -> JsonCodec.asString(doc.json()), file, false, true);

    for (int i = 1; i < 7; i++) {
      String[] args = new String[]{file.getAbsolutePath(), "0.01", "0.99", "1000", "WORD,NUMBER,TERMINAL_MARK",
          Integer.toString(i, 10)};
      Vocabulary.main(args);
    }

    File funigrams = new File(String.format("%svocabulary-1grams.tsv.gz", file.getParent() + File.separator));
    File fbigrams = new File(String.format("%svocabulary-2grams.tsv.gz", file.getParent() + File.separator));
    File ftrigrams = new File(String.format("%svocabulary-3grams.tsv.gz", file.getParent() + File.separator));
    File fquadgrams = new File(String.format("%svocabulary-4grams.tsv.gz", file.getParent() + File.separator));
    File fquintgrams = new File(String.format("%svocabulary-5grams.tsv.gz", file.getParent() + File.separator));
    File fsextgrams = new File(String.format("%svocabulary-6grams.tsv.gz", file.getParent() + File.separator));

    Assert.assertTrue(funigrams.exists());
    Assert.assertTrue(fbigrams.exists());
    Assert.assertTrue(ftrigrams.exists());
    Assert.assertTrue(fquadgrams.exists());
    Assert.assertTrue(fquintgrams.exists());
    Assert.assertTrue(fsextgrams.exists());

    Vocabulary unigrams = funigrams.exists() ? new Vocabulary(funigrams) : null;
    Vocabulary bigrams = fbigrams.exists() ? new Vocabulary(fbigrams) : null;
    Vocabulary trigrams = ftrigrams.exists() ? new Vocabulary(ftrigrams) : null;
    Vocabulary quadgrams = fquadgrams.exists() ? new Vocabulary(fquadgrams) : null;
    Vocabulary quintgrams = fquintgrams.exists() ? new Vocabulary(fquintgrams) : null;
    Vocabulary sextgrams = fsextgrams.exists() ? new Vocabulary(fsextgrams) : null;

    Assert.assertNotNull(unigrams);
    Assert.assertNotNull(bigrams);
    Assert.assertNotNull(trigrams);
    Assert.assertNotNull(quadgrams);
    Assert.assertNotNull(quintgrams);
    Assert.assertNotNull(sextgrams);

    Assert.assertEquals(1000, unigrams.size());
    Assert.assertEquals(1000, bigrams.size());
    Assert.assertEquals(1000, trigrams.size());
    Assert.assertEquals(1000, quadgrams.size());
    Assert.assertEquals(1000, quintgrams.size());
    Assert.assertEquals(1000, sextgrams.size());

    Set<String> ok = new HashSet<>();
    Set<String> ko = new HashSet<>();

    Pattern pattern = Pattern.compile(".*crowdsourcing.*",
        Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    DocumentTest.papers().map(doc -> (String) doc.text()).flatten(text -> View.of(Splitter.on('\f').split(text)))
        .forEachRemaining(page -> {
          if (pattern.matches(page)) {
            ok.add(page);
          } else {
            ko.add(page);
          }
        });

    List<Entry<String, Double>> terms = AbstractDocSetLabeler.findInterestingNGrams(unigrams, bigrams, trigrams,
        quadgrams, quintgrams, sextgrams, View.of(ok).toList(), View.of(ko).sample(500));

    Assert.assertEquals("crowdsourcing", terms.get(0).getKey());
  }
}
