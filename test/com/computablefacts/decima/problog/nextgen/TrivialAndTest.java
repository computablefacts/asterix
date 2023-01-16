package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_and.pl
 */
public class TrivialAndTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.5::heads1(a).");
    facts.add("0.6::heads2(a).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("twoHeads(X) :- heads1(X), heads2(X).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("heads1(a)?", "0.5::heads1(a).");
    queries.put("heads2(a)?", "0.6::heads2(a).");
    queries.put("twoHeads(a)?", "0.3::twoHeads(a).");

    return queries;
  }
}