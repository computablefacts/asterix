package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;

import com.computablefacts.asterix.BoxedType;
import com.computablefacts.asterix.codecs.Base64Codec;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.decima.Builder;
import com.computablefacts.logfmt.LogFormatter;
import com.computablefacts.nona.Function;
import com.computablefacts.nona.types.Csv;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
@CheckReturnValue
final public class Functions extends AbstractFunctions {

  private static final Logger logger_ = LoggerFactory.getLogger(Functions.class);

  private final AbstractKnowledgeBase kb_;

  public Functions(AbstractKnowledgeBase kb) {

    super();

    kb_ = Preconditions.checkNotNull(kb, "kb should not be null");

    // [DEPRECATED] Special operator. Allow KB modification at runtime.
    register("ASSERT_JSON", assertJson());

    // [DEPRECATED] Special operator. Allow KB modification at runtime.
    register("ASSERT_CSV", assertCsv());

    // [DEPRECATED] Special operator. See {@link Literal#execute} for details.
    register("EXIST_IN_KB", existInKb());

    // [DEPRECATED] Special operator. Execute a GET HTTP query and use the returned data as new facts.
    register("HTTP_MATERIALIZE_FACTS", httpMaterializeFacts());

    // [DEPRECTED] Ask the solver to materialize each collection's element as a fact.
    register("MATERIALIZE_FACTS", materializeFacts());
  }

  private Function assertJson() {
    return new Function("ASSERT_JSON") {

      @Override
      protected boolean isCacheable() {
        return false; // The function's cache is shared between multiple processes
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() == 2, "ASSERT_JSON takes exactly two parameters.");
        Preconditions.checkArgument(parameters.get(0).isString(), "%s should be a string", parameters.get(0));
        Preconditions.checkArgument(parameters.get(1).isCollection() || parameters.get(1).isMap(),
            "%s should be a json array or object", parameters.get(1));

        String uuid = parameters.get(0).asString();
        List<?> jsons = parameters.get(1).isMap() ? Lists.newArrayList(parameters.get(1).asMap())
            : Lists.newArrayList(parameters.get(1).asCollection());

        for (int i = 0; i < jsons.size(); i++) {

          String json = JsonCodec.asString(jsons.get(i));

          kb_.azzert(Builder.json(uuid, Integer.toString(i, 10), json));
          kb_.azzert(Builder.jsonPaths(uuid, Integer.toString(i, 10), json));
        }
        return BoxedType.create(true);
      }
    };
  }

  private Function assertCsv() {
    return new Function("ASSERT_CSV") {

      @Override
      protected boolean isCacheable() {
        return false; // The function's cache is shared between multiple processes
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() == 2, "ASSERT_CSV takes exactly two parameters.");
        Preconditions.checkArgument(parameters.get(0).isString(), "%s should be a string", parameters.get(0));
        Preconditions.checkArgument(parameters.get(1).value() instanceof Csv, "%s should be a csv array",
            parameters.get(1));

        String uuid = parameters.get(0).asString();
        Csv csvs = (Csv) parameters.get(1).value();

        for (int i = 0; i < csvs.nbRows(); i++) {

          String json = JsonCodec.asString(csvs.row(i));

          kb_.azzert(Builder.json(uuid, Integer.toString(i, 10), json));
          kb_.azzert(Builder.jsonPaths(uuid, Integer.toString(i, 10), json));
        }
        return BoxedType.create(true);
      }
    };
  }

  private Function existInKb() {
    return new Function("EXIST_IN_KB") {

      @Override
      protected boolean isCacheable() {
        return false; // The function's cache is shared between multiple processes
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() > 1, "EXIST_IN_KB takes at least two parameters.");

        String predicate = parameters.get(0).asString();
        List<String> terms = parameters.subList(1, parameters.size()).stream().map(bt -> {

          String term = bt.asString();

          if (bt.isNumber() || bt.isBoolean()) {
            return term;
          }
          return "_".equals(term) ? "_" : "\"" + term + "\"";
        }).collect(Collectors.toList());

        String fact = predicate + "(" + Joiner.on(',').join(terms) + ")?";
        Literal query = Parser.parseQuery(fact);
        Iterator<Fact> facts = kb_.facts(query);

        while (facts.hasNext()) {
          if (facts.next().isFact()) {
            return BoxedType.create(true);
          }
        }
        return BoxedType.create(false);
      }
    };
  }

  /**
   * Example :
   *
   * <pre>
   * fn_http_materialize_facts("https://api.cf.com/api/v0/facts/crm/client", "prenom", _, Prenom,
   *     "nom", _, Nom, "email", _, Email)
   * </pre>
   * <p>
   * Result :
   *
   * <pre>
   *  [
   *    {
   *      "namespace": "crm",
   *      "class": "client",
   *      "facts": [
   *        {
   * 	          "prenom": "Jane"
   * 	          "nom": "Doe"
   * 	  	      "email": "jane.doe@gmail.com"
   *        }, {
   * 	  	     "prenom": "John"
   * 	  	      "nom": "Doe"
   * 	  	      "email": "j.doe@gmail.com"
   *        },
   * 	      ...
   *      ]
   *    },
   *    ...
   *  ]
   * </pre>
   */
  private Function httpMaterializeFacts() {
    return new Function("HTTP_MATERIALIZE_FACTS") {

      @Override
      protected boolean isCacheable() {
        return false; // The function's cache is shared between multiple processes
      }

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() >= 4,
            "HTTP_MATERIALIZE_FACTS_QUERY takes at least four parameters.");
        Preconditions.checkArgument(parameters.get(0).isString(), "%s should be a string", parameters.get(0));

        Base64.Encoder b64Encoder = Base64.getEncoder();
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < parameters.size(); i = i + 3) {

          String name = parameters.get(i).asString();
          String filter = "_".equals(parameters.get(i + 1).asString()) ? "" : parameters.get(i + 1).asString();
          String value = "_".equals(parameters.get(i + 2).asString()) ? "" : parameters.get(i + 2).asString();

          if (builder.length() > 0) {
            builder.append('&');
          }
          builder.append(name).append('=').append(Base64Codec.encodeB64(b64Encoder, value.isEmpty() ? filter : value));
        }

        String httpUrl = parameters.get(0).asString();
        String queryString = builder.toString();

        try {

          URL url = new URL(httpUrl);
          HttpURLConnection con = (HttpURLConnection) url.openConnection();
          con.setRequestMethod("GET");
          con.setInstanceFollowRedirects(true);
          con.setConnectTimeout(5 * 1000);
          con.setReadTimeout(10 * 1000);
          con.setDoOutput(true);

          try (DataOutputStream out = new DataOutputStream(con.getOutputStream())) {
            out.writeBytes(queryString);
          }

          StringBuilder result = new StringBuilder();
          int status = con.getResponseCode();

          if (status > 299) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {

              @com.google.errorprone.annotations.Var String inputLine;

              while ((inputLine = in.readLine()) != null) {
                result.append(inputLine);
              }

              logger_.error(LogFormatter.create().message(result.toString()).formatError());
            }
            return BoxedType.empty();
          }

          try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {

            @Var String inputLine;

            while ((inputLine = in.readLine()) != null) {
              result.append(inputLine);
            }
          }

          con.disconnect();
          List<Literal> facts = new ArrayList<>();
          Map<String, Object>[] jsons = JsonCodec.asArray(result.toString());

          for (Map<String, Object> json : jsons) {

            if (!json.containsKey("namespace")) {
              return BoxedType.empty();
            }
            if (!json.containsKey("class")) {
              return BoxedType.empty();
            }
            if (!json.containsKey("facts")) {
              return BoxedType.empty();
            }

            // String namespace = (String) json.get("namespace");
            // String clazz = (String) json.get("class");

            facts.addAll(((List<Map<String, Object>>) json.get("facts")).stream().map(fact -> {

              List<AbstractTerm> terms = new ArrayList<>();
              terms.add(newConst(parameters.get(0)));

              for (int i = 1; i < parameters.size(); i = i + 3) {
                String name = parameters.get(i).asString();
                String filter = parameters.get(i + 1).asString();
                terms.add(newConst(name));
                terms.add(newConst(filter));
                terms.add(newConst(fact.get(name)));
              }
              return new Literal("fn_" + name().toLowerCase(), terms);
            }).collect(Collectors.toList()));
          }
          return BoxedType.create(facts);
        } catch (IOException e) {
          logger_.error(LogFormatter.create().message(e).formatError());
          // fall through
        }
        return BoxedType.empty();
      }
    };
  }

  /**
   * Ask the solver to materialize each collection's element as a fact.
   *
   * <pre>
   *     fn_materialize_facts(b64_(...), _).
   * </pre>
   */
  private Function materializeFacts() {
    return new Function("MATERIALIZE_FACTS") {

      @Override
      public BoxedType<?> evaluate(List<BoxedType<?>> parameters) {

        Preconditions.checkArgument(parameters.size() == 2, "MATERIALIZE_FACTS takes exactly two parameters.");
        Preconditions.checkArgument(parameters.get(0).isString(), "%s must be a string", parameters.get(0));
        Preconditions.checkArgument(parameters.get(1).isString(), "%s must be a string", parameters.get(1));

        String predicate = "fn_" + name().toLowerCase();
        Collection<Literal> newCollection = new ArrayList<>();
        Object[] oldCollection = Strings.isNullOrEmpty(parameters.get(0).asString()) ? new Map[0]
            : JsonCodec.asArrayOfUnknownType(parameters.get(0).asString());
        String filter = parameters.get(1).asString();

        for (Object obj : oldCollection) {

          List<AbstractTerm> terms = new ArrayList<>();
          terms.add(newConst(parameters.get(0).asString()));
          terms.add(newConst(obj));

          if ("_".equals(filter)) {
            newCollection.add(new Literal(predicate, terms));
          } else if (filter.equals(terms.get(1).toString())) {
            newCollection.add(new Literal(predicate, terms));
          }
        }
        return newCollection.isEmpty() ? BoxedType.empty() : BoxedType.create(newCollection);
      }
    };
  }
}
