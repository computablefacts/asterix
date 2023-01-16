package com.computablefacts.decima.problog.nextgen;

import java.util.HashMap;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_undefined.pl
 */
public class TrivialUndefined1Test extends AbstractTest {

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("p(_)?", null);

    return queries;
  }
}