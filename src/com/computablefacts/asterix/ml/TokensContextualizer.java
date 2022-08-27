package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.SpanSequence;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Extract tokens with a distance from the central target token not exceeding the context window size as the context
 * tokens of the given center target token.
 */
@CheckReturnValue
final public class TokensContextualizer implements Function<SpanSequence, SpanSequence> {

  private final Random random_ = new Random();
  private final int maxWindowSize_;

  public TokensContextualizer(int maxWindowSize) {

    Preconditions.checkArgument(maxWindowSize > 1, "maxWindowSize must be > 1");

    maxWindowSize_ = maxWindowSize;
  }

  @Override
  public SpanSequence apply(SpanSequence spans) {

    Preconditions.checkNotNull(spans, "spans should not be null");
    Preconditions.checkArgument(spans.size() > 2, "spans.size() must be > 2");

    for (int i = 0; i < spans.size(); i++) {

      // Get a random window length
      int windowSize = random_.nextInt(maxWindowSize_ - 1) + 1;
      int center = i;

      // Center the window around i (excluding the target token at i) and extract tokens before/after the target token at i
      List<String> tokensBefore = new ArrayList<>();
      List<String> tokensAfter = new ArrayList<>();

      IntStream.range(Math.max(0, i - windowSize), Math.min(spans.size(), i + 1 + windowSize)).boxed()
          .filter(idx -> idx != center).forEach(idx -> {
            if (idx < center) {
              tokensBefore.add(spans.span(idx).text());
            }
            if (idx > center) {
              tokensAfter.add(spans.span(idx).text());
            }
          });

      // Save context as a span's feature
      spans.span(center).setFeature("CTX_BEFORE", Joiner.on('\0').join(tokensBefore));
      spans.span(center).setFeature("CTX_AFTER", Joiner.on('\0').join(tokensAfter));
    }
    return spans;
  }
}
