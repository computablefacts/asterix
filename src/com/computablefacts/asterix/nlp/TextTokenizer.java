package com.computablefacts.asterix.nlp;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.function.Function;
import javax.validation.constraints.NotNull;

@CheckReturnValue
public class TextTokenizer implements Function<String, SpanSequence> {

  public TextTokenizer() {
  }

  public static boolean isWord(Span span) {
    return span != null && span.hasTag("WORD");
  }

  public static boolean isApostrophe(Span span) {
    return span != null && span.hasTag("APOSTROPHE");
  }

  public static boolean isArrow(Span span) {
    return span != null && span.hasTag("ARROW");
  }

  public static boolean isBracket(Span span) {
    return span != null && span.hasTag("BRACKET");
  }

  public static boolean isCjkSymbol(Span span) {
    return span != null && span.hasTag("CJK_SYMBOL");
  }

  public static boolean isCurrency(Span span) {
    return span != null && span.hasTag("CURRENCY");
  }

  public static boolean isDoubleQuotationMark(Span span) {
    return span != null && span.hasTag("DOUBLE_QUOTATION_MARK");
  }

  public static boolean isGeneralPunctuation(Span span) {
    return span != null && span.hasTag("GENERAL_PUNCTUATION");
  }

  public static boolean isNumber(Span span) {
    return span != null && span.hasTag("NUMBER");
  }

  public static boolean isListMark(Span span) {
    return span != null && span.hasTag("LIST_MARK");
  }

  public static boolean isPunctuation(Span span) {
    return span != null && span.hasTag("PUNCTUATION");
  }

  public static boolean isQuotationMark(Span span) {
    return span != null && span.hasTag("QUOTATION_MARK");
  }

  public static boolean isSeparatorMark(Span span) {
    return span != null && span.hasTag("SEPARATOR_MARK");
  }

  public static boolean isSingleQuotationMark(Span span) {
    return span != null && span.hasTag("SINGLE_QUOTATION_MARK");
  }

  public static boolean isTerminalMark(Span span) {
    return span != null && span.hasTag("TERMINAL_MARK");
  }

  @Override
  final public SpanSequence apply(String text) {

    Preconditions.checkNotNull(text, "text should not be null");

    @Var int begin = 0;
    SpanSequence spans = new SpanSequence();
    StringBuilder token = new StringBuilder();
    StringIterator iterator = new StringIterator(text);

    while (iterator.hasNext()) {

      char c = iterator.next();

      boolean isApostrophe = StringIterator.isApostrophe(c);
      boolean isArrow = StringIterator.isArrow(c);
      boolean isBracket = StringIterator.isBracket(c);
      boolean isCjkSymbol = StringIterator.isCjkSymbol(c);
      boolean isCurrency = StringIterator.isCurrency(c);
      boolean isDoubleQuotationMark = StringIterator.isDoubleQuotationMark(c);
      boolean isGeneralPunctuation = StringIterator.isGeneralPunctuation(c);
      boolean isNumber = Character.isDigit((int) c);
      boolean isListMark = StringIterator.isListMark(c);
      boolean isPunctuation = StringIterator.isPunctuation(c);
      boolean isQuotationMark = StringIterator.isQuotationMark(c);
      boolean isSeparatorMark = StringIterator.isSeparatorMark(c);
      boolean isSingleQuotationMark = StringIterator.isSingleQuotationMark(c);
      boolean isTerminalMark = StringIterator.isTerminalMark(c);
      boolean isWhitespace = StringIterator.isWhitespace(c);

      if (!isApostrophe && !isArrow && !isBracket && !isCjkSymbol && !isCurrency && !isDoubleQuotationMark
          && !isGeneralPunctuation && !isNumber && !isListMark && !isPunctuation && !isQuotationMark && !isSeparatorMark
          && !isSingleQuotationMark && !isTerminalMark && !isWhitespace) {
        token.append(c);
      } else {
        if (token.length() <= 0 && !isWhitespace) {
          Span span = new Span(text, iterator.position() - 1, iterator.position());
          if (isApostrophe) {
            span.addTag("APOSTROPHE");
          }
          if (isArrow) {
            span.addTag("ARROW");
          }
          if (isBracket) {
            span.addTag("BRACKET");
          }
          if (isCjkSymbol) {
            span.addTag("CJK_SYMBOL");
          }
          if (isCurrency) {
            span.addTag("CURRENCY");
          }
          if (isDoubleQuotationMark) {
            span.addTag("DOUBLE_QUOTATION_MARK");
          }
          if (isGeneralPunctuation) {
            span.addTag("GENERAL_PUNCTUATION");
          }
          if (isNumber) {
            span.addTag("NUMBER");
          }
          if (isListMark) {
            span.addTag("LIST_MARK");
          }
          if (isPunctuation) {
            span.addTag("PUNCTUATION");
          }
          if (isQuotationMark) {
            span.addTag("QUOTATION_MARK");
          }
          if (isSeparatorMark) {
            span.addTag("SEPARATOR_MARK");
          }
          if (isSingleQuotationMark) {
            span.addTag("SINGLE_QUOTATION_MARK");
          }
          if (isTerminalMark) {
            span.addTag("TERMINAL_MARK");
          }
          addMoreTags(span);
          addMoreFeatures(span);
          spans.add(span); // span == c
        } else if (token.length() > 0 && isWhitespace) {
          Span span = new Span(text, begin, iterator.position() - 1);
          span.addTag("WORD");
          addMoreTags(span);
          addMoreFeatures(span);
          spans.add(span);
        } else if (token.length() > 0) {
          Span span1 = new Span(text, begin, iterator.position() - 1);
          span1.addTag("WORD");
          addMoreTags(span1);
          addMoreFeatures(span1);
          spans.add(span1); // span1 == token
          Span span2 = new Span(text, iterator.position() - 1, iterator.position());
          if (isApostrophe) {
            span2.addTag("APOSTROPHE");
          }
          if (isArrow) {
            span2.addTag("ARROW");
          }
          if (isBracket) {
            span2.addTag("BRACKET");
          }
          if (isCjkSymbol) {
            span2.addTag("CJK_SYMBOL");
          }
          if (isCurrency) {
            span2.addTag("CURRENCY");
          }
          if (isDoubleQuotationMark) {
            span2.addTag("DOUBLE_QUOTATION_MARK");
          }
          if (isGeneralPunctuation) {
            span2.addTag("GENERAL_PUNCTUATION");
          }
          if (isNumber) {
            span2.addTag("NUMBER");
          }
          if (isListMark) {
            span2.addTag("LIST_MARK");
          }
          if (isPunctuation) {
            span2.addTag("PUNCTUATION");
          }
          if (isQuotationMark) {
            span2.addTag("QUOTATION_MARK");
          }
          if (isSeparatorMark) {
            span2.addTag("SEPARATOR_MARK");
          }
          if (isSingleQuotationMark) {
            span2.addTag("SINGLE_QUOTATION_MARK");
          }
          if (isTerminalMark) {
            span2.addTag("TERMINAL_MARK");
          }
          addMoreTags(span2);
          addMoreFeatures(span2);
          spans.add(span2); // span2 == c
        }
        token.setLength(0);
        begin = iterator.position();
      }
    }
    return spans;
  }

  protected void addMoreTags(@NotNull Span span) {
  }

  protected void addMoreFeatures(@NotNull Span span) {
  }
}
