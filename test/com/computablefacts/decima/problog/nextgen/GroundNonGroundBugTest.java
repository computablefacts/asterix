package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/ground_nonground_bug_v4.pl
 */
public class GroundNonGroundBugTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.1::p(1).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("p(X) :- p(1), X is unk.");
    rules.add("fill(X) :- X is unk.");
    rules.add("fill(X) :- p(X), fill(X).");
    rules.add("q(X) :- fill(X).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("q(unk)?", "0.1::q(unk).");

    return queries;
  }
}