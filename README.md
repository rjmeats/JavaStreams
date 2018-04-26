# JavaStreams
Some examples of using various Java Streams facilities and lambdas / functional interfaces to process various sets of data:
* English Premier League football results from 2016-17 ([source](http://www.football-data.co.uk/englandm.php))
* 2017 UK General Election results ([source](https://www.electoralcommission.org.uk/our-work/our-research/electoral-data/electoral-data-files-and-reports))
* [Fibonacci series](https://en.wikipedia.org/wiki/Fibonacci_number) and other example series generators

[Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html) facilities demonstrated include:

* stream generation operations:
  * stream() - for Collection-based objects
  * IntStream.rangeClosed()
  * Stream.iterate()
  * Stream.generate()
  * Stream.empty()
  * Stream.of()
  * Stream.builder()
  * Stream.concat()
  * Arrays.stream()
  * Files.lines()
  * String.chars()
* intermediate stream processing operations:
  * limit()
  * skip()
  * peek()
  * filter()
  * map()
  * mapToObj() - an IntStream method
  * flatMap()
  * distinct()
  * sorted()
* terminal stream operations:
  * forEachOrdered()
  * count()
  * collect()
* stream [Collectors](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Collectors.html) operations:
  * Collectors.summingInt()
  * Collectors.groupingBy()
  * Collectors.toList()

Also showing examples of:
* a [Predicate](https://docs.oracle.com/javase/8/docs/api/java/util/function/Predicate.html) object
  * including a use of its .or() method
* a [UnaryOperator](https://docs.oracle.com/javase/8/docs/api/java/util/function/UnaryOperator.html) object (extends [Function](https://docs.oracle.com/javase/8/docs/api/java/util/function/Function.html))
* a [Comparator](https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html) object, created using the combination of:
  * Comparator.comparing()
  * thenComparing()
  * reversed()
* a [Comparator](https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html) object, created using a lambda expression
* the [Objects](https://docs.oracle.com/javase/8/docs/api/java/util/Objects.html) object
* [BigInteger](https://docs.oracle.com/javase/8/docs/api/java/math/BigInteger.html) and [BigDecimal](https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html) objects
