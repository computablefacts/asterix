package com.computablefacts.asterix;

import static com.computablefacts.asterix.IO.eCompressionAlgorithm.GZIP;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.Var;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

public class ViewTest {

  @Test
  public void testViewOfArrayOfStrings() {
    Assert.assertEquals(Lists.newArrayList("a", "b", "c"), View.of("a", "b", "c").toList());
  }

  @Test
  public void testViewOfArrayOfIntegers() {
    Assert.assertEquals(Lists.newArrayList(1, 2, 3), View.of(1, 2, 3).toList());
  }

  @Test
  public void testViewOfSet() {

    View<String> view = View.of(Sets.newHashSet("a", "b", "c"));

    Assert.assertEquals(Sets.newHashSet("a", "b", "c"), view.toSet());
  }

  @Test
  public void testViewOfList() {

    View<String> view = View.of(Lists.newArrayList("a", "b", "c"));

    Assert.assertEquals(Lists.newArrayList("a", "b", "c"), view.toList());
  }

  @Test
  public void testViewOfMap() {

    Map<Integer, Set<String>> map = new HashMap<>();
    map.put(1, Sets.newHashSet("a"));
    map.put(2, Sets.newHashSet("ab"));
    map.put(3, Sets.newHashSet("abc", "abc"));
    map.put(4, Sets.newHashSet("abcd"));

    List<Map.Entry<Integer, Set<String>>> list = View.of(map).toList();

    Assert.assertTrue(list.contains(new AbstractMap.SimpleEntry<>(1, Sets.newHashSet("a"))));
    Assert.assertTrue(list.contains(new AbstractMap.SimpleEntry<>(2, Sets.newHashSet("ab"))));
    Assert.assertTrue(list.contains(new AbstractMap.SimpleEntry<>(3, Sets.newHashSet("abc", "abc"))));
    Assert.assertTrue(list.contains(new AbstractMap.SimpleEntry<>(4, Sets.newHashSet("abcd"))));
  }

  @Test
  public void testViewOfFile() throws IOException {

    File file = java.nio.file.Files.createTempFile("test-", ".txt").toFile();
    String text = "a\nb\nc\nd";

    Assert.assertTrue(file.exists());
    Assert.assertTrue(IO.writeText(file, text, true));

    List<String> rows = View.of(file).toList();

    Assert.assertEquals(Lists.newArrayList("a", "b", "c", "d"), rows);
  }

  @Test
  public void testOfBufferedReader() throws Exception {

    String command = "for i in `seq 0 2 10`; do echo $i; done";
    ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
    processBuilder.redirectErrorStream(true);

    Process process = processBuilder.start();
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    List<String> rows = View.of(reader).toList();

    Assert.assertEquals(6, rows.size());
    Assert.assertEquals(Lists.newArrayList("0", "2", "4", "6", "8", "10"), rows);
  }

  @Test
  public void testStitchIterablesHaveSameLength() {

    List<String> list1 = Lists.newArrayList("0", "2", "4", "6", "8", "10");
    List<String> list2 = Lists.newArrayList("a", "c", "e", "g", "i", "k");
    List<Iterator<String>> views = Lists.newArrayList(list1.iterator(), list2.iterator());
    List<List<String>> stitched = View.stitch(views).toList();

    Assert.assertEquals(6, stitched.size());
    Assert.assertEquals(
        Lists.newArrayList(Lists.newArrayList("0", "a"), Lists.newArrayList("2", "c"), Lists.newArrayList("4", "e"),
            Lists.newArrayList("6", "g"), Lists.newArrayList("8", "i"), Lists.newArrayList("10", "k")), stitched);
  }

  @Test
  public void testStitchOneIterablesIsShorter() {

    List<String> list1 = Lists.newArrayList("0", "2", "4");
    List<String> list2 = Lists.newArrayList("a", "c", "e", "g", "i", "k");
    List<Iterator<String>> views = Lists.newArrayList(list1.iterator(), list2.iterator());
    List<List<String>> stitched = View.stitch(views).toList();

    Assert.assertEquals(3, stitched.size());
    Assert.assertEquals(
        Lists.newArrayList(Lists.newArrayList("0", "a"), Lists.newArrayList("2", "c"), Lists.newArrayList("4", "e")),
        stitched);
  }

  @Test
  public void testSplitOnChar() {

    String str = "0, 2, 4, 6, 8, 10";
    List<String> rows = View.split(str, ',').toList();

    Assert.assertEquals(6, rows.size());
    Assert.assertEquals(Lists.newArrayList("0", "2", "4", "6", "8", "10"), rows);
  }

  @Test
  public void testSplitOnString() {

    String str = "0, 2, 4, 6, 8, 10";
    List<String> rows = View.split(str, ",").toList();

    Assert.assertEquals(6, rows.size());
    Assert.assertEquals(Lists.newArrayList("0", "2", "4", "6", "8", "10"), rows);
  }

  @Test
  public void testExecuteBashCommand() {

    String command = "for i in `seq 0 2 10`; do echo $i; done";
    List<String> rows = View.executeBashCommand(command).toList();

    Assert.assertEquals(6, rows.size());
    Assert.assertEquals(Lists.newArrayList("0", "2", "4", "6", "8", "10"), rows);
  }

  @Test
  public void testViewOfCompressedFile() throws IOException {

    File file = java.nio.file.Files.createTempFile("test-", ".gz").toFile();
    String text = "a\nb\nc\nd";

    Assert.assertTrue(file.exists());
    Assert.assertTrue(IO.writeText(file, text, true, GZIP));

    List<String> rows = View.of(file, GZIP).toList();

    Assert.assertEquals(Lists.newArrayList("a", "b", "c", "d"), rows);
  }

  @Test
  public void testToList() {

    View<String> view = View.of("a", "b", "b", "c", "c", "c");

    Assert.assertEquals(Lists.newArrayList("a", "b", "b", "c", "c", "c"), view.toList());
  }

  @Test
  public void testToSortedList() {

    View<String> view = View.of("a", "b", "b", "c", "c", "c");

    Assert.assertEquals(Lists.newArrayList("c", "c", "c", "b", "b", "a"),
        view.toSortedList(Ordering.natural().reverse()));
  }

  @Test
  public void testToSet() {

    View<String> view = View.of("a", "b", "b", "c", "c", "c");

    Assert.assertEquals(Sets.newHashSet("a", "b", "c"), view.toSet());
  }

  @Test
  public void testToFile() throws IOException {

    List<String> list = Lists.newArrayList("a", "b", "b", "c", "c", "c");
    File file = java.nio.file.Files.createTempFile("test-", ".txt").toFile();

    View.of(list).toFile(file, true);

    Assert.assertEquals(list, View.of(file).toList());
  }

  @Test
  public void testToFileOfEmptyView() throws IOException {

    List<String> list = Lists.newArrayList();
    File file = java.nio.file.Files.createTempFile("test-", ".txt").toFile();

    View.of(list).toFile(file, true);

    Assert.assertEquals(list, View.of(file).toList());
  }

  @Test
  public void testToCompressedFile() throws IOException {

    List<String> list = Lists.newArrayList("a", "b", "b", "c", "c", "c");
    File file = java.nio.file.Files.createTempFile("test-", ".gz").toFile();

    View.of(list).toFile(file, true, true);

    Assert.assertEquals(list, View.of(file, true).toList());
  }

  @Test
  public void testZipUnzipViewsHaveSameSize() {

    View<String> view1 = View.repeat("a").take(5);
    View<String> view2 = View.repeat("b").take(5);
    List<String> zipped = view1.zip(view2).map(e -> e.getKey() + e.getValue()).toList();

    Assert.assertEquals(Lists.newArrayList("ab", "ab", "ab", "ab", "ab"), zipped);

    Map.Entry<List<String>, List<String>> unzipped = View.of(zipped)
        .unzip(e -> new AbstractMap.SimpleEntry<>(e.substring(0, 1), e.substring(1, 2)));

    Assert.assertEquals(View.repeat("a").take(5).toList(), unzipped.getKey());
    Assert.assertEquals(View.repeat("b").take(5).toList(), unzipped.getValue());
  }

  @Test
  public void testZipUnzipLeftViewIsLonger() {

    View<String> view1 = View.repeat("a").take(10);
    View<String> view2 = View.repeat("b").take(5);
    List<String> zipped = view1.zip(view2).map(e -> e.getKey() + e.getValue()).toList();

    Assert.assertEquals(Lists.newArrayList("ab", "ab", "ab", "ab", "ab"), zipped);

    Map.Entry<List<String>, List<String>> unzipped = View.of(zipped)
        .unzip(e -> new AbstractMap.SimpleEntry<>(e.substring(0, 1), e.substring(1, 2)));

    Assert.assertEquals(View.repeat("a").take(5).toList(), unzipped.getKey());
    Assert.assertEquals(View.repeat("b").take(5).toList(), unzipped.getValue());
  }

  @Test
  public void testZipUnzipRightViewIsLonger() {

    View<String> view1 = View.repeat("a").take(5);
    View<String> view2 = View.repeat("b").take(10);
    List<String> zipped = view1.zip(view2).map(e -> e.getKey() + e.getValue()).toList();

    Assert.assertEquals(Lists.newArrayList("ab", "ab", "ab", "ab", "ab"), zipped);

    Map.Entry<List<String>, List<String>> unzipped = View.of(zipped)
        .unzip(e -> new AbstractMap.SimpleEntry<>(e.substring(0, 1), e.substring(1, 2)));

    Assert.assertEquals(View.repeat("a").take(5).toList(), unzipped.getKey());
    Assert.assertEquals(View.repeat("b").take(5).toList(), unzipped.getValue());
  }

  @Test
  public void testRepeat() {

    Set<String> set = View.repeat("a").take(5).toSet();
    List<String> list = View.repeat("a").take(5).toList();

    Assert.assertEquals(Sets.newHashSet("a"), set);
    Assert.assertEquals(Lists.newArrayList("a", "a", "a", "a", "a"), list);
  }

  @Test
  public void testRange() {

    List<Integer> list = View.range(100, 105).toList();

    Assert.assertEquals(Lists.newArrayList(100, 101, 102, 103, 104), list);
  }

  @Test
  public void testIterate() {

    Set<Integer> set = View.iterate(1, x -> x + 1).take(5).toSet();
    List<Integer> list = View.iterate(1, x -> x + 1).take(5).toList();

    Assert.assertEquals(Sets.newHashSet(1, 2, 3, 4, 5), set);
    Assert.assertEquals(Lists.newArrayList(1, 2, 3, 4, 5), list);
  }

  @Test
  public void testIndex() {

    Set<Map.Entry<Integer, String>> set = View.repeat("a").take(3).index().toSet();
    List<Map.Entry<Integer, String>> list = View.repeat("a").take(3).index().toList();

    Assert.assertEquals(Sets.newHashSet(new AbstractMap.SimpleEntry<>(1, "a"), new AbstractMap.SimpleEntry<>(2, "a"),
        new AbstractMap.SimpleEntry<>(3, "a")), set);
    Assert.assertEquals(Lists.newArrayList(new AbstractMap.SimpleEntry<>(1, "a"), new AbstractMap.SimpleEntry<>(2, "a"),
        new AbstractMap.SimpleEntry<>(3, "a")), list);
  }

  @Test
  public void testContains() {

    View<Integer> view = View.range(100, 105);

    Assert.assertTrue(view.contains(x -> x == 103));
  }

  @Test
  public void testAnyMatch() {

    View<Integer> view = View.range(100, 105);

    Assert.assertTrue(view.anyMatch(x -> x % 2 == 0));
  }

  @Test
  public void testAllMatch() {

    View<Integer> view = View.range(100, 105).filter(x -> x % 2 == 0);

    Assert.assertTrue(view.allMatch(x -> x % 2 == 0));
  }

  @Test
  public void testNoneMatch() {

    View<Integer> view = View.range(100, 105).filter(x -> x % 2 == 0);

    Assert.assertTrue(view.noneMatch(x -> x % 2 == 1));
  }

  @Test
  public void testFindAll() {

    List<Integer> list = View.iterate(1, x -> x + 1).take(5).findAll(x -> x % 2 == 0).toList();

    Assert.assertEquals(Lists.newArrayList(2, 4), list);
  }

  @Test
  public void testFindFirst() {

    Result<Integer> opt = View.iterate(1, x -> x + 1).take(5).findFirst(x -> x % 2 == 0);

    Assert.assertEquals(2, (long) opt.successValue());
  }

  @Test
  public void testReduce() {

    int value = View.iterate(1, x -> x + 1).take(100).reduce(0, (c, x) -> c + x);

    Assert.assertEquals(5050, value);
  }

  @Test
  public void testPrepend() {

    List<Integer> list = View.iterate(2, x -> x + 1).take(3).prepend(1).toList();

    Assert.assertEquals(Lists.newArrayList(1, 2, 3, 4), list);
  }

  @Test
  public void testAppend() {

    List<Integer> list = View.iterate(1, x -> x + 1).take(3).append(4).toList();

    Assert.assertEquals(Lists.newArrayList(1, 2, 3, 4), list);
  }

  @Test
  public void testToMap() {

    View<String> view = View.of("a", "a", "ab", "abc", "abc", "abcd");
    Map<Integer, String> actual = view.toMap(String::length, Function.identity());
    Map<Integer, String> expected = new HashMap<>();

    expected.put(1, "a");
    expected.put(2, "ab");
    expected.put(3, "abc");
    expected.put(4, "abcd");

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testToMapOfSets() {

    View<String> view = View.of("a", "a", "ab", "abc", "abc", "abcd");
    Map<Integer, Collection<String>> actual = view.toMap(String::length, Function.identity(), Sets::newHashSet);
    Map<Integer, Collection<String>> expected = new HashMap<>();

    expected.put(1, Sets.newHashSet("a"));
    expected.put(2, Sets.newHashSet("ab"));
    expected.put(3, Sets.newHashSet("abc", "abc"));
    expected.put(4, Sets.newHashSet("abcd"));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testToMapOfLists() {

    View<String> view = View.of("a", "a", "ab", "abc", "abc", "abcd");
    Map<Integer, Collection<String>> actual = view.toMap(String::length, Function.identity(), Lists::newArrayList);
    Map<Integer, Collection<String>> expected = new HashMap<>();

    expected.put(1, Lists.newArrayList("a", "a"));
    expected.put(2, Lists.newArrayList("ab"));
    expected.put(3, Lists.newArrayList("abc", "abc"));
    expected.put(4, Lists.newArrayList("abcd"));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testTakeAll() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.take(5).toList();

    Assert.assertEquals(Lists.newArrayList("a", "ab", "abc", "abcd", "abcde"), list);
  }

  @Test
  public void testTakeNone() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.take(0).toList();

    Assert.assertTrue(list.isEmpty());
  }

  @Test
  public void testTakeNOnEmptyView() {

    View<String> view = View.of(Collections.emptyList());
    List<String> list = view.take(5).toList();

    Assert.assertTrue(list.isEmpty());
  }

  @Test
  public void testTakeN() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.take(2).toList();

    Assert.assertEquals(Lists.newArrayList("a", "ab"), list);
  }

  @Test
  public void testTakeWhileOnEmptyView() {

    View<String> view = View.of(Collections.emptyList());
    List<String> list = view.takeWhile(w -> w.length() < 6).toList();

    Assert.assertTrue(list.isEmpty());
  }

  @Test
  public void testTakeWhileExhaustsView() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.takeWhile(w -> w.length() < 6).toList();

    Assert.assertEquals(Lists.newArrayList("a", "ab", "abc", "abcd", "abcde"), list);
  }

  @Test
  public void testTakeWhile() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.takeWhile(w -> w.length() == 1).toList();

    Assert.assertEquals(Lists.newArrayList("a"), list);
  }

  @Test
  public void testDropAll() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.skip(5).toList();

    Assert.assertTrue(list.isEmpty());
  }

  @Test
  public void testDropNone() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.skip(0).toList();

    Assert.assertEquals(Lists.newArrayList("a", "ab", "abc", "abcd", "abcde"), list);
  }

  @Test
  public void testDropNOnEmptyView() {

    View<String> view = View.of(Collections.emptyList());
    List<String> list = view.skip(2).toList();

    Assert.assertTrue(list.isEmpty());
  }

  @Test
  public void testDropN() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.skip(2).toList();

    Assert.assertEquals(Lists.newArrayList("abc", "abcd", "abcde"), list);
  }

  @Test
  public void testDropWhileOnEmptyView() {

    View<String> view = View.of(Collections.emptyList());
    List<String> list = view.skipWhile(w -> w.length() < 6).toList();

    Assert.assertTrue(list.isEmpty());
  }

  @Test
  public void testDropWhileExhaustsView() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.skipWhile(w -> w.length() < 6).toList();

    Assert.assertTrue(list.isEmpty());
  }

  @Test
  public void testDropWhile() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.skipWhile(w -> w.length() < 3).toList();

    Assert.assertEquals(Lists.newArrayList("abc", "abcd", "abcde"), list);
  }

  @Test
  public void testDropAndTake() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.skip(2).take(1).toList();

    Assert.assertEquals(Lists.newArrayList("abc"), list);
  }

  @Test
  public void testFilterView() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = view.filter(w -> w.length() % 2 == 0).toList();

    Assert.assertEquals(Lists.newArrayList("ab", "abcd"), list);
  }

  @Test
  public void testMapView() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<Integer> list = view.map(w -> w.length()).toList();

    Assert.assertEquals(Lists.newArrayList(1, 2, 3, 4, 5), list);
  }

  @Test
  public void testSort() {

    View<String> view = View.of("a", "b", "b", "c", "c", "c").sort(Ordering.natural().reverse());

    Assert.assertEquals(Lists.newArrayList("c", "c", "c", "b", "b", "a"), view.toList());
  }

  @Test
  public void testMapUsingASingleBashCommand() {

    Function<Integer, String> toString = x -> Integer.toString(x, 10);
    Function<String, Integer> toInt = x -> Integer.parseInt(x, 10);

    View<String> view1 = View.iterate(0, x -> x + 1).take(10_000_000).map(toString);
    List<Integer> list1 = view1.mapUsingBashCommand("sort | awk '/^[0-9][0]*$/{ print }'").map(toInt).toList();

    Assert.assertEquals(64, list1.size());
    Assert.assertEquals(0, (int) list1.get(0));
    Assert.assertEquals(1, (int) list1.get(1));
    Assert.assertEquals(2, (int) list1.get(8));
    Assert.assertEquals(3, (int) list1.get(15));
    Assert.assertEquals(4, (int) list1.get(22));
    Assert.assertEquals(5, (int) list1.get(29));
    Assert.assertEquals(6, (int) list1.get(36));
    Assert.assertEquals(7, (int) list1.get(43));
    Assert.assertEquals(8, (int) list1.get(50));
    Assert.assertEquals(9, (int) list1.get(57));

    View<String> view2 = View.iterate(0, x -> x + 1).take(10_000_000).map(toString);
    List<Integer> list2 = view2.mapUsingBashCommand("awk '/^[0-9][0]*$/{ print }' | sort").map(toInt).toList();

    Assert.assertEquals(64, list2.size());
    Assert.assertEquals(0, (int) list2.get(0));
    Assert.assertEquals(1, (int) list2.get(1));
    Assert.assertEquals(2, (int) list2.get(8));
    Assert.assertEquals(3, (int) list2.get(15));
    Assert.assertEquals(4, (int) list2.get(22));
    Assert.assertEquals(5, (int) list2.get(29));
    Assert.assertEquals(6, (int) list2.get(36));
    Assert.assertEquals(7, (int) list2.get(43));
    Assert.assertEquals(8, (int) list2.get(50));
    Assert.assertEquals(9, (int) list2.get(57));
  }

  @Test
  public void testMapIntegersToStringsUsingASingleBashCommand() {

    Function<String, Integer> toInt = x -> Integer.parseInt(x, 10);

    View<Integer> view = View.iterate(0, x -> x + 1).take(10_000_000);
    List<Integer> list = view.mapUsingBashCommand("sort | awk '/^[0-9][0]*$/{ print }'").map(toInt).toList();

    Assert.assertEquals(64, list.size());
    Assert.assertEquals(0, (int) list.get(0));
    Assert.assertEquals(1, (int) list.get(1));
    Assert.assertEquals(2, (int) list.get(8));
    Assert.assertEquals(3, (int) list.get(15));
    Assert.assertEquals(4, (int) list.get(22));
    Assert.assertEquals(5, (int) list.get(29));
    Assert.assertEquals(6, (int) list.get(36));
    Assert.assertEquals(7, (int) list.get(43));
    Assert.assertEquals(8, (int) list.get(50));
    Assert.assertEquals(9, (int) list.get(57));
  }

  @Test
  public void testMapLongsToStringsUsingASingleBashCommand() {

    Function<String, Integer> toInt = x -> Integer.parseInt(x, 10);

    View<Long> view = View.iterate(0L, x -> x + 1L).take(10_000_000);
    List<Integer> list = view.mapUsingBashCommand("sort | awk '/^[0-9][0]*$/{ print }'").map(toInt).toList();

    Assert.assertEquals(64, list.size());
    Assert.assertEquals(0, (int) list.get(0));
    Assert.assertEquals(1, (int) list.get(1));
    Assert.assertEquals(2, (int) list.get(8));
    Assert.assertEquals(3, (int) list.get(15));
    Assert.assertEquals(4, (int) list.get(22));
    Assert.assertEquals(5, (int) list.get(29));
    Assert.assertEquals(6, (int) list.get(36));
    Assert.assertEquals(7, (int) list.get(43));
    Assert.assertEquals(8, (int) list.get(50));
    Assert.assertEquals(9, (int) list.get(57));
  }

  @Test
  public void testMapDoublesToStringsUsingASingleBashCommand() {

    View<Double> view = View.iterate(0.0d, x -> x + 1.0d).take(10_000_000);
    List<Integer> list = view.mapUsingBashCommand("sort | awk '/^[0-9][0]*\\.[0-9]+$/{ print }'")
        .map(Double::parseDouble).map(Double::intValue).toList();

    Assert.assertEquals(64, list.size());
    Assert.assertEquals(0, (int) list.get(0));
    Assert.assertEquals(1, (int) list.get(1));
    Assert.assertEquals(2, (int) list.get(8));
    Assert.assertEquals(3, (int) list.get(15));
    Assert.assertEquals(4, (int) list.get(22));
    Assert.assertEquals(5, (int) list.get(29));
    Assert.assertEquals(6, (int) list.get(36));
    Assert.assertEquals(7, (int) list.get(43));
    Assert.assertEquals(8, (int) list.get(50));
    Assert.assertEquals(9, (int) list.get(57));
  }

  @Test
  public void testMapFloatsToStringsUsingASingleBashCommand() {

    View<Float> view = View.iterate(0.0f, x -> x + 1.0f).take(10_000_000);
    List<Integer> list = view.mapUsingBashCommand("sort | awk '/^[0-9][0]*\\.[0-9]+$/{ print }'").map(Float::parseFloat)
        .map(Float::intValue).toList();

    Assert.assertEquals(64, list.size());
    Assert.assertEquals(0, (int) list.get(0));
    Assert.assertEquals(1, (int) list.get(1));
    Assert.assertEquals(2, (int) list.get(8));
    Assert.assertEquals(3, (int) list.get(15));
    Assert.assertEquals(4, (int) list.get(22));
    Assert.assertEquals(5, (int) list.get(29));
    Assert.assertEquals(6, (int) list.get(36));
    Assert.assertEquals(7, (int) list.get(43));
    Assert.assertEquals(8, (int) list.get(50));
    Assert.assertEquals(9, (int) list.get(57));
  }

  @Test
  public void testMapUsingMultipleBashCommands() {

    Function<Integer, String> toString = x -> Integer.toString(x, 10);
    Function<String, Integer> toInt = x -> Integer.parseInt(x, 10);

    View<String> view1 = View.iterate(0, x -> x + 1).take(10_000_000).map(toString);
    List<Integer> list1 = view1.mapUsingBashCommand("sort").mapUsingBashCommand("awk '/^[0-9][0]*$/{ print }'")
        .map(toInt).toList();

    Assert.assertEquals(64, list1.size());
    Assert.assertEquals(0, (int) list1.get(0));
    Assert.assertEquals(1, (int) list1.get(1));
    Assert.assertEquals(2, (int) list1.get(8));
    Assert.assertEquals(3, (int) list1.get(15));
    Assert.assertEquals(4, (int) list1.get(22));
    Assert.assertEquals(5, (int) list1.get(29));
    Assert.assertEquals(6, (int) list1.get(36));
    Assert.assertEquals(7, (int) list1.get(43));
    Assert.assertEquals(8, (int) list1.get(50));
    Assert.assertEquals(9, (int) list1.get(57));

    View<String> view2 = View.iterate(0, x -> x + 1).take(10_000_000).map(toString);
    List<Integer> list2 = view2.mapUsingBashCommand("awk '/^[0-9][0]*$/{ print }'").mapUsingBashCommand("sort")
        .map(toInt).toList();

    Assert.assertEquals(64, list2.size());
    Assert.assertEquals(0, (int) list2.get(0));
    Assert.assertEquals(1, (int) list2.get(1));
    Assert.assertEquals(2, (int) list2.get(8));
    Assert.assertEquals(3, (int) list2.get(15));
    Assert.assertEquals(4, (int) list2.get(22));
    Assert.assertEquals(5, (int) list2.get(29));
    Assert.assertEquals(6, (int) list2.get(36));
    Assert.assertEquals(7, (int) list2.get(43));
    Assert.assertEquals(8, (int) list2.get(50));
    Assert.assertEquals(9, (int) list2.get(57));
  }

  @Test
  public void testEncryptThenDecrypt() {

    Function<Integer, String> toString = x -> Integer.toString(x, 10);
    Function<String, Integer> toInt = x -> Integer.parseInt(x, 10);

    // Write the expected output to file
    Path expected = IO.newTmpFile(".tsv.gz").getOrThrow();

    View.iterate(0, x -> x + 1).take(10_000_000).map(toString)
        .mapUsingBashCommand("awk '/^[0-9][0]*$/{ print }' | sort").toFile(expected.toFile(), true, GZIP);

    // Encrypt stream and write the actual output to file
    Path actual = IO.newTmpFile(".tsv.gz").getOrThrow();

    View.iterate(0, x -> x + 1).take(10_000_000).map(toString)
        .mapUsingBashCommand("awk '/^[0-9][0]*$/{ print }' | sort").encrypt("p@ssw0rd!")
        .toFile(actual.toFile(), true, GZIP);

    // Read file then decrypt stream and compare the result to the expected output
    List<Map.Entry<String, String>> list = View.of(expected.toFile(), GZIP)
        .zip(View.of(actual.toFile(), GZIP).decrypt("p@ssw0rd!")).toList();

    Assert.assertEquals(64, list.size());

    for (Map.Entry<String, String> entry : list) {
      Assert.assertEquals(entry.getKey(), entry.getValue());
    }
  }

  @Test
  public void testConcatViews() {

    View<String> left1 = View.of("a", "ab", "abc");
    View<String> right1 = View.of("abcd", "abcde");
    List<String> list1 = left1.concat(right1).toList();

    Assert.assertEquals(Lists.newArrayList("a", "ab", "abc", "abcd", "abcde"), list1);

    View<String> left2 = View.of("a", "ab", "abc");
    View<String> right2 = View.of("abcd", "abcde");
    List<String> list2 = right2.concat(left2).toList();

    Assert.assertEquals(Lists.newArrayList("abcd", "abcde", "a", "ab", "abc"), list2);
  }

  @Test
  public void testPartitionView() {

    View<String> view = View.of("a", "ab", "abc", "abcd", "abcde");
    List<List<String>> list = view.partition(3).toList();

    Assert.assertEquals(Lists.newArrayList(Lists.newArrayList("a", "ab", "abc"), Lists.newArrayList("abcd", "abcde")),
        list);
  }

  @Test
  public void testDiffEmptyViewAgainstEmptyView() {

    View<String> left = View.of();
    View<String> right = View.of();
    List<String> diff = left.diffSorted(right).toList();

    Assert.assertTrue(diff.isEmpty());
  }

  @Test
  public void testDiffEmptyViewAgainstNonEmptyView() {

    View<String> left = View.of();
    View<String> right = View.of("a", "b", "c", "d", "e");
    List<String> diff = left.diffSorted(right).toList();

    Assert.assertTrue(diff.isEmpty());
  }

  @Test
  public void testDiffNonEmptyViewAgainstEmptyView() {

    View<String> left = View.of("a", "b", "c", "d", "e");
    View<String> right = View.of();
    List<String> diff = left.diffSorted(right).toList();

    Assert.assertEquals(Lists.newArrayList("a", "b", "c", "d", "e"), diff);
  }

  @Test
  public void testDiffViewAgainstItself() {

    View<String> left = View.of("a", "b", "c", "d", "e");
    View<String> right = View.of("a", "b", "c", "d", "e");
    List<String> diff = left.diffSorted(right).toList();

    Assert.assertTrue(diff.isEmpty());
  }

  @Test
  public void testDiffRemovesElementsInEvenPositions() {

    View<String> left = View.of("a", "b", "c", "d", "e");
    View<String> right = View.of("a", "c", "e");
    List<String> diff = left.diffSorted(right).toList();

    Assert.assertEquals(Lists.newArrayList("b", "d"), diff);
  }

  @Test
  public void testDiffRemovesElementsInOddPositions() {

    View<String> left = View.of("a", "b", "c", "d", "e");
    View<String> right = View.of("b", "d");
    List<String> diff = left.diffSorted(right).toList();

    Assert.assertEquals(Lists.newArrayList("a", "c", "e"), diff);
  }

  @Test
  public void testDiffLeftViewHasMoreElements() {

    View<String> left = View.of("a", "b", "c", "d", "e", "f");
    View<String> right = View.of("a", "c", "e");
    List<String> diff = left.diffSorted(right).toList();

    Assert.assertEquals(Lists.newArrayList("b", "d", "f"), diff);
  }

  @Test
  public void testDiffRightViewHasMoreElements() {

    View<String> left = View.of("a", "b", "c", "d", "e");
    View<String> right = View.of("b", "d", "f", "h");
    List<String> diff = left.diffSorted(right).toList();

    Assert.assertEquals(Lists.newArrayList("a", "c", "e"), diff);
  }

  @Test
  public void testDedupEmptyView() {

    List<String> list = View.<String>of(Collections.emptyList()).dedupSorted().toList();

    Assert.assertTrue(list.isEmpty());
  }

  @Test
  public void testDedupViewWithoutDuplicates() {

    List<String> actual = View.of("a", "b", "c", "d", "e", "f", "g", "h").dedupSorted().toList();
    List<String> expected = Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h");

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testDedupViewWithDuplicates() {

    List<String> actual = View.of(
            Lists.newArrayList("a", "a", "b", "b", "b", "c", "c", "c", "c", "d", "e", "f", "g", "h")).dedupSorted()
        .toList();
    List<String> expected = Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h");

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testFlattenEmptyView() {

    List<String> actual = View.<View<String>>of(Collections.emptyList()).flatten(l -> l).toList();

    Assert.assertTrue(actual.isEmpty());
  }

  @Test
  public void testFlattenFlatView() {

    List<String> actual = View.of("a", "ab", "abc", "abcd", "abcde").flatten(View::of).toList();
    List<String> expected = Lists.newArrayList("a", "ab", "abc", "abcd", "abcde");

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testFlattenViewOfViews() {

    List<String> actual = View.of(Lists.newArrayList(View.of("a", "ab"), View.of("abc", "abcd", "abcde")))
        .flatten(l -> l).toList();
    List<String> expected = Lists.newArrayList("a", "ab", "abc", "abcd", "abcde");

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testIntersectEmptyViewWithEmptyView() {

    View<String> left = View.of(Collections.emptyList());
    View<String> right = View.of(Collections.emptyList());
    List<String> list = left.intersectSorted(right).toList();

    Assert.assertTrue(list.isEmpty());
  }

  @Test
  public void testIntersectEmptyViewWithNonEmptyView() {

    View<String> left = View.of(Collections.emptyList());
    View<String> right = View.of("a", "ab", "abc", "abcd", "abcde");
    List<String> list = left.intersectSorted(right).toList();

    Assert.assertTrue(list.isEmpty());
  }

  @Test
  public void testIntersectNonEmptyViewWithEmptyView() {

    View<String> left = View.of("a", "ab", "abc", "abcd", "abcde");
    View<String> right = View.of(Collections.emptyList());
    List<String> list = left.intersectSorted(right).toList();

    Assert.assertTrue(list.isEmpty());
  }

  @Test
  public void testIntersectKeepsOnlyElementsInEvenPositions() {

    View<String> left = View.of("a", "b", "c", "d", "e");
    View<String> right = View.of("b", "d");
    List<String> intersection = left.intersectSorted(right).toList();

    Assert.assertEquals(Lists.newArrayList("b", "d"), intersection);
  }

  @Test
  public void testIntersectKeepsOnlyElementsInOddPositions() {

    View<String> left = View.of("a", "b", "c", "d", "e");
    View<String> right = View.of("a", "c", "e");
    List<String> intersection = left.intersectSorted(right).toList();

    Assert.assertEquals(Lists.newArrayList("a", "c", "e"), intersection);
  }

  @Test
  public void testIntersectLeftViewDoesNotOverlapRightView() {

    View<String> left = View.of("a", "b", "c", "d", "e", "f");
    View<String> right = View.of("0", "1", "2");
    List<String> intersection = left.intersectSorted(right).toList();

    Assert.assertTrue(intersection.isEmpty());
  }

  @Test
  public void testIntersectLeftViewOverlapsRightView() {

    View<String> left = View.of("a", "b", "c", "d", "e", "f");
    View<String> right = View.of("0", "1", "2", "a", "c", "e");
    List<String> intersection = left.intersectSorted(right).toList();

    Assert.assertEquals(Lists.newArrayList("a", "c", "e"), intersection);
  }

  @Test
  public void testIntersectLeftViewHasDuplicatedElements() {

    View<String> left = View.of("a", "a", "b", "b", "b", "c", "c", "c", "c", "d", "d", "d", "d", "d", "e", "e", "e",
        "e", "e", "e");
    View<String> right = View.of("a", "c", "e");
    List<String> intersection = left.intersectSorted(right).toList();

    Assert.assertEquals(Lists.newArrayList("a", "c", "e"), intersection);
  }

  @Test
  public void testIntersectRightViewHasDuplicatedElements() {

    View<String> left = View.of("a", "b", "c", "d", "e");
    View<String> right = View.of("a", "a", "c", "c", "c", "e", "e", "e", "e");
    List<String> intersection = left.intersectSorted(right).toList();

    Assert.assertEquals(Lists.newArrayList("a", "c", "e"), intersection);
  }

  @Test
  public void testIntersectBothViewsHaveDuplicatedElements() {

    View<String> left = View.of("a", "b", "c", "c", "c", "d", "e");
    View<String> right = View.of("a", "a", "a", "c", "e");
    List<String> intersection = left.intersectSorted(right).toList();

    Assert.assertEquals(Lists.newArrayList("a", "c", "e"), intersection);
  }

  @Test
  public void testForEachRemainingWithoutBreaker() {

    List<String> result = new ArrayList<>();
    View<String> view = View.of(Stream.of("cat", "dog", "elephant", "fox", "rabbit", "duck"));

    view.forEachRemaining((elem) -> {
      if (elem.length() % 2 == 0) {
        result.add(elem);
      }
    });

    Assert.assertEquals(Lists.newArrayList("elephant", "rabbit", "duck"), result);
  }

  @Test
  public void testForEachRemainingWithBreaker() {

    List<String> result = new ArrayList<>();
    View<String> view = View.of(Stream.of("cat", "dog", "elephant", "fox", "rabbit", "duck"));

    view.forEachRemaining((elem, breaker) -> {
      if (elem.length() % 2 == 0) {
        breaker.stop();
      } else {
        result.add(elem);
      }
    });

    Assert.assertEquals(Lists.newArrayList("cat", "dog"), result);
  }

  @Test
  public void testForEachRemainingInParallel() {

    Set<String> result = new HashSet<>();
    View<String> view = View.of(Stream.of("cat", "dog", "elephant", "fox", "rabbit", "duck"));

    view.forEachRemainingInParallel((elem) -> {
      if (elem.length() % 2 == 0) {
        result.add(elem);
      }
    });

    Assert.assertEquals(3, Sets.intersection(Sets.newHashSet("elephant", "rabbit", "duck"), result).size());
  }

  @Test
  public void testMerge() {

    View<String> view1 = View.of("0", "2", "4");
    View<String> view2 = View.of("1", "3", "5");
    View<String> view3 = View.of("6", "7", "8", "9", "b", "d", "f");
    View<String> view4 = View.of("a", "c", "e", "g");

    View<String> merged = view1.mergeSorted(Lists.newArrayList(view2, view3, view4), String::compareTo);

    Assert.assertEquals(
        Lists.newArrayList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g"),
        merged.toList());
  }

  @Test
  public void testOverlappingWindow() {

    View<String> view = View.of("1", "2", "3", "4", "5", "6", "7");
    List<List<String>> windows = view.overlappingWindow(3).toList();

    Assert.assertEquals(7, windows.size());
    Assert.assertTrue(windows.contains(ImmutableList.of("1", "2", "3")));
    Assert.assertTrue(windows.contains(ImmutableList.of("2", "3", "4")));
    Assert.assertTrue(windows.contains(ImmutableList.of("3", "4", "5")));
    Assert.assertTrue(windows.contains(ImmutableList.of("4", "5", "6")));
    Assert.assertTrue(windows.contains(ImmutableList.of("5", "6", "7")));
    Assert.assertTrue(windows.contains(ImmutableList.of("6", "7")));
    Assert.assertTrue(windows.contains(ImmutableList.of("7")));
  }

  @Test
  public void testNonOverlappingWindow() {

    View<String> view = View.of("1", "2", "3", "4", "5", "6", "7");
    List<List<String>> windows = view.nonOverlappingWindow(3).toList();

    Assert.assertEquals(3, windows.size());
    Assert.assertTrue(windows.contains(ImmutableList.of("1", "2", "3")));
    Assert.assertTrue(windows.contains(ImmutableList.of("4", "5", "6")));
    Assert.assertTrue(windows.contains(ImmutableList.of("7")));
  }

  @Test
  public void testOverlappingWindowWithStrictLength() {

    View<String> view = View.of("1", "2", "3", "4", "5", "6", "7");
    List<List<String>> windows = view.overlappingWindowWithStrictLength(3).toList();

    Assert.assertEquals(5, windows.size());
    Assert.assertTrue(windows.contains(ImmutableList.of("1", "2", "3")));
    Assert.assertTrue(windows.contains(ImmutableList.of("2", "3", "4")));
    Assert.assertTrue(windows.contains(ImmutableList.of("3", "4", "5")));
    Assert.assertTrue(windows.contains(ImmutableList.of("4", "5", "6")));
    Assert.assertTrue(windows.contains(ImmutableList.of("5", "6", "7")));
  }

  @Test
  public void testNonOverlappingWindowWithStrictLength() {

    View<String> view = View.of("1", "2", "3", "4", "5", "6", "7");
    List<List<String>> windows = view.nonOverlappingWindowWithStrictLength(3).toList();

    Assert.assertEquals(2, windows.size());
    Assert.assertTrue(windows.contains(ImmutableList.of("1", "2", "3")));
    Assert.assertTrue(windows.contains(ImmutableList.of("4", "5", "6")));
  }

  @Test
  public void testGroupSorted() {

    List<Integer> list = new ArrayList<>();
    list.add(1);

    @Var List<List<Integer>> groups = View.of(list).groupSorted(Integer::equals).map(View::toList).toList();

    Assert.assertEquals(1, groups.size());
    Assert.assertTrue(groups.contains(ImmutableList.of(1)));

    list.add(2);
    list.add(2);

    groups = View.of(list).groupSorted(Integer::equals).map(View::toList).toList();

    Assert.assertEquals(2, groups.size());
    Assert.assertEquals(ImmutableList.of(1), groups.get(0));
    Assert.assertEquals(ImmutableList.of(2, 2), groups.get(1));

    list.add(3);
    list.add(3);
    list.add(3);

    groups = View.of(list).groupSorted(Integer::equals).map(View::toList).toList();

    Assert.assertEquals(3, groups.size());
    Assert.assertEquals(ImmutableList.of(1), groups.get(0));
    Assert.assertEquals(ImmutableList.of(2, 2), groups.get(1));
    Assert.assertEquals(ImmutableList.of(3, 3, 3), groups.get(2));
  }

  @Test
  public void testPeek() {

    List<String> valuesPeeked = new ArrayList<>();
    View<String> view = View.of("1", "2", "3", "4", "5", "6", "7").peek(valuesPeeked::add);

    Assert.assertTrue(valuesPeeked.isEmpty());

    List<String> valuesMaterialized = view.toList();

    Assert.assertEquals(valuesPeeked, valuesMaterialized);
  }

  @Test
  public void testPeekIfTrue() {

    List<Integer> evenNumbers = new ArrayList<>();
    Predicate<Integer> isEven = x -> x % 2 == 0;
    Consumer<Integer> ifTrue = element -> evenNumbers.add(element);
    List<Integer> list = View.iterate(1, x -> x + 1).take(5).peekIfTrue(isEven, ifTrue).toList();

    Assert.assertEquals(5, list.size());
    Assert.assertEquals(1, (long) list.get(0));
    Assert.assertEquals(2, (long) list.get(1));
    Assert.assertEquals(3, (long) list.get(2));
    Assert.assertEquals(4, (long) list.get(3));
    Assert.assertEquals(5, (long) list.get(4));

    Assert.assertEquals(Lists.newArrayList(2, 4), evenNumbers);
  }

  @Test
  public void testPeekIfFalse() {

    List<Integer> oddNumbers = new ArrayList<>();
    Predicate<Integer> isEven = x -> x % 2 == 0;
    Consumer<Integer> ifFalse = element -> oddNumbers.add(element);
    List<Integer> list = View.iterate(1, x -> x + 1).take(5).peekIfFalse(isEven, ifFalse).toList();

    Assert.assertEquals(5, list.size());
    Assert.assertEquals(1, (long) list.get(0));
    Assert.assertEquals(2, (long) list.get(1));
    Assert.assertEquals(3, (long) list.get(2));
    Assert.assertEquals(4, (long) list.get(3));
    Assert.assertEquals(5, (long) list.get(4));

    Assert.assertEquals(Lists.newArrayList(1, 3, 5), oddNumbers);
  }

  @Test
  public void testPeekIfTrueAndIfFalse() {

    List<Integer> oddNumbers = new ArrayList<>();
    List<Integer> evenNumbers = new ArrayList<>();
    Predicate<Integer> isEven = x -> x % 2 == 0;
    Consumer<Integer> ifTrue = element -> evenNumbers.add(element);
    Consumer<Integer> ifFalse = element -> oddNumbers.add(element);
    List<Integer> list = View.iterate(1, x -> x + 1).take(5).peek(isEven, ifTrue, ifFalse).toList();

    Assert.assertEquals(5, list.size());
    Assert.assertEquals(1, (long) list.get(0));
    Assert.assertEquals(2, (long) list.get(1));
    Assert.assertEquals(3, (long) list.get(2));
    Assert.assertEquals(4, (long) list.get(3));
    Assert.assertEquals(5, (long) list.get(4));

    Assert.assertEquals(Lists.newArrayList(2, 4), evenNumbers);
    Assert.assertEquals(Lists.newArrayList(1, 3, 5), oddNumbers);
  }

  @Test
  public void testForEachRemainingIfTrue() {

    List<Integer> evenNumbers = new ArrayList<>();
    Predicate<Integer> isEven = x -> x % 2 == 0;
    Consumer<Integer> ifTrue = element -> evenNumbers.add(element);
    View.iterate(1, x -> x + 1).take(5).forEachRemainingIfTrue(isEven, ifTrue);

    Assert.assertEquals(Lists.newArrayList(2, 4), evenNumbers);
  }

  @Test
  public void testForEachRemainingIfFalse() {

    List<Integer> oddNumbers = new ArrayList<>();
    Predicate<Integer> isEven = x -> x % 2 == 0;
    Consumer<Integer> ifFalse = element -> oddNumbers.add(element);
    View.iterate(1, x -> x + 1).take(5).forEachRemainingIfFalse(isEven, ifFalse);

    Assert.assertEquals(Lists.newArrayList(1, 3, 5), oddNumbers);
  }

  @Test
  public void testForEachRemainingIfTrueAndIfFalse() {

    List<Integer> oddNumbers = new ArrayList<>();
    List<Integer> evenNumbers = new ArrayList<>();
    Predicate<Integer> isEven = x -> x % 2 == 0;
    Consumer<Integer> ifTrue = element -> evenNumbers.add(element);
    Consumer<Integer> ifFalse = element -> oddNumbers.add(element);
    View.iterate(1, x -> x + 1).take(5).forEachRemaining(isEven, ifTrue, ifFalse);

    Assert.assertEquals(Lists.newArrayList(2, 4), evenNumbers);
    Assert.assertEquals(Lists.newArrayList(1, 3, 5), oddNumbers);
  }

  @Test
  public void testFirst() {

    View<String> view = View.of("1", "2", "3", "4", "5", "6", "7");

    Assert.assertEquals(Result.of("1"), view.first());
    Assert.assertEquals(Result.of("2"), view.first());
    Assert.assertEquals(Result.of("3"), view.first());
    Assert.assertEquals(Result.of("4"), view.first());
    Assert.assertEquals(Result.of("5"), view.first());
    Assert.assertEquals(Result.of("6"), view.first());
    Assert.assertEquals(Result.of("7"), view.first());
    Assert.assertFalse(view.first().isSuccess());
  }

  @Test
  public void testLast() {

    View<String> view = View.of("1", "2", "3", "4", "5", "6", "7");

    Assert.assertEquals(Result.of("7"), view.last());
    Assert.assertFalse(view.first().isSuccess());
  }

  @Test
  public void testRetain() {

    View<Integer> view = View.of(1, 2, 3, 4, 5, 6, 7);
    List<Integer> even = view.retain(x -> x % 2 == 0).toList();

    Assert.assertEquals(Lists.newArrayList(2, 4, 6), even);
  }

  @Test
  public void testDiscard() {

    View<Integer> view = View.of(1, 2, 3, 4, 5, 6, 7);
    List<Integer> even = view.discard(x -> x % 2 != 0).toList();

    Assert.assertEquals(Lists.newArrayList(2, 4, 6), even);
  }

  @Test
  public void testToStringWithoutPrefixOrSuffix() {

    View<Integer> view = View.of(1, 2, 3, 4, 5, 6, 7);
    String str = view.toString(", ");

    Assert.assertEquals("1, 2, 3, 4, 5, 6, 7", str);
  }

  @Test
  public void testToStringWithPrefixAndSuffix() {

    View<Integer> view = View.of(1, 2, 3, 4, 5, 6, 7);
    String str = view.toString(", ", "{", "}");

    Assert.assertEquals("{1, 2, 3, 4, 5, 6, 7}", str);
  }

  @Test
  public void testJoinViewOfViews() {

    String actual = View.of(Lists.newArrayList(View.of("a", "ab"), View.of("abc", "abcd", "abcde"))).flatten(l -> l)
        .join(", ", "{", "}");
    String expected = "{a, ab, abc, abcd, abcde}";

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testJoinEmptyView() {

    String actual = View.of().join(", ", "{", "}");
    String expected = "";

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testDivideEmptyView() {

    View<Integer> view = View.of();
    Map.Entry<List<Integer>, List<Integer>> evenAndOdd = view.divide(x -> x % 2 != 0);

    List<Integer> even = evenAndOdd.getKey();
    List<Integer> odd = evenAndOdd.getValue();

    Assert.assertEquals(Lists.newArrayList(), even);
    Assert.assertEquals(Lists.newArrayList(), odd);
  }

  @Test
  public void testDivideEvenAndOdd() {

    View<Integer> view = View.of(1, 2, 3, 4, 5, 6, 7);
    Map.Entry<List<Integer>, List<Integer>> evenAndOdd = view.divide(x -> x % 2 != 0);

    List<Integer> even = evenAndOdd.getKey();
    List<Integer> odd = evenAndOdd.getValue();

    Assert.assertEquals(Lists.newArrayList(1, 3, 5, 7), even);
    Assert.assertEquals(Lists.newArrayList(2, 4, 6), odd);
  }

  @Test
  public void testDivideOddAndEven() {

    View<Integer> view = View.of(1, 2, 3, 4, 5, 6, 7);
    Map.Entry<List<Integer>, List<Integer>> evenAndOdd = view.divide(x -> x % 2 == 0);

    List<Integer> odd = evenAndOdd.getKey();
    List<Integer> even = evenAndOdd.getValue();

    Assert.assertEquals(Lists.newArrayList(1, 3, 5, 7), even);
    Assert.assertEquals(Lists.newArrayList(2, 4, 6), odd);
  }

  @Test
  public void testDivide() {

    View<Integer> view = View.of(1, 2, 3, 4, 5, 6, 7);
    Map.Entry<List<Integer>, List<Integer>> evenAndOdd = view.divide();

    List<Integer> even = evenAndOdd.getKey();
    List<Integer> odd = evenAndOdd.getValue();

    Assert.assertEquals(Lists.newArrayList(1, 3, 5, 7), even);
    Assert.assertEquals(Lists.newArrayList(2, 4, 6), odd);
  }

  @Test
  public void testSampleIsSmallerThanListSize() {

    List<Integer> sample = View.iterate(1, x -> x + 1).take(3).sample(5);

    Assert.assertEquals(Lists.newArrayList(1, 2, 3), sample);
  }

  @Test
  public void testSampleIsEqualToListSize() {

    List<Integer> sample = View.iterate(1, x -> x + 1).take(5).sample(5);

    Assert.assertEquals(Lists.newArrayList(1, 2, 3, 4, 5), sample);
  }

  @Test
  public void testSampleIsLargerThanListSize() {

    List<Integer> sample = View.iterate(1, x -> x + 1).take(5).sample(1);

    Assert.assertEquals(1, sample.size());
    Assert.assertTrue(1 <= sample.get(0) && sample.get(0) <= 5);
  }
}