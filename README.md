# JavaStreams
Some examples of using various Java Streams facilities, lambdas and functional interfaces to process various sets of data:
* English Premier League football results from 2016-17 ([source](http://www.football-data.co.uk/englandm.php))
* 2017 UK General Election results ([source](https://www.electoralcommission.org.uk/our-work/our-research/electoral-data/electoral-data-files-and-reports))
* example series generators including the [Fibonacci series](https://en.wikipedia.org/wiki/Fibonacci_number), [factorial](https://en.wikipedia.org/wiki/Factorial) series and [prime number](https://en.wikipedia.org/wiki/Prime_number) series 

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
  * map(), mapToObj() - an IntStream method
  * flatMap()
  * distinct()
  * sorted()
  * parallel(), sequential()
* terminal stream operations:
  * forEachOrdered(), forEach()
  * count(), min(), max()
  * sum(), average(), summaryStatistics() - IntStream methods
  * allMatch(), anyMatch(), noneMatch()
  * findAll(), findAny()
  * collect()
* stream [Collectors](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Collectors.html) operations:
  * Collectors.toList() and .toSet()
  * Collectors.summingInt()
  * Collectors.averagingInt()
  * Collectors.maxBy() and .minBy()
  * Collectors.counting()
  * Collectors.joining()
  * Collectors.groupingBy()
  * Collectors.partitioningBy()
  * Collectors.reducing()
  * Collectors.toMap()
  * Collectors.toCollection()
  * Collectors.collectingAndThen()
  * and also a bespoke [Collector](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Collector.html) generated by calling Collector.of() with a class implementing [Supplier](https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html), [BiConsumer](https://docs.oracle.com/javase/8/docs/api/java/util/function/BiConsumer.html) and [BinaryOperator](https://docs.oracle.com/javase/8/docs/api/java/util/function/BinaryOperator.html) interfaces

The examples also include cases of:
* a [Predicate](https://docs.oracle.com/javase/8/docs/api/java/util/function/Predicate.html) object
  * including a use of its .or() method
* a [UnaryOperator](https://docs.oracle.com/javase/8/docs/api/java/util/function/UnaryOperator.html) object (extends [Function](https://docs.oracle.com/javase/8/docs/api/java/util/function/Function.html))
* a [Supplier](https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html) object
* a [Comparator](https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html) object, created using the combination of:
  * Comparator.comparing()
  * thenComparing()
  * reversed()
* a [Comparator](https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html) object, created using a lambda expression
* the [Objects](https://docs.oracle.com/javase/8/docs/api/java/util/Objects.html) object
* [BigInteger](https://docs.oracle.com/javase/8/docs/api/java/math/BigInteger.html) and [BigDecimal](https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html) objects
* [Optional](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) objects
