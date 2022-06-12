package com.computablefacts.asterix.queries;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.computablefacts.asterix.Generated;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.StringCodec;
import com.computablefacts.logfmt.LogFormatter;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Terminal (leaf) expression node class.
 *
 * See http://www.blackbeltcoder.com/Articles/data/easy-full-text-search-queries for details.
 */
@CheckReturnValue
final public class TerminalNode<T extends AbstractQueryEngine> extends AbstractNode<T> {

  private static final Logger logger_ = LoggerFactory.getLogger(TerminalNode.class);

  private final eTermForms form_;
  private final String key_;
  private final String value_;

  public TerminalNode(eTermForms form, String key, String value) {
    form_ = Preconditions.checkNotNull(form, "form should not be null");
    key_ = Strings.nullToEmpty(key);
    value_ = Strings.nullToEmpty(value);
  }

  public static Optional<Map.Entry<Number, Number>> range(String value) {

    List<String> range =
        Splitter.on(QueryBuilder._TO_).trimResults().omitEmptyStrings().splitToList(value);

    if (range.size() == 2) {

      String min = range.get(0);
      String max = range.get(1);

      boolean isValid = ("*".equals(min) && StringCodec.isNumber(max))
          || ("*".equals(max) && StringCodec.isNumber(min))
          || (StringCodec.isNumber(min) && StringCodec.isNumber(max));

      if (isValid) {

        // Set range
        String minTerm = "*".equals(min) ? null : min;
        String maxTerm = "*".equals(max) ? null : max;

        Number minNumber =
            minTerm == null ? null : (Number) StringCodec.defaultCoercer(minTerm, false);
        Number maxNumber =
            maxTerm == null ? null : (Number) StringCodec.defaultCoercer(maxTerm, false);

        return Optional.of(new AbstractMap.SimpleImmutableEntry<>(minNumber, maxNumber));
      }
    }
    return Optional.empty();
  }

  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder();

    if (form_ == eTermForms.Inflectional) {
      if (!Strings.isNullOrEmpty(key_)) {
        builder.append(key_);
        builder.append(':');
      }
      builder.append(value_);
    } else if (form_ == eTermForms.Literal) {
      if (!Strings.isNullOrEmpty(key_)) {
        builder.append(key_);
        builder.append(':');
      }
      builder.append('\"');
      builder.append(value_);
      builder.append('\"');
    } else if (form_ == eTermForms.Thesaurus) {
      if (!Strings.isNullOrEmpty(key_)) {
        builder.append(key_);
        builder.append(':');
      }
      builder.append('~');
      builder.append(value_);
    } else if (form_ == eTermForms.Range) {
      if (!Strings.isNullOrEmpty(key_)) {
        builder.append(key_);
        builder.append(':');
      }
      builder.append('[');
      builder.append(value_);
      builder.append(']');
    }
    return (exclude() ? "Not(" : "") + builder + (exclude() ? ")" : "");
  }

  @Generated
  public eTermForms form() {
    return form_;
  }

  @Generated
  public String key() {
    return key_;
  }

  @Generated
  public String value() {
    return value_;
  }

  @Override
  public long cardinality(T engine) {

    Preconditions.checkNotNull(engine, "engine should not be null");

    if (logger_.isDebugEnabled()) {
      logger_.debug(LogFormatter.create().add("form", form_).add("key", key_)
          .add("value", value_).formatDebug());
    }
    if (eTermForms.Range.equals(form_)) {
      return engine.rangeCardinality(key_, value_);
    }
    if (eTermForms.Inflectional.equals(form_)) {
      return engine.inflectionalCardinality(key_, value_);
    }
    if (eTermForms.Literal.equals(form_)) {
      return engine.literalCardinality(key_, value_);
    }
    if (eTermForms.Thesaurus.equals(form_)) {
      return engine.thesaurusCardinality(key_, value_);
    }
    return 0L;
  }

  @Override
  public View<String> execute(T engine) {

    Preconditions.checkNotNull(engine, "engine should not be null");

    if (logger_.isDebugEnabled()) {
      logger_.debug(LogFormatter.create().add("form", form_).add("key", key_)
          .add("value", value_).formatDebug());
    }
    if (eTermForms.Range.equals(form_)) {
      return engine.rangeQuery(key_, value_);
    }
    if (eTermForms.Inflectional.equals(form_)) {
      return engine.inflectionalQuery(key_, value_);
    }
    if (eTermForms.Literal.equals(form_)) {
      return engine.literalQuery(key_, value_);
    }
    if (eTermForms.Thesaurus.equals(form_)) {
      return engine.thesaurusQuery(key_, value_);
    }
    return View.of();
  }

  public enum eTermForms {
    Inflectional, Literal, Thesaurus, Range
  }
}
