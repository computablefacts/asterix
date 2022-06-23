package com.computablefacts.asterix.ml;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@CheckReturnValue
final public class Shuffler<T> implements Function<List<T>, List<T>> {

  public Shuffler() {
  }

  @Override
  public List<T> apply(List<T> texts) {

    Preconditions.checkNotNull(texts, "texts should not be null");

    List<T> newTexts = Lists.newArrayList(texts);
    Collections.shuffle(newTexts);
    return newTexts;
  }
}
