package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.SpanSequence;
import com.computablefacts.asterix.View;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import java.util.function.Function;
import javax.validation.constraints.NotNull;

@CheckReturnValue
public class UnigramsToNGrams implements Function<SpanSequence, SpanSequence> {

  private final int length_;

  public UnigramsToNGrams(int length) {

    Preconditions.checkArgument(length > 0, "length must be > 0");

    length_ = length;
  }

  @Override
  final public SpanSequence apply(SpanSequence spans) {

    Preconditions.checkNotNull(spans, "spans should not be null");

    SpanSequence newSpans = new SpanSequence();

    View.of(spans).overlappingWindow(length_).forEachRemaining(ngram -> {
      int first = ngram.get(0).begin();
      int last = ngram.get(ngram.size() - 1).end();
      Span newSpan = new Span(ngram.get(0).rawText(), first, last);
      addMoreTags(newSpan, ngram);
      addMoreFeatures(newSpan, ngram);
      newSpans.add(newSpan);
    });
    return newSpans;
  }

  protected void addMoreTags(Span newSpan, @NotNull List<Span> ngram) {
  }

  protected void addMoreFeatures(Span newSpan, @NotNull List<Span> ngram) {
  }
}
