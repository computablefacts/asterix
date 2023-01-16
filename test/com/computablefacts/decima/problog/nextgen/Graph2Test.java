package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mantadelis, Theofrastos &amp; Janssens, Gerda. (2010). "Dedicated Tabling for a Probabilistic Setting". Technical
 * Communications of ICLP. 7. 124-133. 10.4230/LIPIcs.ICLP.2010.124.
 */
public class Graph2Test extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.9::edge(1, 2).");
    facts.add("0.9::edge(2, 1).");
    facts.add("0.2::edge(5, 4).");
    facts.add("0.4::edge(6, 5).");
    facts.add("0.4::edge(5, 6).");
    facts.add("0.2::edge(4, 5).");
    facts.add("0.8::edge(2, 3).");
    facts.add("0.8::edge(3, 2).");
    facts.add("0.7::edge(1, 6).");
    facts.add("0.5::edge(2, 6).");
    facts.add("0.5::edge(6, 2).");
    facts.add("0.7::edge(6, 1).");
    facts.add("0.7::edge(5, 3).");
    facts.add("0.7::edge(3, 5).");
    facts.add("0.6::edge(3, 4).");
    facts.add("0.6::edge(4, 3).");

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
    queries.put("path(1, 1)?", "0.92645::path(1, 1).");
    queries.put("path(1, 2)?", "0.94324::path(1, 2).");
    queries.put("path(1, 3)?", "0.80541::path(1, 3).");
    queries.put("path(1, 4)?", "0.53864::path(1, 4).");
    queries.put("path(1, 5)?", "0.69611::path(1, 5).");
    queries.put("path(1, 6)?", "0.86680::path(1, 6).");
    queries.put("path(2, 1)?", "0.94324::path(2, 1).");
    queries.put("path(2, 2)?", "0.96451::path(2, 2).");
    queries.put("path(2, 3)?", "0.84799::path(2, 3).");
    queries.put("path(2, 4)?", "0.56601::path(2, 4).");
    queries.put("path(2, 5)?", "0.72285::path(2, 5).");
    queries.put("path(2, 6)?", "0.85857::path(2, 6).");
    queries.put("path(3, 1)?", "0.80541::path(3, 1).");
    queries.put("path(3, 2)?", "0.84799::path(3, 2).");
    queries.put("path(3, 3)?", "0.90317::path(3, 3).");
    queries.put("path(3, 4)?", "0.66226::path(3, 4).");
    queries.put("path(3, 5)?", "0.80485::path(3, 5).");
    queries.put("path(3, 6)?", "0.75445::path(3, 6).");
    queries.put("path(4, 1)?", "0.53864::path(4, 1).");
    queries.put("path(4, 2)?", "0.56601::path(4, 2).");
    queries.put("path(4, 3)?", "0.66226::path(4, 3).");
    queries.put("path(4, 4)?", "0.44537::path(4, 4).");
    queries.put("path(4, 5)?", "0.57356::path(4, 5).");
    queries.put("path(4, 6)?", "0.50858::path(4, 6).");
    queries.put("path(5, 1)?", "0.69611::path(5, 1).");
    queries.put("path(5, 2)?", "0.72285::path(5, 2).");
    queries.put("path(5, 3)?", "0.80485::path(5, 3).");
    queries.put("path(5, 4)?", "0.57356::path(5, 4).");
    queries.put("path(5, 5)?", "0.68209::path(5, 5).");
    queries.put("path(5, 6)?", "0.68792::path(5, 6).");
    queries.put("path(6, 1)?", "0.86680::path(6, 1).");
    queries.put("path(6, 2)?", "0.85857::path(6, 2).");
    queries.put("path(6, 3)?", "0.75445::path(6, 3).");
    queries.put("path(6, 4)?", "0.50858::path(6, 4).");
    queries.put("path(6, 5)?", "0.68792::path(6, 5).");
    queries.put("path(6, 6)?", "0.79411::path(6, 6).");

    return queries;
  }
}