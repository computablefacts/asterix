package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * http://csci431.artifice.cc/notes/problog.html
 */
public class SocialNetwork1Test extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("person(angelika).");
    facts.add("person(joris).");
    facts.add("person(jonas).");
    facts.add("person(dimitar).");
    facts.add("friend(joris, jonas).");
    facts.add("friend(joris, angelika).");
    facts.add("friend(joris, dimitar).");
    facts.add("friend(angelika, jonas).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("0.3::stress(X) :- person(X).");
    rules.add("0.2::influences(X,Y) :- person(X), person(Y).");
    rules.add("smokes(X) :- stress(X).");
    rules.add("smokes(X) :- friend(X,Y), influences(Y,X), smokes(Y).");
    rules.add("0.4::asthma(X) :- smokes(X).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("smokes(angelika)?", "0.342::smokes(angelika).");
    // TODO : queries.put("smokes(joris)?", "0.42301::smokes(joris).");
    
    return queries;
  }
}