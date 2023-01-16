package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Theofrastos Mantadelis and Gerda Janssens (2010). "Nesting Probabilistic Inference". Online Proceedings of the 11th
 * International Colloquium on Implementation of Constraint LOgic Programming Systems (CICLOPS 2011), Lexington, KY,
 * U.S.A., July 10, 2011
 */
public class Graph3Test extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.4::edge(a, b).");
    facts.add("0.55::edge(a, c).");
    facts.add("0.8::edge(b, e).");
    facts.add("0.2::edge(b, d).");
    facts.add("0.4::edge(c, d).");
    facts.add("0.3::edge(e, f).");
    facts.add("0.5::edge(d, f).");
    facts.add("0.6::edge(d, g).");
    facts.add("0.7::edge(f, h).");
    facts.add("0.7::edge(g, h).");

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
    queries.put("path(a, b)?", "0.4::path(a, b).");
    queries.put("path(a, c)?", "0.55::path(a, c).");
    queries.put("path(a, d)?", "0.2824::path(a, d).");
    queries.put("path(a, e)?", "0.32::path(a, e).");
    queries.put("path(a, f)?", "0.21915::path(a, f).");
    queries.put("path(a, g)?", "0.16944::path(a, g).");
    queries.put("path(a, h)?", "0.22520::path(a, h).");
    queries.put("path(b, d)?", "0.2::path(b, d).");
    queries.put("path(b, e)?", "0.8::path(b, e).");
    queries.put("path(b, f)?", "0.316::path(b, f).");
    queries.put("path(b, g)?", "0.12::path(b, g).");
    queries.put("path(b, h)?", "0.26874::path(b, h).");
    queries.put("path(c, d)?", "0.4::path(c, d).");
    queries.put("path(c, f)?", "0.2::path(c, f).");
    queries.put("path(c, g)?", "0.24::path(c, g).");
    queries.put("path(c, h)?", "0.2492::path(c, h).");
    queries.put("path(d, f)?", "0.5::path(d, f).");
    queries.put("path(d, g)?", "0.6::path(d, g).");
    queries.put("path(d, h)?", "0.623::path(d, h).");
    queries.put("path(e, f)?", "0.3::path(e, f).");
    queries.put("path(e, h)?", "0.21::path(e, h).");
    queries.put("path(f, h)?", "0.7::path(f, h).");
    queries.put("path(g, h)?", "0.7::path(g, h).");

    return queries;
  }
}