package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Angelika Kimmig, Bart Demoen and Luc De Raedt (2010). "On the Implementation of the Probabilistic Logic Programming
 * Language ProbLog". June 2010Theory and Practice of Logic Programming 11(2-3). DOI:10.1017/S1471068410000566
 */
public class Graph4Test extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.8::edge(a, c).");
    facts.add("0.6::edge(b, c).");
    facts.add("0.7::edge(a, b).");
    facts.add("0.9::edge(c, d).");
    facts.add("0.8::edge(c, e).");
    facts.add("0.5::edge(e, d).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("path(X, Y) :- edge(X, Y).");
    rules.add("path(X, Y) :- path(X, Z), edge(Z, Y).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("path(a, b)?", "0.7::path(a, b).");
    queries.put("path(a, c)?", "0.884::path(a, c).");
    queries.put("path(a, d)?", "0.83096::path(a, d).");
    queries.put("path(a, e)?", "0.7072::path(a, e).");
    queries.put("path(b, c)?", "0.6::path(b, c).");
    queries.put("path(b, d)?", "0.564::path(b, d).");
    queries.put("path(b, e)?", "0.48::path(b, e).");
    queries.put("path(c, d)?", "0.94::path(c, d).");
    queries.put("path(c, e)?", "0.8::path(c, e).");
    queries.put("path(e, d)?", "0.5::path(e, d).");

    return queries;
  }
}