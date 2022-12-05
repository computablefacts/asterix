package com.computablefacts.asterix.queries;

import com.computablefacts.asterix.nlp.StringIterator;
import com.computablefacts.asterix.nlp.WildcardMatcher;
import com.google.common.base.Splitter;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A simple query parser for wildcard and range queries.
 * <p>
 * The syntax for wildcard queries is:
 *
 * <ul>
 * <li>{@code name:zorglub} the field {@code name} must contain the word {@code zorglub}</li>
 * <li>{@code name:zor*} the field {@code name} must contain a word starting with {@code zor}</li>
 * <li>{@code name:*glub} the field {@code name} must contain a word ending with {@code glub}</li>
 * </ul>
 * <p>
 * The syntax for range queries is:
 *
 * <ul>
 * <li>{@code age:[* TO y]} the field {@code age} must contain a value in {@code -inf} and
 * {@code y}</li>
 * <li>{@code age:[x TO *]} the field {@code age} must contain a value in {@code x} and
 * {@code +inf}</li>
 * <li>{@code age:[x TO y]} the field {@code age} must contain a value in {@code x} and
 * {@code y}</li>
 * </ul>
 * <p>
 * Note that:
 *
 * <ul>
 * <li>If the field is omitted, the value is searched in all fields ;</li>
 * <li>Clauses are grouped from left to right i.e. {@code A && B || C} &gt;=&lt;
 * {@code (A && B) || C}.</li>
 * </ul>
 * <p>
 * Heavily based on
 * {@link http://www.blackbeltcoder.com/Articles/data/easy-full-text-search-queries}.
 */
@CheckReturnValue
final public class QueryBuilder<T extends AbstractQueryEngine> {

  public static final String _TO_ = " TO ";

  // TODO : add $ to punctuation ?
  // Keep * and ? because they are considered wildcards
  private final String PUNCTUATION = "~'\"`!@#%^&()-+=[]{}\\|;:,.<>/";
  private final Set<String> STOPWORDS = new HashSet<>();
  private final boolean keepPunctuationMarks_;

  private QueryBuilder(boolean keepPunctuationMarks) {
    keepPunctuationMarks_ = keepPunctuationMarks;
  }

  /**
   * Parses a query and converts it to a tree. Discard all punctuation marks
   *
   * @param query the query to parse.
   * @param <T>   the type of the {@link AbstractQueryEngine}.
   * @return the root node of the tree.
   */
  public static <T extends AbstractQueryEngine> AbstractNode<T> build(String query) {
    return build(query, false);
  }

  /**
   * Parses a query and converts it to a tree.
   *
   * @param query                the query to parse.
   * @param keepPunctuationMarks true iif the punctuation marks must be kept, false otherwise.
   * @param <T>                  the type of the {@link AbstractQueryEngine}.
   * @return the root node of the tree.
   */
  public static <T extends AbstractQueryEngine> AbstractNode<T> build(String query, boolean keepPunctuationMarks) {
    QueryBuilder<T> queryBuilder = new QueryBuilder<>(keepPunctuationMarks);
    AbstractNode<T> root = queryBuilder.parse(query, InternalNode.eConjunctionTypes.And);
    return queryBuilder.fixUpTree(root, true);
  }

  /**
   * Parses a query segment and converts it to an expression tree.
   *
   * @param query              Query segment to convert.
   * @param defaultConjunction Implicit conjunction type.
   * @return Root node of expression tree.
   */
  public AbstractNode<T> parse(String query, InternalNode.eConjunctionTypes defaultConjunction) {

    @Var String predicate = "";
    @Var TerminalNode.eTermForms form = TerminalNode.eTermForms.Inflectional;
    @Var boolean exclude = false;
    @Var InternalNode.eConjunctionTypes conjunction = defaultConjunction;
    @Var boolean resetState = true;
    @Var AbstractNode<T> root = null;
    @Var AbstractNode<T> node;
    @Var String object;

    StringIterator iterator = new StringIterator(query);

    while (!iterator.isEndOfText()) {

      if (resetState) {
        predicate = "";
        form = TerminalNode.eTermForms.Inflectional;
        exclude = false;
        conjunction = defaultConjunction;
        resetState = false;
      }

      iterator.movePastWhitespace();

      if (!iterator.isEndOfText() && (keepPunctuationMarks_ || PUNCTUATION.indexOf(iterator.peek()) < 0)) {

        // Extract query term
        int start = iterator.position();
        iterator.moveAhead();

        List<Integer> arrays = new ArrayList<>();

        while (!iterator.isEndOfText() && (keepPunctuationMarks_ || PUNCTUATION.indexOf(iterator.peek()) < 0
            || "[]".indexOf(iterator.peek()) >= 0 /* array index */) && !Character.isWhitespace(iterator.peek())) {

          if (iterator.peek() == '[' || iterator.peek() == ']') {
            arrays.add(iterator.position());
          }
          iterator.moveAhead();
        }

        // Ensure we are dealing with a predicate if arrays were found
        if (!arrays.isEmpty() && iterator.peek() != ':') {

          iterator.reset(query);

          while (!iterator.isEndOfText() && iterator.position() < arrays.get(0)) {
            char c = iterator.next();
          }
        }

        // Allow trailing wildcards
        // while (!iterator.isEndOfText() && (iterator.peek() == '*' || iterator.peek() == '?')) {
        // iterator.moveAhead();
        // form = TerminalNode.eTermForms.Inflectional;
        // }

        // Interpret token
        object = iterator.extract(start, iterator.position()).toLowerCase();

        if (object.equals("and")) {
          conjunction = InternalNode.eConjunctionTypes.And;
        } else if (object.equals("or")) {
          conjunction = InternalNode.eConjunctionTypes.Or;
        } else if (object.equals("not")) {
          exclude = true;
        } else if (iterator.peek() == ':') {
          predicate = iterator.extract(start, iterator.position());
          object = "";
        } else if (iterator.peek() == '[') {
          predicate = iterator.extract(start, iterator.position());
          resetState = true;
        } else {
          root = addNode(root, iterator.extract(start, iterator.position()), predicate, form, exclude, conjunction);
          resetState = true;
        }
        continue; // Skip iterator.moveAhead()
      }

      if (iterator.peek() == '"') {

        // Extract quoted term
        form = TerminalNode.eTermForms.Literal;
        object = extractQuote(iterator);
        root = addNode(root, object.trim(), predicate, form, exclude, conjunction);
        resetState = true;
      } else if (iterator.peek() == '(') {

        // Parse parentheses block -> group
        object = extractBlock(iterator, '(', ')');
        node = parse(object, defaultConjunction);
        node.exclude(exclude);
        root = addNode(root, node, conjunction);
        resetState = true;
      } else if (iterator.peek() == '[') {

        // Parse bracket block -> range
        object = extractBlock(iterator, '[', ']');

        if (WildcardMatcher.match(object, "*" + _TO_ + "*")) {

          List<String> range = Splitter.on(_TO_).trimResults().omitEmptyStrings().splitToList(object);

          if (range.size() == 2) {

            String min = range.get(0);
            String max = range.get(1);

            boolean isValid =
                ("*".equals(min) && !"*".equals(max)) || ("*".equals(max) && !"*".equals(min)) || (!"*".equals(min)
                    && !"*".equals(max));

            if (isValid) {

              // Extract range
              form = TerminalNode.eTermForms.Range;
              root = addNode(root, object.trim(), predicate, form, exclude, conjunction);
            }
          }
        }
        resetState = true;
      } else if (iterator.peek() == '-') {

        // Match when next term is not present
        exclude = true;
      } else if (iterator.peek() == '+') {

        // Match next term exactly
        form = TerminalNode.eTermForms.Literal;
      } else if (iterator.peek() == '~') {

        // Match synonyms of next term
        form = TerminalNode.eTermForms.Thesaurus;
      }

      // Advance to next character
      iterator.moveAhead();
    }
    return root;
  }

  /**
   * Fixes any portions of the expression tree that would produce an invalid query.
   *
   * <p>
   * While our expression tree may be properly constructed, it may represent a query that is not supported by our
   * backend. This method traverses the expression tree and corrects problem expressions as described below.
   * </p>
   *
   * <ul>
   * <li>NOT term1 AND term2 : Subexpressions swapped.</li>
   * <li>NOT term1 : Expression discarded if node is the root node. Otherwise, the parent node may
   * contain another subexpression that will make this one valid.</li>
   * <li>NOT term1 AND NOT term2 : Expression discarded if node is the root node. Otherwise, the
   * parent node may contain another subexpression that will make this one valid.</li>
   * <li>term1 OR NOT term2 : Expression discarded.</li>
   * </ul>
   *
   * @param node   Node to fix up.
   * @param isRoot True if node is the tree's root node.
   * @return Root node of expression tree.
   */
  private AbstractNode<T> fixUpTree(@Var AbstractNode<T> node, boolean isRoot) {

    // Test for empty expression tree
    if (node == null) {
      return null;
    }

    if (node instanceof InternalNode) {

      // Fix up child nodes
      InternalNode<T> internalNode = (InternalNode<T>) node;

      // Push down negations
      if (node.exclude() && internalNode.child1() != null && internalNode.child2() != null) {
        if (InternalNode.eConjunctionTypes.Or.equals(internalNode.conjunction())) {
          internalNode.conjunction(InternalNode.eConjunctionTypes.And);
          internalNode.exclude(false);
          internalNode.child1().exclude(!internalNode.child1().exclude());
          internalNode.child2().exclude(!internalNode.child2().exclude());
        } else if (InternalNode.eConjunctionTypes.And.equals(internalNode.conjunction())) {
          internalNode.conjunction(InternalNode.eConjunctionTypes.Or);
          internalNode.exclude(false);
          internalNode.child1().exclude(!internalNode.child1().exclude());
          internalNode.child2().exclude(!internalNode.child2().exclude());
        }
      }

      internalNode.child1(fixUpTree(internalNode.child1(), false));
      internalNode.child2(fixUpTree(internalNode.child2(), false));

      // Move up negations
      if (!node.exclude() && internalNode.child1() != null && internalNode.child2() != null) {
        if (InternalNode.eConjunctionTypes.Or.equals(internalNode.conjunction())) {
          if (internalNode.child1().exclude() && internalNode.child2().exclude()) {
            internalNode.conjunction(InternalNode.eConjunctionTypes.And);
            internalNode.exclude(true);
            internalNode.child1().exclude(!internalNode.child1().exclude());
            internalNode.child2().exclude(!internalNode.child2().exclude());
          }
        } else if (InternalNode.eConjunctionTypes.And.equals(internalNode.conjunction())) {
          if (internalNode.child1().exclude() && internalNode.child2().exclude()) {
            internalNode.conjunction(InternalNode.eConjunctionTypes.Or);
            internalNode.exclude(true);
            internalNode.child1().exclude(!internalNode.child1().exclude());
            internalNode.child2().exclude(!internalNode.child2().exclude());
          }
        }
      }

      // Handle eliminated child expressions
      if (internalNode.child1() == null && internalNode.child2() == null) {

        // Eliminate parent node if both child nodes were eliminated
        return null;
      } else if (internalNode.child2() == null) {

        // child2 eliminated so return only child1
        node = internalNode.child1();
      } else if (internalNode.child1() == null) {

        // child1 eliminated so return only child2
        node = internalNode.child2();
      } else if (internalNode.child1().exclude() && !internalNode.child2().exclude()) {

        // If only first child expression is an exclude expression, then simply swap child
        // expressions
        AbstractNode<T> temp = internalNode.child1();
        internalNode.child1(internalNode.child2());
        internalNode.child2(temp);
      }
    }

    // Eliminate group if it contains only negated expressions
    return isRoot && node.exclude() ? null : node;
  }

  /**
   * Extracts a block of text delimited by double quotes. It is assumed the parser is positioned at the first quote. The
   * quotes are not included in the returned string. On return, the parser is positioned at the closing quote or at the
   * end of the text if the closing quote was not found.
   *
   * @param iterator TextParser object.
   * @return The extracted text.
   */
  private String extractQuote(StringIterator iterator) {

    iterator.moveAhead();
    int start = iterator.position();

    while (!iterator.isEndOfText() && iterator.peek() != '\"') {
      iterator.moveAhead();
    }
    return iterator.extract(start, iterator.position());
  }

  /**
   * Extracts a block of text delimited by the specified open and close characters. It is assumed the parser is
   * positioned at an occurrence of the open character. The open and closing characters are not included in the returned
   * string. On return, the parser is positioned at the closing character or at the end of the text if the closing
   * character was not found.
   *
   * @param iterator  TextParser object.
   * @param openChar  Start-of-block delimiter.
   * @param closeChar End-of-block delimiter.
   * @return The extracted text.
   */
  private String extractBlock(StringIterator iterator, char openChar, char closeChar) {

    // Track delimiter depth
    @Var int depth = 1;

    // Extract characters between delimiters
    iterator.moveAhead();
    int start = iterator.position();

    while (!iterator.isEndOfText()) {
      if (iterator.peek() == openChar) {

        // Increase block depth
        depth++;
      } else if (iterator.peek() == closeChar) {

        // Decrease block depth
        depth--;

        // Test for end of block
        if (depth == 0) {
          break;
        }
      } else if (iterator.peek() == '"') {

        // Don't count delimiters within quoted text
        String quotedTerm = extractQuote(iterator);
      }

      // Move to next character
      iterator.moveAhead();
    }
    return iterator.extract(start, iterator.position());
  }

  /**
   * Adds an expression node to the given tree.
   *
   * @param root        Root node of expression tree.
   * @param node        Node to add.
   * @param conjunction Conjunction used to join with other nodes.
   * @return The new root node.
   */
  private AbstractNode<T> addNode(@Var AbstractNode<T> root, AbstractNode<T> node,
      InternalNode.eConjunctionTypes conjunction) {
    if (node != null) {
      if (root != null) {
        root = new InternalNode<>(conjunction, root, node);
      } else {
        root = node;
      }
    }
    return root;
  }

  /**
   * Creates an expression node and adds it to the given tree.
   *
   * @param root        Root node of expression tree.
   * @param object      Term for this node.
   * @param predicate   Indicates dimension of this term.
   * @param form        Indicates form of this term.
   * @param exclude     Indicates if this is an excluded term.
   * @param conjunction Conjunction used to join with other nodes.
   * @return The new root node.
   */
  private AbstractNode<T> addNode(@Var AbstractNode<T> root, String object, String predicate,
      TerminalNode.eTermForms form, boolean exclude, InternalNode.eConjunctionTypes conjunction) {
    if (object != null && object.length() > 0 && !isStopWord(object)) {
      TerminalNode<T> terminalNode = new TerminalNode<>(form, predicate, object);
      terminalNode.exclude(exclude);
      root = addNode(root, terminalNode, conjunction);
    }
    return root;
  }

  /**
   * Determines if the given word has been identified as a stop word.
   *
   * @param word word.
   * @return true if word is a stopword, false otherwise.
   */
  private boolean isStopWord(String word) {
    return STOPWORDS.contains(word);
  }

  /**
   * Determines if the specified node is invalid on either side of an OR conjunction.
   *
   * @param node Node to test.
   * @return true if the node is valid, false otherwise.
   */
  private boolean isInvalidWithOr(AbstractNode<T> node) {

    // OR is only valid with non-null, non-excluded (NOT) subexpressions
    return node == null || node.exclude();
  }
}
