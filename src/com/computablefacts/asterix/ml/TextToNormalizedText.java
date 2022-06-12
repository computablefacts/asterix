package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.codecs.StringCodec;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.function.Function;
import javax.validation.constraints.NotNull;

@CheckReturnValue
public class TextToNormalizedText implements Function<String, String> {

  private final boolean lowercase_;

  public TextToNormalizedText() {
    this(false);
  }

  public TextToNormalizedText(boolean lowercase) {
    lowercase_ = lowercase;
  }

  @Override
  final public String apply(String text) {
    if (Strings.isNullOrEmpty(text)) {
      return "";
    }
    String newText = normalize(text);
    return lowercase_ ? newText.toLowerCase() : newText;
  }

  protected String normalize(@NotNull String text) {
    return StringCodec.removeDiacriticalMarks(text);
  }
}
