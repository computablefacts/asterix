package com.computablefacts.asterix.queries;

import com.computablefacts.asterix.View;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class RangeQueryEngineTest {

  @Test
  public void testRangeInfValQuery() {

    RangeQueryEngine engine = new RangeQueryEngine();
    AbstractNode<RangeQueryEngine> node = QueryBuilder.build("[* TO 3]");

    Assert.assertEquals(4, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("0", "1", "2", "3"), node.execute(engine).toList());
  }

  @Test
  public void testRangeValInfQuery() {

    RangeQueryEngine engine = new RangeQueryEngine();
    AbstractNode<RangeQueryEngine> node = QueryBuilder.build("[6 TO *]");

    Assert.assertEquals(4, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("6", "7", "8", "9"), node.execute(engine).toList());
  }

  @Test
  public void testRangeValValQuery() {

    RangeQueryEngine engine = new RangeQueryEngine();
    AbstractNode<RangeQueryEngine> node = QueryBuilder.build("[3 TO 6]");

    Assert.assertEquals(4, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("3", "4", "5", "6"), node.execute(engine).toList());
  }

  @Test
  public void testRangeQueryAnd() {

    RangeQueryEngine engine = new RangeQueryEngine();
    AbstractNode<RangeQueryEngine> node1 = QueryBuilder.build("[3 TO 6] AND odd:[3 TO 6]");

    Assert.assertEquals(2, node1.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("3", "5"), node1.execute(engine).toList());

    AbstractNode<RangeQueryEngine> node2 = QueryBuilder.build("[3 TO 6] AND even:[3 TO 6]");

    Assert.assertEquals(2, node2.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("4", "6"), node2.execute(engine).toList());
  }

  @Test
  public void testRangeQueryOr() {

    RangeQueryEngine engine = new RangeQueryEngine();
    AbstractNode<RangeQueryEngine> node1 = QueryBuilder.build("[3 TO 6] OR odd:[3 TO 6]");

    Assert.assertEquals(6, node1.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("3", "4", "5", "6"), node1.execute(engine).toList());

    AbstractNode<RangeQueryEngine> node2 = QueryBuilder.build("[3 TO 6] OR even:[3 TO 6]");

    Assert.assertEquals(6, node2.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("3", "4", "5", "6"), node2.execute(engine).toList());
  }

  @Test
  public void testRangeQueryAndNot() {

    RangeQueryEngine engine = new RangeQueryEngine();
    AbstractNode<RangeQueryEngine> node1 = QueryBuilder.build("[3 TO 6] AND NOT odd:[4 TO 5]");

    Assert.assertEquals(4, node1.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("3", "4", "6"), node1.execute(engine).toList());

    AbstractNode<RangeQueryEngine> node2 = QueryBuilder.build("[3 TO 6] AND NOT even:[4 TO 5]");

    Assert.assertEquals(4, node2.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("3", "5", "6"), node2.execute(engine).toList());
  }

  @Test
  public void testRangeQueryNotAnd() {

    RangeQueryEngine engine = new RangeQueryEngine();
    AbstractNode<RangeQueryEngine> node1 = QueryBuilder.build("NOT [4 TO 5] AND odd:[3 TO 6]");

    Assert.assertEquals(2, node1.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("3"), node1.execute(engine).toList());

    AbstractNode<RangeQueryEngine> node2 = QueryBuilder.build("NOT [4 TO 5] AND even:[3 TO 6]");

    Assert.assertEquals(2, node2.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("6"), node2.execute(engine).toList());
  }

  @Test
  public void testRangeQueryNotAndNot() {

    AbstractNode<RangeQueryEngine> node = QueryBuilder.build("NOT [3 TO 6] AND NOT odd:[4 TO 5]");

    Assert.assertNull(node);
  }

  @Test
  public void testInvalidRangeQueryOrNot() {

    RangeQueryEngine engine = new RangeQueryEngine();
    AbstractNode<RangeQueryEngine> node1 = QueryBuilder.build("[3 TO 6] OR NOT odd:[3 TO 6]");

    Assert.assertEquals(4, node1.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList(), node1.execute(engine).toList());

    AbstractNode<RangeQueryEngine> node2 = QueryBuilder.build("[3 TO 6] OR NOT even:[3 TO 6]");

    Assert.assertEquals(4, node2.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList(), node2.execute(engine).toList());
  }

  @Test
  public void testInvalidRangeQueryNotOr() {

    RangeQueryEngine engine = new RangeQueryEngine();
    AbstractNode<RangeQueryEngine> node1 = QueryBuilder.build("NOT [3 TO 6] OR odd:[3 TO 6]");

    Assert.assertEquals(2, node1.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList(), node1.execute(engine).toList());

    AbstractNode<RangeQueryEngine> node2 = QueryBuilder.build("NOT [3 TO 6] OR even:[3 TO 6]");

    Assert.assertEquals(2, node2.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList(), node2.execute(engine).toList());
  }

  @Test
  public void testInvalidRangeQueryNotOrNot() {

    AbstractNode<RangeQueryEngine> node = QueryBuilder.build("NOT [3 TO 6] OR NOT odd:[3 TO 6]");

    Assert.assertNull(node);
  }

  @Test
  public void testInvalidRangeQueryNot() {

    AbstractNode<RangeQueryEngine> node = QueryBuilder.build("NOT [3 TO 6]");

    Assert.assertNull(node);
  }

  @Test
  public void testRangeQueryOverlapping() {

    RangeQueryEngine engine = new RangeQueryEngine();
    AbstractNode<RangeQueryEngine> node = QueryBuilder.build("[3 TO 6] AND [4 TO 8]");

    Assert.assertEquals(4, node.cardinality(engine));
    Assert.assertEquals(Lists.newArrayList("4", "5", "6"), node.execute(engine).toList());
  }

  private static class RangeQueryEngine extends AbstractQueryEngine {

    @Override
    public View<String> executeQuery(String field, String term) {
      return View.of();
    }

    @Override
    public View<String> executeQuery(String field, Number min, Number max) {
      if ("even".equals(field)) {
        return View.of(even()).filter(i -> min == null || min.intValue() <= i)
            .filter(i -> max == null || max.intValue() >= i).map(i -> Integer.toString(i, 10));
      }
      if ("odd".equals(field)) {
        return View.of(odd()).filter(i -> min == null || min.intValue() <= i)
            .filter(i -> max == null || max.intValue() >= i).map(i -> Integer.toString(i, 10));
      }
      return View.of(evenAndOdd()).filter(i -> min == null || min.intValue() <= i)
          .filter(i -> max == null || max.intValue() >= i).map(i -> Integer.toString(i, 10));
    }

    private List<Integer> odd() {
      return Lists.newArrayList(1, 3, 5, 7, 9);
    }

    private List<Integer> even() {
      return Lists.newArrayList(0, 2, 4, 6, 8);
    }

    private List<Integer> evenAndOdd() {
      return Lists.newArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }
  }
}