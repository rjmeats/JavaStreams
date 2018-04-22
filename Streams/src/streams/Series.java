package streams;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class Series {

	public static void main(String argv[]) {

		int howMany = 30;
		int seed = 1;
		Stream<Integer> s = Stream.iterate(seed, new FibonacciSeries());			// seed value is applied to stream as first item, before apply has ever been called. 		
		s.limit(howMany).forEachOrdered(System.out::println);

		System.out.println();
		
		FibonacciSeries2.FibonacciNumber seed2 = new FibonacciSeries2.FibonacciNumber(0, 1, 0);
		Stream<FibonacciSeries2.FibonacciNumber> s2 = Stream.iterate(seed2, new FibonacciSeries2()); 		
		s2.limit(howMany).forEachOrdered(System.out::println);
	}
}

class FibonacciSeries implements UnaryOperator<Integer> {
	
	int m_previous;
	int m_count;
	
	FibonacciSeries() {
		m_previous = 0;
		m_count = 0;
	}
	
	public Integer apply(Integer seed) {
		m_count++;
		int latest = (seed == 0) ? 1 : m_previous + seed;		
		m_previous = seed;
//		System.out.println("In apply : count=" + m_count + ", seed=" + seed + ", return= " + latest);		
		return latest;
	}
	
}

class FibonacciSeries2 implements UnaryOperator<FibonacciSeries2.FibonacciNumber>  {
	int m_previous;
	int m_count;
	
	FibonacciSeries2() {
		m_previous = 0;
		m_count = 0;
	}
	
	static class FibonacciNumber {
		int m_index;
		int m_value;
		int m_previous;
		
		FibonacciNumber(int index, int value, int previous) {
			m_index = index;
			m_value = value;
			m_previous = previous;
		}
		
		public String toString() {
			double ratio = m_previous == 0 ? 0 : 1.0 * m_value / m_previous;
			return m_index + " : " + m_value + " (" + m_previous + ") + ratio: " + (m_previous != 0 ? ("" + ratio) : "-");
		}
	}

	public FibonacciNumber apply(FibonacciNumber seed) {
		m_count++;
		int latest = (seed.m_value == 0) ? 1 : m_previous + seed.m_value;		
		m_previous = seed.m_value;
//		System.out.println("In apply : count=" + m_count + ", seed=" + seed + ", return= " + latest);		
		return new FibonacciNumber(m_count, latest, m_previous);
	}
}

