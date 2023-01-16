package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mantadelis, Theofrastos &amp; Janssens, Gerda. (2010). "Dedicated Tabling for a Probabilistic Setting". Technical
 * Communications of ICLP. 7. 124-133. 10.4230/LIPIcs.ICLP.2010.124.
 */
public class Graph1Test extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.1::edge(1, 2).");
    facts.add("0.5::edge(1, 3).");
    facts.add("0.7::edge(3, 1).");
    facts.add("0.3::edge(2, 3).");
    facts.add("0.2::edge(3, 2).");
    facts.add("0.6::edge(2, 4).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("path(X, Y) :- edge(X, Y).");
    rules.add("path(X, Y) :- path(X, Z), fn_eq(U, X, Z), fn_is_false(U), edge(Z, Y).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("path(1, 1)?", "0.3605::path(1, 1).");
    queries.put("path(1, 2)?", "0.19::path(1, 2).");
    queries.put("path(1, 3)?", "0.515::path(1, 3).");
    queries.put("path(1, 4)?", "0.114::path(1, 4).");
    queries.put("path(2, 1)?", "0.21::path(2, 1).");
    queries.put("path(2, 2)?", "0.0768::path(2, 2).");
    queries.put("path(2, 3)?", "0.3::path(2, 3).");
    queries.put("path(2, 4)?", "0.6::path(2, 4).");
    queries.put("path(3, 1)?", "0.7::path(3, 1).");
    queries.put("path(3, 2)?", "0.256::path(3, 2).");
    queries.put("path(3, 3)?", "0.3974::path(3, 3).");
    queries.put("path(3, 4)?", "0.1536::path(3, 4).");

    return queries;
  }
}