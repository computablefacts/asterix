package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://dtai.cs.kuleuven.be/problog/tutorial/basic/10_inhibitioneffects.html
 */
public class SocialNetwork2Test extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("person(angelika).");
    facts.add("person(boris).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("0.1::initialInf(X) :- person(X).");
    rules.add("0.1::contact(X,Y) :- person(X), person(Y).");
    rules.add("inf(X) :- initialInf(X).");
    rules.add("0.6::inf(X) :- contact(X, Y), inf(Y).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("inf(angelika)?", "0.1054::inf(angelika).");
    queries.put("inf(boris)?", "0.1054::inf(boris).");

    return queries;
  }
}