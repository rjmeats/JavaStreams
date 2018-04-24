package streams;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Series {

	public static void main(String argv[]) {

		int howMany = 30;
		int seed = 1;
		Stream<Integer> s = Stream.iterate(seed, new FibonacciSeries());			// seed value is applied to stream as first item, before apply has ever been called. 		
		s.limit(howMany).forEachOrdered(System.out::println);

		System.out.println();
		
		FibonacciSeries2.FibonacciNumber seed2 = new FibonacciSeries2.FibonacciNumber(1, 1, 0);
		Stream<FibonacciSeries2.FibonacciNumber> s2 = Stream.iterate(seed2, new FibonacciSeries2()); 		
		s2.limit(howMany).forEachOrdered(System.out::println);

		System.out.println();
		howMany = 20;
		double seed3 = 0.0;
		Stream<Double> s3 = Stream.iterate(seed3, new ApproxE());			// seed value is applied to stream as first item, before apply has ever been called. 		
		s3.limit(howMany).forEachOrdered(System.out::println);
		
		System.out.println();
		howMany = 500;
		BigDecimal seed4 = BigDecimal.ZERO;
		Stream<BigDecimal> s4 = Stream.iterate(seed4, new BDApproxE());			// seed value is applied to stream as first item, before apply has ever been called. 		
		s4.limit(howMany).forEachOrdered(System.out::println);
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
		int index = seed.m_index+1;
		int latest = (seed.m_value == 0) ? 1 : m_previous + seed.m_value;		
		m_previous = seed.m_value;
//		System.out.println("In apply : count=" + m_count + ", seed=" + seed + ", return= " + latest);		
		return new FibonacciNumber(index, latest, m_previous);
	}
}

/**
 * Approximations to the value of e (2.18281828 ...) produced using the formula:
 * 
 *   e = 2 + 1/2! + 1/3! + 1/4! + ...
 *   
 *   To 50 decimal places, e = 2.71828182845904523536028747135266249775724709369995 from https://en.wikipedia.org/wiki/E_(mathematical_constant)
 */
class ApproxE implements UnaryOperator<Double> {		
		
	long m_previousFactorial;
	int m_count;
	
	ApproxE() {
		m_previousFactorial = 0;
		m_count = 0;
	}
	
	public Double apply(Double seed) {
		m_count++;
		double latest = 0;
		if(m_count == 1) {
			latest = 2;
			m_previousFactorial = 1;
		}
		else {
			long nextFactorial = m_previousFactorial * m_count;
			latest = seed + (1.0/nextFactorial);
			m_previousFactorial = nextFactorial;
		}				
		return latest;
	}
}

// Try using BigDecimal instead of double to get lots more digits
// https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html

class BDApproxE implements UnaryOperator<BigDecimal> {		
	
	BigDecimal m_previousFactorial;
	int m_count;
	
	static int scale = 1000;
	
	BDApproxE() {
		m_previousFactorial = new BigDecimal(0);
		m_count = 0;
	}
	
	public BigDecimal apply(BigDecimal seed) {
		m_count++;
		BigDecimal latest = new BigDecimal(0);
		if(m_count == 1) {
			latest = new BigDecimal(2);
			m_previousFactorial = new BigDecimal(1);
		}
		else {
			BigDecimal nextFactorial = m_previousFactorial.multiply(new BigDecimal(m_count));
			BigDecimal reciprocal = BigDecimal.ONE.divide(nextFactorial, scale, RoundingMode.HALF_DOWN);
			// System.out.println("Reciprocal of " + nextFactorial + ": " + reciprocal.stripTrailingZeros());			
			latest = seed.add(reciprocal);
			m_previousFactorial = nextFactorial;
		}				
		return latest;
	}
}
