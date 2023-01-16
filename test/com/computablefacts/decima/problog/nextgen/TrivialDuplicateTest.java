package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_duplicate.pl
 */
public class TrivialDuplicateTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.3::p(1).");
    facts.add("0.2::p(2).");
    facts.add("0.6::p(1).");

    return facts;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("p(1)?", "0.72::p(1).");
    queries.put("p(2)?", "0.2::p(2).");

    return queries;
  }
}