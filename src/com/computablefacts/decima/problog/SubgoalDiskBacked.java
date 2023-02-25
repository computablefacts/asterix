package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.Parser.parseClause;

import com.computablefacts.asterix.BloomFilter;
import com.computablefacts.asterix.RandomString;
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
final public class SubgoalDiskBacked extends AbstractSubgoal {

  private static final char SEPARATOR = '¤';
  private static final double FALSE_POSITIVE_PROBABILITY = 0.05;
  private static final int EXPECTED_NUMBER_OF_ELEMENTS = 10_000_000;
  private static final Logger logger_ = LoggerFactory.getLogger(SubgoalDiskBacked.class);

  // B+-Tree
  private final BPlusTree<Integer, String> facts_;
  private final BPlusTree<Integer, String> proofs_;
  private final Consumer<Fact> peek_;
  private final BloomFilter<String> bf_; // by default, around ~72MB in size

  // Metrics
  private int nbFacts_ = 0;
  private int nbProofs_ = 0;

  public SubgoalDiskBacked(Literal literal, String directory) {
    this(literal, directory, null, FALSE_POSITIVE_PROBABILITY, EXPECTED_NUMBER_OF_ELEMENTS);
  }

  public SubgoalDiskBacked(Literal literal, String directory, Consumer<Fact> peek) {
    this(literal, directory, peek, FALSE_POSITIVE_PROBABILITY, EXPECTED_NUMBER_OF_ELEMENTS);
  }

  public SubgoalDiskBacked(Literal literal, String directory, Consumer<Fact> peek, double falsePositiveProbability,
      int expectedNumberOfElements) {

    super(literal);

    RandomString randomString = new RandomString(10);
    String uuid = randomString.nextString();
    File dirFacts = new File(String.format("%s%s%s_facts", directory, File.separator, uuid));

    if (!dirFacts.exists()) {
      dirFacts.mkdirs();
    }

    File dirProofs = new File(String.format("%s%s%s_proofs", directory, File.separator, uuid));

    if (!dirProofs.exists()) {
      dirProofs.mkdirs();
    }

    peek_ = peek;
    facts_ = BPlusTree.file().directory(dirFacts.getAbsolutePath()).deleteOnClose().maxLeafKeys(32).maxNonLeafKeys(8)
        .segmentSizeMB(1).uniqueKeys(false).keySerializer(Serializer.INTEGER).valueSerializer(Serializer.utf8())
        .naturalOrder();
    proofs_ = BPlusTree.file().directory(dirProofs.getAbsolutePath()).deleteOnClose().maxLeafKeys(32).maxNonLeafKeys(8)
        .segmentSizeMB(1).uniqueKeys(false).keySerializer(Serializer.INTEGER).valueSerializer(Serializer.utf8())
        .naturalOrder();
    bf_ = new BloomFilter<>(falsePositiveProbability, expectedNumberOfElements);
  }

  @Override
  protected void finalize() {
    if (facts_ != null) {
      try {
        facts_.close();
      } catch (Exception e) {
        logger_.error(LogFormatter.create().message(e).formatError());
      }
    }
    if (proofs_ != null) {
      try {
        proofs_.close();
      } catch (Exception e) {
        logger_.error(LogFormatter.create().message(e).formatError());
      }
    }
  }

  @Override
  public void fact(Fact fact) {

    Preconditions.checkNotNull(fact, "fact should not be null");

    String cacheKey = cacheKey(fact);
    bf_.add(cacheKey);
    facts_.insert(cacheKey.hashCode(), cacheKey + SEPARATOR + fact.toString());
    nbFacts_++;

    if (peek_ != null) {
      peek_.accept(fact);
    }
  }

  @Override
  public Iterator<Fact> facts() {
    return View.of(facts_.findAll()).map(value -> (Fact) parseClause(value.substring(value.indexOf(SEPARATOR) + 1)));
  }

  @Override
  public boolean contains(Fact fact) {

    Preconditions.checkNotNull(fact, "fact should not be null");

    String cacheKey = cacheKey(fact);
    return bf_.contains(cacheKey) && View.of(facts_.find(cacheKey.hashCode()))
        .contains(value -> value.startsWith(cacheKey + SEPARATOR));
  }

  @Override
  public int nbFacts() {
    return nbFacts_;
  }

  @Override
  public void proof(Rule proof) {

    Preconditions.checkNotNull(proof, "proof should not be null");
    Preconditions.checkArgument(proof.isGrounded(), "proof should be grounded : %s", proof);

    String cacheKey = cacheKey(proof);
    proofs_.insert(cacheKey.hashCode(), cacheKey + SEPARATOR + proof.toString());
    nbProofs_++;
  }

  @Override
  public Iterator<Rule> proofs() {
    return View.of(proofs_.findAll()).map(value -> (Rule) parseClause(value.substring(value.indexOf(SEPARATOR) + 1)));
  }

  @Override
  public int nbProofs() {
    return nbProofs_;
  }

  private String cacheKey(Fact fact) {
    return fact.head().id();
  }

  private String cacheKey(Rule proof) {
    return View.of(proof.head()).concat(proof.body()).join(Literal::id, "");
  }
}