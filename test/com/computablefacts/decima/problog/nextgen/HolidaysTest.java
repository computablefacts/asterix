package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/12_holidays.pl
 */
public class HolidaysTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("person(alice).");
    facts.add("destinations(seaside, mountains, city).");
    facts.add("destinations(mountains, seaside, city).");
    facts.add("destinations(city, seaside, mountains).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();

    rules.add("0.4::goes_to(P, seaside, 0) :- person(P).");
    rules.add("0.3::goes_to(P, mountains, 0) :- person(P).");
    rules.add("0.3::goes_to(P, city, 0) :- person(P).");
    rules.add("0.7::goes_to(X, D1, T) :- T > 0, fn_sub(TPrev, T, 1), destinations(D1, D2, D3), goes_to(X, D1, TPrev).");
    rules.add(
        "0.15::goes_to(X, D2, T) :- T > 0, fn_sub(TPrev, T, 1), destinations(D1, D2, D3), goes_to(X, D1, TPrev).");
    rules.add(
        "0.15::goes_to(X, D3, T) :- T > 0, fn_sub(TPrev, T, 1), destinations(D1, D2, D3), goes_to(X, D1, TPrev).");

    rules.add("0.4::gt(P, seaside, 0) :- person(P).");
    rules.add("0.3::gt(P, mountains, 0) :- person(P).");
    rules.add("0.3::gt(P, city, 0) :- person(P).");
    rules.add("0.7::gt(X, D1, T) :- T > 0, fn_sub(TPrev, T, 1), gt(X, D1, TPrev), destinations(D1, D2, D3).");
    rules.add("0.15::gt(X, D2, T) :- T > 0, fn_sub(TPrev, T, 1), gt(X, D1, TPrev), destinations(D1, D2, D3).");
    rules.add("0.15::gt(X, D3, T) :- T > 0, fn_sub(TPrev, T, 1), gt(X, D1, TPrev), destinations(D1, D2, D3).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();

    queries.put("goes_to(alice, seaside, 1)?", "0.34334::goes_to(alice, seaside, 1).");
    queries.put("goes_to(alice, mountains, 1)?", "0.29082::goes_to(alice, mountains, 1).");
    queries.put("goes_to(alice, city, 1)?", "0.29082::goes_to(alice, city, 1).");

    queries.put("gt(alice, seaside, 1)?", "0.34334::gt(alice, seaside, 1).");
    queries.put("gt(alice, mountains, 1)?", "0.29082::gt(alice, mountains, 1).");
    queries.put("gt(alice, city, 1)?", "0.29082::gt(alice, city, 1).");

    return queries;
  }
}