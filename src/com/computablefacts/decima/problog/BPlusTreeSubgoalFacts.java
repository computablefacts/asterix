package com.computablefacts.decima.problog;

import com.computablefacts.asterix.View;
import com.computablefacts.logfmt.LogFormatter;
import com.github.davidmoten.bplustree.BPlusTree;
import com.github.davidmoten.bplustree.Serializer;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.File;
import java.util.Iterator;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CheckReturnValue
final public class BPlusTreeSubgoalFacts extends AbstractSubgoalFacts {

  private static final Logger logger_ = LoggerFactory.getLogger(BPlusTreeSubgoalFacts.class);
  private static final char SEPARATOR = '¤';

  // B+-Tree
  private final BPlusTree<Integer, String> tree_;

  // Metrics
  private final Consumer<Literal> peek_;
  private int nbFacts_ = 0;

  public BPlusTreeSubgoalFacts(String directory, String tableName, int subgoalId) {
    this(directory, tableName, subgoalId, null);
  }

  public BPlusTreeSubgoalFacts(String directory, String tableName, int subgoalId, Consumer<Literal> peek) {

    File dir = new File(String.format("%s%s%s_%d", directory, File.separator, tableName, subgoalId));

    if (!dir.exists()) {
      dir.mkdirs();
    }

    peek_ = peek;
    tree_ = BPlusTree.file().directory(dir.getAbsolutePath()).deleteOnClose().maxLeafKeys(32).maxNonLeafKeys(8)
        .segmentSizeMB(1).uniqueKeys(false).keySerializer(Serializer.INTEGER).valueSerializer(Serializer.utf8())
        .naturalOrder();
  }

  @Override
  protected void finalize() {
    if (tree_ != null) {
      try {
        tree_.close();
      } catch (Exception e) {
        logger_.error(LogFormatter.create(true).message(e).formatError());
      }
    }
  }

  @Override
  public boolean contains(Fact fact) {
    String cacheKey = cacheKey(fact);
    return View.of(tree_.find(cacheKey.hashCode())).contains(value -> value.startsWith(cacheKey + SEPARATOR));
  }

  @Override
  public Iterator<Fact> facts() {
    return View.of(tree_.findAll())
        .map(value -> (Fact) Parser.parseClause(value.substring(value.indexOf(SEPARATOR) + 1)));
  }

  @Override
  public int size() {
    return nbFacts_;
  }

  @Override
  public void add(Fact fact) {

    String cacheKey = cacheKey(fact);
    tree_.insert(cacheKey.hashCode(), cacheKey + SEPARATOR + fact.toString() + ".");
    nbFacts_++;

    if (peek_ != null) {
      peek_.accept(fact.head());
    }
  }

  private String cacheKey(Fact fact) {

    Preconditions.checkNotNull(fact, "fact should not be null");

    return fact.head().id();
  }
}
