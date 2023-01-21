package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/ad_fact.pl
 */
public class AdFactTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.3::p(1).");
    facts.add("0.4::p(2).");

    return facts;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("p(1)?", "0.3::p(1).");
    queries.put("p(2)?", "0.4::p(2).");

    return queries;
  }
}