package com.computablefacts.asterix.ml;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@CheckReturnValue
final public class RegexVectorizer implements Function<String, FeatureVector> {

  private final Pattern pattern_;
  private final List<Double> weights_;

  public RegexVectorizer(Pattern pattern) {
    pattern_ = Preconditions.checkNotNull(pattern, "pattern should not be null");
    weights_ = null;
  }

  public RegexVectorizer(Pattern pattern, List<Double> weights) {

    Preconditions.checkNotNull(pattern, "pattern should not be null");
    Preconditions.checkArgument(pattern.groupCount() == weights.size(),
        "mismatch between the number of groups and the number of weights");

    pattern_ = pattern;
    weights_ = new ArrayList<>(weights);
  }

  @Override
  public FeatureVector apply(String text) {

    Matcher matcher = pattern_.matcher(Strings.nullToEmpty(text));
    FeatureVector vector = new FeatureVector(matcher.groupCount());

    while (matcher.find()) {
      for (int i = 1; i <= matcher.groupCount(); i++) {

        int start = matcher.start(i);
        int end = matcher.end(i);

        if (start >= 0 && end >= 0) {
          if (weights_ == null) {
            vector.set(i - 1, vector.get(i - 1) + 1.0);
          } else {
            vector.set(i - 1, vector.get(i - 1) + weights_.get(i - 1));
          }
        }
      }
    }
    return vector;
  }
}
