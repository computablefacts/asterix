package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/10_cards.pl
 */
public class CardsTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("position(1).");
    facts.add("position(2).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("0.25::card(C, q, h) :- position(C).");
    rules.add("0.25::card(C, k, h) :- position(C).");
    rules.add("0.25::card(C, q, s) :- position(C).");
    rules.add("0.25::card(C, k, s) :- position(C).");
    rules.add("doublecard(X, Y) :- card(C1, X, Y), card(C2, X, Y), C1 < C2.");
    rules.add("spade(C) :- card(C, _, s).");
    rules.add("samecard(A, B) :- card(1, A, B), card(2, A, B).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();

    queries.put("doublecard(q, h)?", "0.0625::doublecard(q, h).");
    queries.put("doublecard(k, h)?", "0.0625::doublecard(k, h).");
    queries.put("doublecard(k, s)?", "0.0625::doublecard(k, s).");
    queries.put("doublecard(q, s)?", "0.0625::doublecard(q, s).");

    queries.put("samecard(q, h)?", "0.0625::samecard(q, h).");
    queries.put("samecard(k, h)?", "0.0625::samecard(k, h).");
    queries.put("samecard(k, s)?", "0.0625::samecard(k, s).");
    queries.put("samecard(q, s)?", "0.0625::samecard(q, s).");

    return queries;
  }
}