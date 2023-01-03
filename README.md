# Asterix

![Maven Central](https://img.shields.io/maven-central/v/com.computablefacts/asterix)
![Build Status](https://travis-ci.com/computablefacts/asterix.svg?branch=master)
![codecov](https://codecov.io/gh/computablefacts/asterix/branch/master/graph/badge.svg)

## Adding Asterix to your build

Asterix's Maven group ID is `com.computablefacts` and its artifact ID is `asterix`.

To add a dependency on Asterix using Maven, use the following:

```xml
<dependency>
  <groupId>com.computablefacts</groupId>
  <artifactId>asterix</artifactId>
  <version>0.x</version>
</dependency>
```

## Snapshots

Snapshots of Asterix built from the `master` branch are available through Sonatype
using the following dependency:

```xml
<dependency>
  <groupId>com.computablefacts</groupId>
  <artifactId>asterix</artifactId>
  <version>0.x-SNAPSHOT</version>
</dependency>
```

In order to be able to download snapshots from Sonatype add the following profile
to your project `pom.xml`:

```xml
<profiles>
  <profile>
    <id>allow-snapshots</id>
    <activation>
      <activeByDefault>true</activeByDefault>
    </activation>
    <repositories>
      <repository>
        <id>snapshots-repo</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
        <releases>
          <enabled>false</enabled>
        </releases>
        <snapshots>
          <enabled>true</enabled>
        </snapshots>
      </repository>
    </repositories>
  </profile>
</profiles>
```

## Publishing a new version

Deploy a release to Maven Central with these commands:

```bash
$ git tag <version_number>
$ git push origin <version_number>
```

To update and publish the next SNAPSHOT version, just change and push the version:

```bash
$ mvn versions:set -DnewVersion=<version_number>-SNAPSHOT
$ git commit -am "Update to version <version_number>-SNAPSHOT"
$ git push origin master
```

## Packages Summary

- [Asterix](#asterix).  Core data structures and algorithms.
- [Decima](#decima).  Decima is a proof-of-concept Java implementation of the probabilistic logic programming language ProbLog.
- [Junon](#junon).  Junon is a data transfer object for Java compatible with our platform API.
- [Jupiter](#jupiter).  Jupiter is an easy to use storage layer for Apache Accumulo.
- [Nona](#nona).  Nona is an extensible Excel-like programming language.

## Asterix

[Asterix](/src/com/computablefacts/asterix) contains a few helpers to perform text mining/NLP related tasks :

- Data Structures
    - [Span](#span)
    - [SpanSequence](#spansequence)
    - [ConfusionMatrix](#confusionmatrix)
- Algorithms
    - [StringIterator](#stringiterator)
    - [SnippetExtractor](#snippetextractor)
    - [Codecs](#codecs)
- Text-based user interface
    - [AsciiProgressBar](#asciiprogressbar)
    - [AsciiTable](#asciitable)
- Algorithms
    - [DocSetLabeler](#docsetlabeler)

### Span

A [Span](src/com/computablefacts/asterix/nlp/Span.java) is a fragment of string
with properties/features. Furthermore, this class contains many methods to detect
and deal with overlaps between spans.

```java
// Create a new Span object
Span span=new Span("123456789",2,5);
span.setFeature("has_digits","true");
span.setFeature("has_letters","false");

// Usage
String text=span.text(); // "345"
String rawText=span.rawText(); // "123456789"
Map<String, String> features=span.features(); // {"has_digits":"true", "has_letters":"false"}
```

### SpanSequence

A [SpanSequence](src/com/computablefacts/asterix/nlp/SpanSequence.java) is a list
of spans.

```java
// Create a new SpanSequence object
String text="123456789";

Span span123=new Span(text,0,3);
Span span456=new Span(text,3,6);
Span span789=new Span(text,6,9);

SpanSequence sequence=new SpanSequence(Lists.newArrayList(span123,span456,span789));

// Usage
int size=sequence.size() // 3
Span span=sequence.span(1) // span456
```

### ConfusionMatrix

A [ConfusionMatrix](src/com/computablefacts/asterix/ml/ConfusionMatrix.java) is
a tool to evaluate the accuracy of a classification. The following metrics are
available :

- Matthews Correlation Coefficient
- Accuracy
- Positive Predictive Value (aka. Precision)
- Negative Predictive Value
- Sensitivity (aka. Recall)
- Specificity
- F1 Score
- False Positive Rate
- False Discovery Rate
- False Negative Rate

Furthermore, two functions have been added to compute the following metrics :

- Micro-Average
- Macro-Average

```java
ConfusionMatrix matrix=new ConfusionMatrix("");
matrix.addTruePositives(620);
matrix.addTrueNegatives(8820);
matrix.addFalsePositives(180);
matrix.addFalseNegatives(380);

double mcc=matrix.matthewsCorrelationCoefficient(); // 0.0001
double accuracy=matrix.accuracy(); // 0.000001
double sensitivity=matrix.sensitivity(); // 0.000001
...
```

### StringIterator

A [StringIterator](src/com/computablefacts/asterix/nlp/StringIterator.java)
facilitates iterating over the characters in a string. Furthermore, this class
contains many functions to find a character category : punctuation marks, arrows,
hyphens, apostrophes, bullets, quotation marks, etc.

```java
// Split a string on white-space characters
String text="123 456 789";
SpanSequence sequence=new SpanSequence();
StringIterator iterator=new StringIterator(text);

while(iterator.hasNext()){

  iterator.movePastWhitespace();
  int begin=iterator.position();
  iterator.moveToWhitespace();
  int end=iterator.position();
  
  sequence.add(new Span(text,begin,end));
}

// Here, sequence = ["123", "456", "789"]
```

### SnippetExtractor

A [SnippetExtractor](src/com/computablefacts/asterix/nlp/SnippetExtractor.java)
allows the extraction (from a text) of the snippet that contains the most dense
selection of words (from a given list).

```java
String text=
  "Welcome to Yahoo!, the world’s most visited home page. Quickly find what you’re "+
  "searching for, get in touch with friends and stay in-the-know with the latest news "+
  "and information. CloudSponge provides an interface to easily enable your users to "+
  "import contacts from a variety of the most popular webmail services including Yahoo, "+
  "Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book "+
  "and Outlook.";

String words=Lists.newArrayList("latest","news","CloudSponge");

String snippet=SnippetExtractor.extract(words,text);

// Here, snippet = "...touch with friends and stay in-the-know with the latest news and 
//                  information. CloudSponge provides an interface to easily enable your 
//                  users to import contacts from a variety of the most popular webmail 
//                  services including Yahoo, Gmail and Hotmail/MSN as well as popular 
//                  desktop address books such as Mac..."
```

### Codecs

The [Codecs](src/com/computablefacts/asterix/codecs) package contains helpers to :

- Encode/decode objects to/from JSON strings,
- Encode/decode string to/from Base64,
- Encode a number as a string such that the lexicographic order of the generated
  string is in the same order as the numeric order.

```java
Pair<String, String> pair=new Pair<>("key1","value1");
String json=JsonCodec.asString(pair); // {"key":"key1","value":"value1"}

List<Pair<?, ?>>pairs=Lists.newArrayList(new Pair<>("key1","value1"),new Pair<>("key2",2),new Pair<>("key3",false));
String json=JsonCodec.asString(pairs); // "[{"key":"key1","value":"value1"},{"key":"key2","value":2},{"key":"key3","value":false}]"

Map<String, Object> json=JsonCodec.asObject("{\"key\":\"key1\",\"value\":\"value1\"}");
Collection<Map<String, Object>>json=JsonCodec.asCollection("[{\"key\":\"key1\",\"value\":\"value1\"},{\"key\":\"key2\",\"value\":2},{\"key\":\"key3\",\"value\":false}]");

Base64Codec.decodeB64(Base64.getDecoder(),"dGVzdA=="); // test
Base64Codec.encodeB64(Base64.getEncoder(),"test"); // dGVzdA==

BigDecimalCodec.encode(BigDecimal.valueOf(123456789L)); // ??9123456789*
BigDecimalCodec.encode(BigDecimal.valueOf(-123456789L)); // **0876543210?
```

### AsciiProgressBar

The [AsciiProgressBar](src/com/computablefacts/asterix/console/AsciiProgressBar.java) class
contains helpers to display a textual progress bar while enumerating a bounded or
an unbounded dataset.

```java
// Progress bar on a bounded dataset
AtomicInteger count=new AtomicInteger(0);
AsciiProgressBar.ProgressBar bar=AsciiProgressBar.create();
List<String> list=...
list.peek(e->bar.update(count.incrementAndGet(),list.size())).forEach(System.out::println);

// Progress bar on an unbounded dataset
AsciiProgressBar.IndeterminateProgressBar bar=AsciiProgressBar.createIndeterminate();
Stream<String> stream=...
stream.peek(e->bar.update()).forEach(System.out::println);
bar.complete(); // reset
```

### AsciiTable

The [AsciiTable](src/com/computablefacts/asterix/console/AsciiTable.java) class
contains helpers to display a textual table.

```java
String[][]table=...

// Display a table with header
System.out.println(AsciiTable.format(table,true));

// Display a table without header
System.out.println(AsciiTable.format(table,false));
```

### DocSetLabeler

A highly customizable implementation of
the [DocSetLabeler](src/com/computablefacts/asterix/ml/AbstractDocSetLabeler.java)
[algorithm](https://arxiv.org/abs/1409.7591) :

> An algorithm capable of generating expressive thematic labels for any subset of
> documents in a corpus can greatly facilitate both characterization and navigation
> of document collections.

## Decima

[Decima](/src/com/computablefacts/decima) is a proof-of-concept Java implementation of the probabilistic logic
programming language [ProbLog](https://dtai.cs.kuleuven.be/problog).

This library embeds a Java port of the C# library [BDDSharp](https://github.com/ancailliau/BDDSharp) (
under [MIT licence](https://opensource.org/licenses/mit-license.php)).
BDDSharp is a library for manipulating [ROBDDs](https://en.wikipedia.org/wiki/Binary_decision_diagram) (Reduced Ordered
Binary Decision Diagrams). A good overview of Binary Decision Diagrams can be found
in [Lecture Notes on Binary Decision Diagrams](https://www.cs.cmu.edu/~fp/courses/15122-f10/lectures/19-bdds.pdf)
by Frank Pfenning.

### Usage

ProbLog is a redesign and new implementation of Prolog in which facts and rules can be annotated with probabilities
(ProbLog makes the assumption that all probabilistic facts are mutually independent) by adding a floating-point number
in front of the fact/rule followed by double-colons (
from [ProbLog's site](https://dtai.cs.kuleuven.be/problog/tutorial/basic/05_smokers.html)) :

```
0.3::stress(X) :- person(X).
0.2::influences(X, Y) :- person(X), person(Y).

smokes(X) :- stress(X).
smokes(X) :- friend(X, Y), influences(Y, X), smokes(Y).

0.4::asthma(X) :- smokes(X).

person(éléana).
person(jean).
person(pierre).
person(alexis).

friend(jean, pierre).
friend(jean, éléana).
friend(jean, alexis).
friend(éléana, pierre).
```

The program above encodes a variant of the "Friends & Smokers" problem. The first two rules state that there are two
possible causes for a person X to smoke, namely X having stress, and X having a friend Y who smokes himself and
influences X. Furthermore, the program encodes that if X smokes, X has asthma with probability 0.4.

It is then possible to calculates the probability of the various people smoking and having asthma :

```
smokes(éléana)?
0.342

smokes(jean)?
0.42556811
```

The rules above can also be stored as a YAML file ([example](/src/resources/data/tests/valid-yaml.yml)).
This YAML file can be transpiled into a valid set of rules using
the [Compiler](/src/com/computablefacts/decima/Compiler.java)
tool. One big advantage of this approach is that it allows the user to easily write
unit tests.

```
java -Xms1g -Xmx1g com.computablefacts.decima.Compiler \
     -input "rules.yml" \
     -output "rules-compiled.txt" \
     -show_logs true
```

The [Builder](/src/com/computablefacts/decima/Builder.java) tool allows the user
to automatically generate facts from [ND-JSON](http://ndjson.org/) files containing
one or more JSON objects.

```
java -Xms1g -Xmx1g com.computablefacts.decima.Builder \
     -input "facts.json" \
     -output "facts-compiled.txt" \
     -show_logs true
```

The [Solver](/src/com/computablefacts/decima/Solver.java) tool allows the user to
load facts and rules into a Knowledge Base and query it.

```
java -Xms2g -Xmx4g com.computablefacts.decima.Solver \
     -rules "rules-compiled.txt" \
     -facts "facts-compiled.txt" \
     -queries "queries.txt" \ 
     -show_logs true
```

### Proof-of-Concept

Decima has the ability to perform HTTP calls at runtime to fill the knowledge base
with new facts. The function for that is :

```
fn_http_materialize_facts(https://<base_url>/<namespace>/<class>, <field_name_1>, <field_variable_1>, <field_name_2>, <field_variable_2>, ...)
```

At runtime, the following HTTP query will be performed (with each `field_variable_x`
encoded as a base 64 string) :

```
GET https://<base_url>/<namespace>/<class>?<field_name_1>=<field_variable_1>&<field_name_2>=<field_variable_2>&...
```

The function expects the following JSON in return :

```
[
  {
    "namespace": "<namespace>",
    "class": "<class>",
    "facts": [{
        "field_name_1": "...",
        "field_name_2": "...",
        ...
      }, {
        "field_name_1": "...",
        "field_name_2": "...",
        ...
      },
      ...
    ]
  },
  ...
]
```

An example of use-case, is to merge the content of multiple data sources :

```
// Dataset CRM1 -> 2 clients
clients(FirstName, LastName, Email) :- 
    fn_http_materialize_facts("http://localhost:3000/crm1", "first_name", FirstName, "last_name", LastName, "email", Email).

// Dataset CRM2 -> 3 clients
clients(FirstName, LastName, Email) :- 
    fn_http_materialize_facts("http://localhost:3000/crm2", "first_name", FirstName, "last_name", LastName, "email", Email).

// Merge both datasets
clients(FirstName, LastName, Email)?

// Result (example)
clients("Robert", "Brown", "bobbrown432@yahoo.com").
clients("Lucy", "Ballmer", "lucyb56@gmail.com").
clients("Roger", "Bacon", "rogerbacon12@yahoo.com").
clients("Robert", "Schwartz", "rob23@gmail.com").
clients("Anna", "Smith", "annasmith23@gmail.com").
```

## Junon

[Junon](/src/com/computablefacts/junon) is a data transfer object for Java compatible with our platform API.

## Jupiter

[Jupiter](/src/com/computablefacts/jupiter)  implements 3 data stores on top of Apache Accumulo : one for blobs, one for terms and facts (a fact is a predicate expression that makes a declarative statement about a problem domain) and one for JSON objects.

These data stores are not meant to be efficients but are intended to be easy to use.

### BlobStore

The [BlobStore](/src/com/computablefacts/jupiter/storage/blobstore) API allows
your application to persist data objects. Methods are available to write and read
opaque Strings, JSON and Files.

```java
Configurations configurations = ...;
BlobStore blobStore = new BlobStore(configurations, "blobs" /* table name */);

// Write blobs
Set<String> noBlobSpecificVizLabels = Sets.newHashSet();

try (BatchWriter writer = blobStore.writer()) {
    
    String str = ...;
    blobStore.putString(writer, "my_strings", UUID.randomUUID().toString(), noBlobSpecificVizLabels, str);

    Map<String, Object> json = ...;
    blobStore.putJson(writer, "my_jsons", UUID.randomUUID().toString(), noBlobSpecificVizLabels, json);

    File file = ...;
    blobStore.putFile(writer, "my_files", UUID.randomUUID().toString(), noBlobSpecificVizLabels, file);
}

// Read blobs
// For convenience, <dataset>_RAW_DATA authorizations are automatically added to each blob
Authorizations auths = new Authorizations("MY_STRINGS_RAW_DATA", "MY_JSONS_RAW_DATA", "MY_FILES_RAW_DATA");

blobStore.strings(auths, "my_strings", null, null).forEachRemaining(blob -> ...);
blobStore.jsons(auths, "my_jsons", null, null).forEachRemaining(blob -> ...);
blobStore.files(auths, "my_files", null, null).forEachRemaining(blob -> ...);
```

Note that it is possible to filter-out JSON fields at the tserver level before
returning the JSON object to the client.

```java
Map<String, Object> json = new HashMap<>();
json.put("first_name", "john");
json.put("last_name", "doe");
json.put("email", "john.doe@gmail.com");
json.put("password", "&N?8LXtT7&f4@nH$");

try (BatchWriter writer = blobStore.writer()) {
    blobStore.putJson(writer, "my_jsons", UUID.randomUUID().toString(), Sets.newHashSet(), json);
}

Set<String> fieldsToKeep = Sets.newHashSet("first_name", "last_name", "email");
Optional<Value> blob = blobStore.jsons(scanner, "my_jsons", null, fieldsToKeep).first();

json.remove("password");
Assert.assertEquals(json, Codecs.asObject(blob.get().toString()));
```

### TermStore

The [TermStore](/src/com/computablefacts/jupiter/storage/termstore) API allows
your application to persist buckets of key-value pairs. Numbers and dates are
automatically lexicoded to maintain their native Java sort order.

```java
Configurations configurations = ...;
TermStore termStore = new TermStore(configurations, "terms" /* table name */);

Map<String, Object> bucket = new HashMap<>();
bucket.put("first_name", "john");
bucket.put("last_name", "doe");
bucket.put("age", 37);
bucket.put("last_seen", new Date());

String dataset = "my_buckets";
String bucketId = UUID.randomUUID().toString();
Set<String> bucketSpecificLabels = Sets.newHashSet("MY_BUCKETS_RAW_DATA");

// Write terms
try (BatchWriter writer = termStore.writer()) {
    
    bucket.entrySet().forEach(keyValuePair -> {
        
        String field = keyValuePair.getKey();
        Object value = keyValuePair.getValue();
        Set<String> fieldSpecificLabels = Sets.newHashSet();
        
        boolean isOk = termStore.put(writer, dataset, bucketId, key, value, 1, bucketSpecificLabels, fieldSpecificLabels);
    });
}

Authorizations auths = new Authorizations("MY_BUCKETS_RAW_DATA");

/* Get the number of distinct buckets containing a given term */
        
// Wildcard query
termStore.termCardinalityEstimationForBuckets(scanner, dataset, "joh*").forEachRemaining(estimation -> ...);

// Range query
termStore.termCardinalityEstimationForBuckets(scanner, dataset, null, 30, 40).forEachRemaining(estimation -> ...);

/* Get buckets ids containing a given term */

// Wildcard query
termStore.bucketsIds(scanner, dataset, "joh*").forEachRemaining(term -> ...);
        
// Range query    
termStore.bucketsIds(scanner, dataset, null, 30, 40, null).forEachRemaining(term -> ...);
```

## Nona

[Nona](/src/com/computablefacts/nona) is an extensible Excel-like programming language.