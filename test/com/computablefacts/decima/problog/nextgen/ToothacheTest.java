package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToothacheTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.10::cavity(a).");
    facts.add("0.05::gum_disease(a).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("1.00::toothache(X) :- cavity(X), gum_disease(X).");
    rules.add("0.60::toothache(X) :- cavity(X), ~gum_disease(X).");
    rules.add("0.30::toothache(X) :- ~cavity(X), gum_disease(X).");
    rules.add("0.05::toothache(X) :- ~cavity(X), ~gum_disease(X).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("toothache(a)?", "0.11825::toothache(a).");

    return queries;
  }
}