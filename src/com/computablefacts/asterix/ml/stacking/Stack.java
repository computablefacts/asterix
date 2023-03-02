package com.computablefacts.asterix.ml.stacking;

import com.computablefacts.asterix.Result;
import com.computablefacts.asterix.ml.ConfusionMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.computablefacts.asterix.nlp.Span;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@CheckReturnValue
final public class Stack {

  private final Result<AbstractStack> stack_;

  public Stack(List<? extends AbstractStack> stacks) {

    Preconditions.checkNotNull(stacks, "stacks should not be null");

    List<AbstractStack> stackz = new ArrayList<>();

    for (int i = 0; i < stacks.size(); i++) {
      AbstractStack leftStack = stacks.get(i);
      AbstractStack stack = merge(eStackType.AND, leftStack, stacks.subList(i + 1, stacks.size()));
      stackz.add(stack);
    }

    List<AbstractStack> newStackz = new ArrayList<>();

    for (int i = 0; i < stackz.size(); i++) {
      AbstractStack leftStacl = stackz.get(i);
      AbstractStack stack = merge(eStackType.OR, leftStacl, stackz.subList(i + 1, stackz.size()));
      newStackz.add(stack);
    }

    newStackz.addAll(stacks);

    stack_ = newStackz.stream()
        .filter(stack -> Double.isFinite(stack.confusionMatrix().matthewsCorrelationCoefficient())).max(
            Comparator.comparingDouble(
                (AbstractStack stack) -> stack.confusionMatrix().matthewsCorrelationCoefficient())).map(Result::success)
        .orElseGet(() -> Result.failure("stack not found"));
  }

  @Override
  public String toString() {

    Preconditions.checkState(stack_ != null && stack_.isSuccess(), "missing stack");

    return stack_.successValue().toString();
  }

  public ConfusionMatrix confusionMatrix() {

    Preconditions.checkState(stack_ != null && stack_.isSuccess(), "missing stack");

    return stack_.successValue().confusionMatrix();
  }

  public void compactify() {

    Preconditions.checkState(stack_ != null && stack_.isSuccess(), "missing stack");

    stack_.successValue().compactify();
  }

  public int predict(String text) {

    Preconditions.checkNotNull(text, "text should not be null");
    Preconditions.checkState(stack_ != null && stack_.isSuccess(), "missing stack");

    return stack_.successValue().predict(text);
  }

  @Beta
  public int predictOnSplitText(List<List<Span>> text) {

    Preconditions.checkNotNull(text, "text should not be null");
    Preconditions.checkState(stack_ != null && stack_.isSuccess(), "missing stack");

    return stack_.successValue().predictOnSplitText(text);
  }

  public int predict(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkState(stack_ != null && stack_.isSuccess(), "missing stack");

    return stack_.successValue().predict(vector);
  }

  @Beta
  public Result<String> focus(String text) {

    Preconditions.checkNotNull(text, "text should not be null");
    Preconditions.checkState(stack_ != null && stack_.isSuccess(), "missing stack");

    return stack_.successValue().focus(text);
  }

  @Beta
  public Result<String> focusOnSplitText(List<List<Span>> text) {

    Preconditions.checkNotNull(text, "text should not be null");
    Preconditions.checkState(stack_ != null && stack_.isSuccess(), "missing stack");

    return stack_.successValue().focusOnSplitText(text);
  }

  private AbstractStack merge(eStackType stackType, AbstractStack leftNode, List<? extends AbstractStack> rightNodes) {

    Preconditions.checkNotNull(stackType, "stackType should not be null");
    Preconditions.checkNotNull(rightNodes, "rightNodes should not be null");
    Preconditions.checkNotNull(leftNode, "leftNode should not be null");

    if (rightNodes.isEmpty()) {
      return leftNode;
    }

    Optional<AbstractStack> node = merge(stackType, leftNode, rightNodes.get(0));

    if (node.isPresent()) {
      return merge(stackType, node.get(), rightNodes.subList(1, rightNodes.size()));
    }
    return merge(stackType, leftNode, rightNodes.subList(1, rightNodes.size()));
  }

  private Optional<AbstractStack> merge(eStackType stackType, AbstractStack leftStack, AbstractStack rightStack) {

    Preconditions.checkNotNull(stackType, "stackType should not be null");
    Preconditions.checkNotNull(leftStack, "leftStack should not be null");
    Preconditions.checkNotNull(rightStack, "rightStack should not be null");

    AbstractStack node;

    if (eStackType.OR.equals(stackType)) {
      node = new OrStack(leftStack, rightStack);
    } else {
      node = new AndStack(leftStack, rightStack);
    }

    double leftMcc = leftStack.confusionMatrix().matthewsCorrelationCoefficient();
    double rightMcc = rightStack.confusionMatrix().matthewsCorrelationCoefficient();
    double mcc = node.confusionMatrix().matthewsCorrelationCoefficient();

    if ((!Double.isFinite(leftMcc) || !Double.isFinite(rightMcc)) && Double.isFinite(mcc)) {
      return Optional.of(node);
    }
    if (Double.isFinite(leftMcc) && Double.isFinite(rightMcc) && mcc > leftMcc && mcc > rightMcc) {
      return Optional.of(node);
    }
    return Optional.empty();
  }

  private enum eStackType {
    AND, OR
  }
}
