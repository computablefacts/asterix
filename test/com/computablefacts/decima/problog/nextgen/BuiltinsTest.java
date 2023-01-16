package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuiltinsTest extends AbstractTest {

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("q_001(X) :- X = \"true\".");
    rules.add("q_002(X) :- X != \"true\".");
    rules.add("q_003(X) :- X <> \"true\".");
    rules.add("q_004(X) :- X < 10.");
    rules.add("q_005(X) :- X > 10.");
    rules.add("q_006(X) :- X <= 10.");
    rules.add("q_007(X) :- X >= 10.");
    rules.add("q_008(X) :- X is \"OK\".");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();

    queries.put("q_001(true)?", "q_001(true).");
    queries.put("q_001(false)?", null);

    queries.put("q_002(true)?", null);
    queries.put("q_002(false)?", "q_002(false).");

    queries.put("q_003(true)?", null);
    queries.put("q_003(false)?", "q_003(false).");

    queries.put("q_004(5)?", "q_004(5).");
    queries.put("q_004(10)?", null);
    queries.put("q_004(15)?", null);

    queries.put("q_005(5)?", null);
    queries.put("q_005(10)?", null);
    queries.put("q_005(15)?", "q_005(15).");

    queries.put("q_006(5)?", "q_006(5).");
    queries.put("q_006(10)?", "q_006(10).");
    queries.put("q_006(15)?", null);

    queries.put("q_007(5)?", null);
    queries.put("q_007(10)?", "q_007(10).");
    queries.put("q_007(15)?", "q_007(15).");

    queries.put("q_008(\"OK\")?", "q_008(\"OK\").");
    queries.put("q_008(\"KO\")?", null);

    return queries;
  }
}