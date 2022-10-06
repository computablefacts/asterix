package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.View;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@CheckReturnValue
final public class DictionaryVectorizer implements Function<String, FeatureVector> {

  private final Map<String, Double> dictionary_;
  private final List<String> dicKeys_;
  private final Function<String, Multiset<String>> dicBuilder_;

  public DictionaryVectorizer(Map<String, Double> dictionary, Function<String, Multiset<String>> dicBuilder) {

    Preconditions.checkNotNull(dictionary, "dictionary should not be null");
    Preconditions.checkNotNull(dicBuilder, "dicBuilder should not be null");

    dictionary_ = dictionary;
    dicKeys_ = View.of(dictionary.keySet()).toSortedList(Comparator.naturalOrder());
    dicBuilder_ = dicBuilder;
  }

  @Override
  public FeatureVector apply(String text) {

    Multiset<String> dic = dicBuilder_.apply(text);
    FeatureVector vector = new FeatureVector(dicKeys_.size());

    for (int i = 0; i < dicKeys_.size(); i++) {
      String keyword = dicKeys_.get(i);
      if (dic.contains(keyword)) {
        vector.set(i, dic.count(keyword) * dictionary_.get(keyword));
      }
    }
    return vector;
  }

  @Beta
  public List<String> dicKeys() {
    return ImmutableList.copyOf(dicKeys_);
  }
}
