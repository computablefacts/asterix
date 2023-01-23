package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/swap.pl
 */
public class SwapTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.5::f(1,2).");
    facts.add("0.5::f(2,1).");
    facts.add("0.5::f(1,3).");
    facts.add("0.5::f(2,3).");
    facts.add("0.5::b(1).");
    facts.add("0.5::b(2).");
    facts.add("0.5::b(3).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("s1(X) :- b(X).");
    rules.add("s1(X) :- f(X,Y),s1(Y).");
    rules.add("s2(X) :- f(X,Y),s2(Y).");
    rules.add("s2(X) :- b(X).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("s1(1)?", "0.73438::s1(1).");
    queries.put("s2(1)?", "0.73438::s2(1).");

    return queries;
  }
}