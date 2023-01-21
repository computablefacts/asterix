package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/add.pl
 */
public class AddTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.3::p(1).");
    facts.add("0.4::p(2).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("all(a) :- p(1), p(2).");
    rules.add("none(a) :- ~p(1), ~p(2).");
    rules.add("any(a) :- p(1).");
    rules.add("any(a) :- p(2).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("p(1)?", "0.3::p(1).");
    queries.put("p(2)?", "0.4::p(2).");
    queries.put("all(a)?", "0.12::all(a).");
    queries.put("none(a)?", "0.42::none(a).");
    queries.put("any(a)?", "0.58::any(a).");

    return queries;
  }
}