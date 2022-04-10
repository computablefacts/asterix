package com.computablefacts.asterix.queries;

import com.computablefacts.asterix.View;

/**
 * The execution layer.
 */
public class AbstractQueryEngine {

  /**
   * Estimate the cardinality of the range query.
   *
   * @param key the field.
   * @param value the field's value to match.
   * @return the estimated number of documents ids returned by {@link #rangeQuery(String, String)}.
   */
  public long rangeCardinality(String key, String value) {
    return 0L;
  }

  /**
   * Estimate the cardinality of the inflectional query.
   *
   * @param key the field.
   * @param value the field's value to match.
   * @return the estimated number of documents ids returned by
   *         {@link #inflectionalQuery(String, String)}.
   */
  public long inflectionalCardinality(String key, String value) {
    return 0L;
  }

  /**
   * Estimate the cardinality of the literal query.
   *
   * @param key the field.
   * @param value the field's value to match.
   * @return the estimated number of documents ids returned by
   *         {@link #literalQuery(String, String)}.
   */
  public long literalCardinality(String key, String value) {
    return 0L;
  }

  /**
   * Estimate the cardinality of the thesaurus query.
   *
   * @param key the field.
   * @param value the field's value to match.
   * @return the estimated number of documents ids returned by
   *         {@link #thesaurusQuery(String, String)}.
   */
  public long thesaurusCardinality(String key, String value) {
    return 0L;
  }

  /**
   * Perform a range query.
   *
   * @param key the field.
   * @param value the field's value to match.
   * @return an ordered stream of documents ids.
   */
  public View<String> rangeQuery(String key, String value) {
    return View.of();
  }

  /**
   * Perform an inflectional query.
   *
   * @param key the field.
   * @param value the field's value to match.
   * @return an ordered stream of documents ids.
   */
  public View<String> inflectionalQuery(String key, String value) {
    return View.of();
  }

  /**
   * Perform a literal query.
   *
   * @param key the field.
   * @param value the field's value to match.
   * @return an ordered stream of documents ids.
   */
  public View<String> literalQuery(String key, String value) {
    return View.of();
  }

  /**
   * Perform a thesaurus query.
   *
   * @param key the field.
   * @param value the field's value to match.
   * @return an ordered stream of documents ids.
   */
  public View<String> thesaurusQuery(String key, String value) {
    return View.of();
  }
}
