package com.computablefacts.asterix.nlp;

import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.DocumentTest;
import com.computablefacts.asterix.XStream;
import com.computablefacts.asterix.ml.ConfusionMatrix;
import com.computablefacts.asterix.ml.stacking.Stack;
import com.google.errorprone.annotations.Var;
import java.io.File;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ModelTest {

  @Test
  public void testCommandLine() throws Exception {

    File documents = DocumentTest.documents();
    File facts = DocumentTest.facts();
    File vocabularyCompressed = new File(String.format("%s/vocabulary-1grams.tsv.gz", documents.getParent()));
    File goldLabelsCompressed = new File(String.format("%s/gold-labels.jsonl.gz", facts.getParent()));
    File keywordsCompressed = new File(String.format("%s/keywords-1grams.tsv.gz", facts.getParent()));
    @Var String[] args = new String[]{documents.getAbsolutePath(), facts.getAbsolutePath(), "1", "0.0", "1.0", "50000",
        "WORD,NUMBER"};
    DocSetLabeler.main(args);

    Assert.assertTrue(vocabularyCompressed.exists());
    Assert.assertTrue(goldLabelsCompressed.exists());
    Assert.assertTrue(keywordsCompressed.exists());

    File stackCompressed = new File(String.format("%s/stack-contains_crowdsourcing.xml.gz", facts.getParent()));
    args = new String[]{vocabularyCompressed.getAbsolutePath(), goldLabelsCompressed.getAbsolutePath(),
        keywordsCompressed.getAbsolutePath(), "WORD,NUMBER", "DNB,FLD,KNN,MLP,SVM,RF,DT,GBT,AB,LOGIT",
        "contains_crowdsourcing"};
    Model.main(args);

    Assert.assertTrue(stackCompressed.exists());

    Stack stack = XStream.load(stackCompressed);

    Assert.assertNotNull(stack);

    Map.Entry<List<String>, List<Integer>> dataset = GoldLabel.load(goldLabelsCompressed, "contains_crowdsourcing")
        .unzip(gl -> new AbstractMap.SimpleImmutableEntry<>(gl.data(),
            gl.isTruePositive() || gl.isFalseNegative() ? OK : KO));
    List<String> vectors = dataset.getKey();
    List<Integer> categories = dataset.getValue();
    ConfusionMatrix matrix = new ConfusionMatrix();

    for (int i = 0; i < vectors.size(); i++) {
      int actual = categories.get(i);
      int predicted = stack.predict(vectors.get(i));
      matrix.add(actual, predicted);
    }

    Assert.assertEquals(1.0, matrix.matthewsCorrelationCoefficient(), 0.000001);
  }
}
