package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * See "Statistical Relational learning and Probabilistic Programming" page 290.
 */
public class HeadsOrTailsTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.4::heads(1).");
    facts.add("0.7::heads(2).");
    facts.add("0.5::heads(3).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("win(a) :- heads(1).");
    rules.add("win(a) :- heads(2), heads(3).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("win(a)?", "0.61::win(a).");

    return queries;
  }
}