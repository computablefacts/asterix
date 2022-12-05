package com.computablefacts.asterix.nlp;

import com.computablefacts.asterix.codecs.StringCodec;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.function.Function;
import javax.validation.constraints.NotNull;

@CheckReturnValue
public class TextNormalizer implements Function<String, String> {

  private final boolean lowercase_;

  public TextNormalizer() {
    this(false);
  }

  public TextNormalizer(boolean lowercase) {
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
