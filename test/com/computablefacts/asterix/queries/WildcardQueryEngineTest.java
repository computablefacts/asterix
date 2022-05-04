package com.computablefacts.asterix.queries;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.asterix.View;
import com.computablefacts.asterix.WildcardMatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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

    Assert.assertEquals(5, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Marie Delacroix", "Marie Moreau", "Mary Morita",
        "Mary Short", "Summer Martinez"), node.execute(engine).toList());
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

  @Test
  public void testThesaurusQuery() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node = QueryBuilder.build("~marie");

    Assert.assertEquals(4, node.cardinality(engine));
    Assert.assertEquals(
        Lists.newArrayList("Marie Delacroix", "Marie Moreau", "Mary Morita", "Mary Short"),
        node.execute(engine).toList());
  }

  @Test
  public void testThesaurusQueryAnd() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node = QueryBuilder.build("~marie AND mor*");

    Assert.assertEquals(2, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Marie Moreau", "Mary Morita"),
        node.execute(engine).toList());
  }

  @Test
  public void testThesaurusQueryAndNot() {

    WildcardQueryEngine engine = new WildcardQueryEngine();
    AbstractNode<WildcardQueryEngine> node = QueryBuilder.build("~marie AND -mor*");

    Assert.assertEquals(4, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("Marie Delacroix", "Mary Short"),
        node.execute(engine).toList());
  }

  private static class WildcardQueryEngine extends AbstractQueryEngine {

    @Override
    protected Set<String> map(String term) {
      if ("mary".equals(term) || "marie".equals(term)) {
        return Sets.newHashSet("mary", "marie");
      }
      return Sets.newHashSet(term);
    }

    @Override
    public View<String> executeQuery(String field, Number min, Number max) {
      return View.of();
    }

    @Override
    public View<String> executeQuery(String field, String term) {
      String newTerm = WildcardMatcher.compact("*" + term + "*");
      return View.of(persons()).filter(name -> WildcardMatcher.match(name, newTerm));
    }

    private List<String> persons() {
      return Lists.newArrayList("Delores Hardy", "Edward Bell", "Gregory Blackwood",
          "Keith Franklin", "Marie Delacroix", "Marie Moreau", "Mary Morita", "Mary Short",
          "Michele Moore", "Paul Powell", "Robert Frye", "Summer Martinez");
    }
  }
}
