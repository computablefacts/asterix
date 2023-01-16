package com.computablefacts.decima.problog;

import com.computablefacts.nona.Function;
import com.computablefacts.nona.functions.comparisonoperators.Equal;
import com.computablefacts.nona.functions.csvoperators.CsvValue;
import com.computablefacts.nona.functions.stringoperators.StrLength;
import com.computablefacts.nona.functions.stringoperators.ToInteger;
import com.computablefacts.nona.functions.stringoperators.ToLowerCase;
import com.computablefacts.nona.functions.stringoperators.ToUpperCase;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CheckReturnValue
public class AbstractFunctions {

  private final Map<String, Function> definitions_ = new ConcurrentHashMap<>();

  public AbstractFunctions() {

    Map<String, Function> defs = Function.definitions();

    for (Map.Entry<String, Function> def : defs.entrySet()) {
      definitions_.put("FN_" + def.getKey(), def.getValue());
    }

    // TODO : legacy functions. Remove ASAP.
    definitions_.put("FN_EQ", new Equal());
    definitions_.put("FN_CSV_VALUE", new CsvValue());
    definitions_.put("FN_LOWER_CASE", new ToLowerCase());
    definitions_.put("FN_UPPER_CASE", new ToUpperCase());
    definitions_.put("FN_INT", new ToInteger());
    definitions_.put("FN_LENGTH", new StrLength());
  }

  /**
   * Return the list of available definitions for primitives.
   *
   * @return list of definitions.
   */
  public Map<String, Function> definitions() {
    return ImmutableMap.copyOf(definitions_);
  }

  /**
   * Register a new primitive.
   *
   * @param name     the primitive name.
   * @param function the primitive implementation.
   */
  public void register(String name, Function function) {

    Preconditions.checkNotNull(name, "name should not be null");
    Preconditions.checkNotNull(function, "function should not be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "function name should neither be null nor empty");
    Preconditions.checkArgument(!name.trim().toUpperCase().startsWith("FN_"),
        "function name should not start with 'fn_'");
    Preconditions.checkState(!definitions_.containsKey("FN_" + name.trim().toUpperCase()),
        "a function with the same name already exists");

    definitions_.put("FN_" + name.trim().toUpperCase(), function);
  }

  /**
   * Unregister an existing primitive.
   *
   * @param name the primitive name.
   */
  public void unregister(String name) {

    Preconditions.checkNotNull(name, "name should not be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "function name should neither be null nor empty");
    Preconditions.checkArgument(!name.trim().toUpperCase().startsWith("FN_"),
        "function name should not start with 'fn_'");
    Preconditions.checkState(definitions_.containsKey("FN_" + name.trim().toUpperCase()),
        "a function with the given name cannot be found");

    definitions_.remove("FN_" + name.trim().toUpperCase());
  }
}
