package com.computablefacts.asterix.queries;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.WildcardMatcher;
import com.computablefacts.asterix.codecs.StringCodec;
import com.google.common.collect.Lists;

public class WildcardQueryEngineTest {

  @Test
  public void testExactMatch() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node = QueryBuilder.build("mary");

    Assert.assertEquals(2, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Morita", "Mary Short"),
        node.execute(engine).toList());
  }

  @Test
  public void testPrefixMatch() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node = QueryBuilder.build("mar*");

    Assert.assertEquals(3, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Morita", "Mary Short", "Summer Martinez"),
        node.execute(engine).toList());
  }

  @Test
  public void testSuffixMatch() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node = QueryBuilder.build("*ell");

    Assert.assertEquals(2, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Edward Bell", "Paul Powell"),
        node.execute(engine).toList());
  }

  @Test
  public void testInflectionalQuery() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node = QueryBuilder.build("mary mo*");

    Assert.assertEquals(2, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Morita"), node.execute(engine).toList());
  }

  @Test
  public void testInflectionalQueryAnd() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node1 = QueryBuilder.build("mary *or* AND *ita");

    Assert.assertEquals(1, node1.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Morita"), node1.execute(engine).toList());

    AbstractNode<WildcardQueryEngine> node = QueryBuilder.build("mary *or* AND *ort");

    Assert.assertEquals(1, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Short"), node.execute(engine).toList());
  }

  @Test
  public void testInflectionalQueryOr() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node1 = QueryBuilder.build("mary *or* OR *ita");

    Assert.assertEquals(3, node1.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Morita", "Mary Short"),
        node1.execute(engine).toList());

    AbstractNode<WildcardQueryEngine> node = QueryBuilder.build("mary *or* OR *ort");

    Assert.assertEquals(3, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Morita", "Mary Short"),
        node.execute(engine).toList());
  }

  @Test
  public void testLiteralQuery() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node = QueryBuilder.build("\"mary mo*\"");

    Assert.assertEquals(1, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Morita"), node.execute(engine).toList());
  }

  @Test
  public void testLiteralQueryAnd() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node1 = QueryBuilder.build("\"mary *or*\" AND *ita");

    Assert.assertEquals(1, node1.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Morita"), node1.execute(engine).toList());

    AbstractNode<WildcardQueryEngine> node2 = QueryBuilder.build("\"mary *or*\" AND *ort");

    Assert.assertEquals(1, node2.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Short"), node2.execute(engine).toList());
  }

  @Test
  public void testLiteralQueryOr() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node1 = QueryBuilder.build("\"mary *or*\" OR *ita");

    Assert.assertEquals(3, node1.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Morita", "Mary Short"),
        node1.execute(engine).toList());

    AbstractNode<WildcardQueryEngine> node2 = QueryBuilder.build("\"mary *or*\" OR *ort");

    Assert.assertEquals(3, node2.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Mary Morita", "Mary Short"),
        node2.execute(engine).toList());
  }

  private static class WildcardQueryEngine extends AbstractQueryEngine {

    @Override
    public long inflectionalCardinality(String key, String value) {
      return inflectionalQuery(key, value).toList().size();
    }

    @Override
    public View<String> inflectionalQuery(String key, String value) {

      List<String> patterns = StringCodec.defaultTokenizer(value).stream().map(Span::text)
          .map(t -> WildcardMatcher.compact("*" + t + "*")).collect(Collectors.toList());

      return View.of(executeQuery(key, patterns));
    }

    @Override
    public long literalCardinality(String key, String value) {
      return literalQuery(key, value).toList().size();
    }

    @Override
    public View<String> literalQuery(String key, String value) {

      String pattern = WildcardMatcher.compact(StringCodec.defaultTokenizer(value).stream()
          .map(Span::text).collect(Collectors.joining("*")) + "*");

      return View.of(executeQuery(key, Lists.newArrayList(pattern)));
    }

    public List<String> executeQuery(String field, List<String> patterns) {
      return persons().stream()
          .filter(name -> patterns.stream().anyMatch(p -> WildcardMatcher.match(name, p)))
          .collect(Collectors.toList());
    }

    private List<String> persons() {
      return Lists.newArrayList("Delores Hardy", "Edward Bell", "Gregory Blackwood",
          "Keith Franklin", "Mary Morita", "Mary Short", "Michele Moore", "Paul Powell",
          "Robert Frye", "Summer Martinez");
    }
  }
}
