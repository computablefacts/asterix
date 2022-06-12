package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.SpanSequence;
import com.computablefacts.asterix.View;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.function.Function;

@CheckReturnValue
final public class TokensToNGrams implements Function<SpanSequence, SpanSequence> {

  private final int length_;

  public TokensToNGrams(int length) {

    Preconditions.checkArgument(length > 0, "length must be > 0");

    length_ = length;
  }

  @Override
  public SpanSequence apply(SpanSequence spans) {

    SpanSequence newSpans = new SpanSequence();

    View.of(spans).overlappingWindow(length_).forEachRemaining(ngram -> {
      int first = ngram.get(0).begin();
      int last = ngram.get(ngram.size() - 1).end();
      Span newSpan = new Span(ngram.get(0).rawText(), first, last);
      newSpans.add(newSpan);
    });
    return newSpans;
  }
}
