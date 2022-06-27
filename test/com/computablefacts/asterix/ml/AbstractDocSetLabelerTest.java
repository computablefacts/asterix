package com.computablefacts.asterix.ml;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
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

    double informationGainYes = AbstractDocSetLabeler.informationGain("yes", pos, neg);
    double informationGainNo = AbstractDocSetLabeler.informationGain("no", pos, neg);

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

    double intrinsicValueYes = AbstractDocSetLabeler.intrinsicValue("yes", pos, neg);
    double intrinsicValueNo = AbstractDocSetLabeler.intrinsicValue("no", pos, neg);

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

    double informationGainRatioYes =
        AbstractDocSetLabeler.informationGainRatio("yes", pos, neg, Sets.newHashSet("yes", "no"));
    double informationGainRatioNo =
        AbstractDocSetLabeler.informationGainRatio("no", pos, neg, Sets.newHashSet("yes", "no"));

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

    double informationGainHumidityHigh = AbstractDocSetLabeler.informationGain("high", pos, neg);
    double informationGainHumidityNormal = AbstractDocSetLabeler.informationGain("normal", pos, neg);

    Assert.assertEquals(0.15183, informationGainHumidityHigh, 0.00001);
    Assert.assertEquals(0.15183, informationGainHumidityNormal, 0.00001);

    double informationGainWindyTrue = AbstractDocSetLabeler.informationGain("true", pos, neg);
    double informationGainWindyFalse = AbstractDocSetLabeler.informationGain("false", pos, neg);

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

    double intrinsicValueHumidityHigh = AbstractDocSetLabeler.intrinsicValue("high", pos, neg);
    double intrinsicValueHumidityNormal = AbstractDocSetLabeler.intrinsicValue("normal", pos, neg);

    Assert.assertEquals(0.5, intrinsicValueHumidityHigh, 0.00001);
    Assert.assertEquals(0.5, intrinsicValueHumidityNormal, 0.00001);

    double intrinsicValueWindyTrue = AbstractDocSetLabeler.intrinsicValue("true", pos, neg);
    double intrinsicValueWindyFalse = AbstractDocSetLabeler.intrinsicValue("false", pos, neg);

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

    double informationGainRatioHumidityHigh =
        AbstractDocSetLabeler.informationGainRatio("high", pos, neg, Sets.newHashSet("high", "normal"));
    double informationGainRatioHumidityNormal =
        AbstractDocSetLabeler.informationGainRatio("normal", pos, neg, Sets.newHashSet("high", "normal"));

    Assert.assertEquals(0.15183, informationGainRatioHumidityHigh, 0.00001);
    Assert.assertEquals(0.15183, informationGainRatioHumidityNormal, 0.00001);

    double informationGainRatioWindyTrue =
        AbstractDocSetLabeler.informationGainRatio("true", pos, neg, Sets.newHashSet("true", "false"));
    double informationGainRatioWindyFalse =
        AbstractDocSetLabeler.informationGainRatio("false", pos, neg, Sets.newHashSet("true", "false"));

    Assert.assertEquals(0.04884, informationGainRatioWindyTrue, 0.00001);
    Assert.assertEquals(0.04884, informationGainRatioWindyFalse, 0.00001);
  }
}
