package com.computablefacts.asterix.queries;

import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.StringCodec;
import com.computablefacts.asterix.nlp.WildcardMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.Var;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The execution layer.
 */
public abstract class AbstractQueryEngine {

  /**
   * Estimate the cardinality of the range query.
   *
   * @param key   the field.
   * @param value the field's value to match.
   * @return the estimated number of documents ids returned by {@link #rangeQuery(String, String)}.
   */
  public long rangeCardinality(String key, String value) {
    return rangeQuery(key, value).reduce(0L, (carry, id) -> carry + 1L);
  }

  /**
   * Estimate the cardinality of the inflectional query.
   *
   * @param key   the field.
   * @param value the field's value to match.
   * @return the estimated number of documents ids returned by {@link #inflectionalQuery(String, String)}.
   */
  public long inflectionalCardinality(String key, String value) {
    return inflectionalQuery(key, value).reduce(0L, (carry, id) -> carry + 1L);
  }

  /**
   * Estimate the cardinality of the literal query.
   *
   * @param key   the field.
   * @param value the field's value to match.
   * @return the estimated number of documents ids returned by {@link #literalQuery(String, String)}.
   */
  public long literalCardinality(String key, String value) {
    return literalQuery(key, value).reduce(0L, (carry, id) -> carry + 1L);
  }

  /**
   * Estimate the cardinality of the thesaurus query.
   *
   * @param key   the field.
   * @param value the field's value to match.
   * @return the estimated number of documents ids returned by {@link #thesaurusQuery(String, String)}.
   */
  public long thesaurusCardinality(String key, String value) {
    return thesaurusQuery(key, value).reduce(0L, (carry, id) -> carry + 1L);
  }

  /**
   * Perform a range query.
   *
   * @param key   the field.
   * @param value the field's value to match.
   * @return an ordered stream of documents ids.
   */
  public View<String> rangeQuery(String key, String value) {

    Optional<Map.Entry<Number, Number>> range = TerminalNode.range(value);

    if (!range.isPresent()) {
      return View.of();
    }

    Number min = range.get().getKey();
    Number max = range.get().getValue();

    View<String> results = executeQuery(key, min, max);

    return results == null ? View.of() : results;
  }

  /**
   * Perform an inflectional query.
   *
   * @param key   the field.
   * @param value the field's value to match.
   * @return an ordered stream of documents ids.
   */
  public View<String> inflectionalQuery(String key, String value) {

    List<String> terms = tokenize(value);
    List<View<String>> views = new ArrayList<>();

    for (String term : terms) {

      View<String> results = executeQuery(key, term);

      if (results != null) {
        views.add(results.dedupSorted());
      }
    }
    return views.isEmpty() ? View.of() : views.size() == 1 ? views.get(0)
        : views.get(0).mergeSorted(views.subList(1, views.size()), String::compareTo).dedupSorted();
  }

  /**
   * Perform a literal query.
   *
   * @param key   the field.
   * @param value the field's value to match.
   * @return an ordered stream of documents ids.
   */
  public View<String> literalQuery(String key, String value) {

    List<String> terms = tokenize(value);
    @Var View<String> view = null;

    for (String term : terms) {

      View<String> results = executeQuery(key, term);

      if (results != null) {
        if (view == null) {
          view = results.dedupSorted();
        } else {
          view = view.intersectSorted(results.dedupSorted());
        }
        if (!view.hasNext()) {
          return View.of();
        }
      }
    }
    return view == null ? View.of() : view;
  }

  /**
   * Perform a thesaurus query.
   *
   * @param key   the field.
   * @param value the field's value to match.
   * @return an ordered stream of documents ids.
   */
  public View<String> thesaurusQuery(String key, String value) {

    List<String> terms = tokenize(value);
    @Var View<String> view = null;

    for (String term : terms) {

      Set<String> newTerms = map(term);
      List<View<String>> newViews = new ArrayList<>();

      for (String newTerm : newTerms) {

        View<String> results = executeQuery(key, newTerm);

        if (results != null) {
          newViews.add(results.dedupSorted());
        }
      }

      View<String> results = newViews.isEmpty() ? View.of() : newViews.size() == 1 ? newViews.get(0)
          : newViews.get(0).mergeSorted(newViews.subList(1, newViews.size()), String::compareTo).dedupSorted();

      if (view == null) {
        view = results;
      } else {
        view = view.intersectSorted(results);
      }
      if (!view.hasNext()) {
        return View.of();
      }
    }
    return view == null ? View.of() : view;
  }

  /**
   * Tokenize a string.
   *
   * @param value the string to tokenize.
   * @return a list of tokens/terms.
   */
  protected List<String> tokenize(String value) {
    return View.of(Splitter.on(' ').splitToList(value)).flatten(token -> {

      List<String> tokens = WildcardMatcher.split(token);

      if (tokens.size() == 1) {
        return View.of(StringCodec.defaultTokenizer2(tokens.get(0)));
      }

      String newTokens = View.of(tokens).map(tkn -> {
        if (WildcardMatcher.isOnlyMadeOfWildcards(tkn)) {
          return tkn;
        }
        return View.of(StringCodec.defaultTokenizer2(tkn)).join(Function.identity(), "*");
      }).join(Function.identity(), "*");

      return View.of(WildcardMatcher.compact(newTokens));
    }).toList();
  }

  /**
   * Map a single term to a set of equivalent terms.
   *
   * @param term the term to lookup in the dictionary.
   * @return a set of equivalent terms.
   */
  protected Set<String> map(String term) {
    return Sets.newHashSet(term);
  }

  /**
   * Returns the list of documents containing a given term.
   *
   * @param key  the field.
   * @param term the field's term to match.
   * @return an ordered stream of documents ids.
   */
  protected abstract View<String> executeQuery(String key, String term);

  /**
   * Returns the list of documents containing a numeric value between {@code min} and {@code max} included.
   *
   * @param key the field.
   * @param min the field's minimum value to match (included).
   * @param max the field's maximum value to match (included).
   * @return an ordered stream of documents ids.
   */
  protected abstract View<String> executeQuery(String key, Number min, Number max);
}