package streams;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Comparator;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.IntSummaryStatistics;
import java.util.Formatter;

public class GeneralElection {

	public static void main(String argv[]) {

		// Data file from http://www.football-data.co.uk/englandm.php - see notes.txt in data folder		
		List<CandidateResult> results = readResultsFile("data\\UKGeneralElection2017.csv");
		
		if(results == null) return;
		
		System.out.println("Read in " + results.size() + " candidate results");
		
		long constituencyCount = results.stream().map(CandidateResult::constituency).distinct().count();
		long parties = results.stream().map(CandidateResult::partyIdentifier).distinct().count();
		long candidateSurnames = results.stream().map(CandidateResult::surname).distinct().count();
		long candidateFirstNames = results.stream().map(CandidateResult::firstName).distinct().count();
		long candidateNames = results.stream().map(cr -> cr.firstName() + " " + cr.surname()).distinct().count();
		
		System.out.println();
		System.out.println("Constituencies:        " + constituencyCount);
		System.out.println("Parties:               " + parties);
		System.out.println("Distinct surnames:     " + candidateSurnames);
		System.out.println("Distinct first names:  " + candidateFirstNames);
		System.out.println("Distinct names:        " + candidateNames);
		
		int totalVotes = results.stream().collect(Collectors.summingInt(CandidateResult::votes));

		System.out.println();
		System.out.println("Total votes : " + totalVotes);

		// List all the parties
		//System.out.println();
		//results.stream().map(CandidateResult::partyIdentifier).distinct().sorted().forEachOrdered(System.out::println);
		
		// Run some other Collections as example usage
		boolean doCollectorsExercise = false;
		if(doCollectorsExercise) {
			CollectorsExercise.exerciseCollectors(results);
		}
		
		// Generate a set of constituencies
		List<Constituency> constituencies = 
		results.stream()
			.collect(Collectors.groupingBy(CandidateResult::constituency))
			.entrySet().stream()
			.map(x -> Constituency.asConstituency(x.getKey(), x.getValue()))
			.collect(Collectors.toList());

		System.out.println("Generated " + constituencies.size() + " constituencies");
		System.out.println();
		Map<String, List<Constituency>> partyWinners = constituencies.stream().collect(Collectors.groupingBy(Constituency::winningParty));
		for(Map.Entry<String, List<Constituency>> entry : partyWinners.entrySet()) {
			System.out.println("  " + entry.getKey() + " : " + entry.getValue().size() + " seats");
		}

		System.out.println();
		Map<Country, List<Constituency>> byCountry = constituencies.stream().collect(Collectors.groupingBy(Constituency::country));
		for(Map.Entry<Country, List<Constituency>> entry : byCountry.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue().size() + " constituencies");
			Map<String, List<Constituency>> thisCountryWinners = entry.getValue().stream().collect(Collectors.groupingBy(Constituency::winningParty));
			for(Map.Entry<String, List<Constituency>> entry2 : thisCountryWinners.entrySet()) {
				System.out.println("  " + entry2.getKey() + " : " + entry2.getValue().size() + " seats");
			}
		}
		
		// Constituencies with smallest number of votes for the winner
		System.out.println();
		constituencies.stream().sorted(Comparator.comparing(Constituency::winningVotes)).limit(10).forEachOrdered(System.out::println);

		// Constituencies with largest number of votes for the winner
		System.out.println();
		constituencies.stream().sorted(Comparator.comparing(Constituency::winningVotes).reversed()).limit(10).forEachOrdered(System.out::println);

		// Constituencies with the smallest majority
		System.out.println();
		constituencies.stream().sorted(Comparator.comparing(Constituency::majority)).limit(10).forEachOrdered(System.out::println);

		// Constituencies where the winner has the smallest proportion of the vote
		System.out.println();
		constituencies.stream().sorted(Comparator.comparing(Constituency::winningShare)).limit(10).forEachOrdered(System.out::println);

		// Constituencies where the last-placed candidate has the largest proportion of the vote
		System.out.println();
		constituencies.stream().sorted(Comparator.comparing(Constituency::losingShare).reversed()).limit(10)
				.forEachOrdered(c -> System.out.println(c.toString() + " [ " + Math.round(c.losingShare()) + "% " + c.m_results.get(c.m_results.size()-1) + "]"));

		// Map<String, PartyResult> mParties = lConstituencies.stream().parallel().collect(PartyResult.ResultCollector.getCollector());
		Map<String, PartyResult> mParties = constituencies.stream().collect(PartyResult.ResultCollector.getCollector());
		System.out.println();
		List<PartyResult> lParties = mParties.values().stream().sorted((x,y) -> y.m_wins - x.m_wins).collect(Collectors.toList());
		System.out.println("Produced party result for " + mParties.size() + " parties");
		System.out.println();
		System.out.println("Parties with a win or 100000 votes:");
		lParties.stream().filter(p -> p.m_wins > 0 || p.m_votes >= 100000).forEachOrdered(System.out::println);
		
		// Use elsewhere via CSV dump
		dumpOutputFile(results, constituencies);
	}

	static List<CandidateResult> readResultsFile(String path) {

		List<CandidateResult> l = null;
		
		// Not clear what the characterset of the soorce file is. Default for Files.lines is UTF-8, which causes a Malformed exception
		// to be reported from a buffered reader. This seems to arise on a line with a smart quote:
		// 	E14000543,31,Barrow and Furness,O’HARA,Robert  (Known As Rob),Green Party,Green Party,375
		// Using ISO_8859_1 doesn't crash, but the smart quote doesn't come through. Not clear if other characters meet the same fate.
		
		try (Stream<String> stream = Files.lines(Paths.get(path), StandardCharsets.ISO_8859_1)) {
//		try (Stream<String> stream = Files.lines(Paths.get(path), StandardCharsets.UTF_8)) {

			l = stream.map(CandidateResult::fromLine).filter(Objects::nonNull).collect(Collectors.toList());
//			stream.peek(System.out::println).forEachOrdered(System.out::println);
		} catch (IOException e) {			
			System.err.println("Failed to load data from file: " + e.getMessage());
			return null;
		}
		
		return l;
	}
	
	static void dumpOutputFile(List<CandidateResult> results, List<Constituency> constituencies) {
		
		// Augment each candidate-result record with some derived info, and then output a CSV file with this extended information.
		// - did the candidate win or lose, and what was the position number, how many candidates were there ? 
		// - which was the winning party ?
		// - which country is the constituency in
		// - a simplified party identifier, covering lots of smaller parties with 'Other'
		
		List<AugmentedCandidateResult> l = constituencies.stream().flatMap(c-> AugmentedCandidateResult.augmentResultsForConstituency(c).stream()).collect(Collectors.toList());
		System.out.println();
		System.out.println("Produced " + l.size() + " augmented results");
		System.out.println();
		
		String nl = System.lineSeparator();
		StringBuilder sb = new StringBuilder();
		sb.append(AugmentedCandidateResult.toCSVHeader()).append(nl);
		l.stream().limit(10000).forEachOrdered(a -> sb.append(a.toCSV()).append(nl));
		
		String outputFolderName = "output";
		String outputFileName = outputFolderName + "/" + "ExtendedUKGeneralElection2017.csv";
		File logsFolder = new File(outputFolderName);
		if(logsFolder.exists() && logsFolder.isDirectory() && logsFolder.canWrite()) {
			System.out.println("Augmented CSV file produced in file: " + outputFileName);
			writeFile(outputFileName, sb.toString());
		}
		else {
			System.out.println("No augmented CSV file produced - no " + outputFolderName + " folder present");
		}
	}

	private static void writeFile(String filename, String s) {
	    try (BufferedWriter bw = Files.newBufferedWriter(new File(filename).toPath(), StandardCharsets.ISO_8859_1)) {
			bw.write(s, 0, s.length());
//			bw.newLine();
	    }
	    catch(Exception e) {
	        System.err.println("Failed to write to: " + filename + " " + e.getMessage());
	    }
	}
}

class CandidateResult {

	// RESULTS,,,,,,,
	// ONS Code,PANO,Constituency,Surname,First name,Party,Party Identifer,Valid votes
	// E14000530,7,Aldershot,WALLACE,Donna Maria,Green Party,Green Party,1090
	// E14000530,7,Aldershot,SWALES,John Roy,UK Independence Party (UKIP),UKIP,1796

	static CandidateResult fromLine(String line) {
		// Ignore headings and blank lines

		if(line.trim().length() == 0) return null;
		if(line.startsWith("RESULTS")) return null;
		if(line.startsWith("ONS Code")) return null;
		
		String fields[] = line.split(",");	// Assume no commas in data, no quotes around values, no trimming of fields needed
		if(fields.length < 8) {
			System.err.println("Error - data line with insufficient fields: " + line);
			return null;
		}
		else if(fields.length > 8 && line.indexOf("\"") != -1) {
			// Not simple comma-separated, some non-separator commas are being protected by double quotes around a field.
			// Are there any cases with more than one pair of double quotes ?
			if(line.length() != line.replaceAll("\"",  "").length()+2) {
				System.err.println("Complex Protected line ignored: " + line);
				return null;
			}
			else {
				String quotedField = line.substring(line.indexOf("\""), line.lastIndexOf("\"")+1);
				String line2 = line.replace(quotedField, quotedField.replaceAll(",", "@"));
				String fields2[] = line2.split(",");
				if(fields2.length != 8) {
					System.err.println("Error - processing protected line failed: " + line);
					return null;
				} else {
					for(int i = 0; i < fields2.length; i++) {
						fields2[i] = fields2[i].replaceAll("@", ",");		// Stream way of doing this ?
					}
					fields = fields2;
				}
			}			
		}
		
		CandidateResult cr = new CandidateResult();
		
		cr.m_ONSCode = fields[0].trim();
		cr.m_PANO = fields[1].trim();
		cr.m_constituency = fields[2].trim();
		cr.m_surname = fields[3].trim();
		cr.m_firstname = fields[4].trim();
		cr.m_party = fields[5].trim();
		cr.m_partyIdentifier = fields[6].trim();
		try {
			cr.m_votes = Integer.parseInt(fields[7]);
		} catch(Exception e) {
			System.err.println("Error parsing data line - invalid data: " + line);
			return null;
		}		
		
		return cr;
	}
	
	String m_ONSCode;
	String m_PANO;
	String m_constituency;
	String m_surname;
	String m_firstname;
	String m_party;				// Full of inconsistencies
	String m_partyIdentifier;
	int m_votes;
	
	String constituency() { return m_constituency; }
	String surname() { return m_surname; }
	String firstName() { return m_firstname; }
	String party() { return m_party; }
	String partyIdentifier() { return m_partyIdentifier; }
	int votes() { return m_votes; }
	
	public String toString() {
		return constituency() + " / " + firstName() + " " + surname() + " / " + party() + " : " + votes();
	}
}

// Class for outputting CSV version of candidate data with some extra fields added compared to the input file fields
class AugmentedCandidateResult {
	CandidateResult m_basicResult;
	int m_position;
	String m_outcome;	// Win or Loss
	double m_voteShare;
	int m_majority;
	String m_simplifiedParty;
	int m_numCandidates;
	String m_country;
	
	// Produce a list of augmented results for a constituency
	static List<AugmentedCandidateResult> augmentResultsForConstituency(Constituency constituency) {		
		List<AugmentedCandidateResult> l = 
				IntStream.rangeClosed(1, constituency.m_results.size())
				.mapToObj(pos -> new AugmentedCandidateResult(constituency, pos, constituency.m_results.get(pos-1)))
				.collect(Collectors.toList());
		return l;
	}
	
	AugmentedCandidateResult(Constituency constituency, int position, CandidateResult result) {
		m_basicResult = result;
		m_position = position;
		m_outcome = (position == 1) ? "Winner" : "Loser";
		m_voteShare = result.m_votes * 1.0 / constituency.m_totalVotes; 
		m_majority = (position == 1) ? constituency.m_majority : 0;
		m_simplifiedParty = simplifyParty();
		m_numCandidates = constituency.m_results.size();
		m_country = constituency.country().toString();
	}

	// List of main parties, allowing an 'Other' category to be used to cover everyone else
	static List<String> s_keepParties = Arrays.asList("Conservative", "Labour", "Liberal Democrats", "SNP", "UKIP", "Green Party", 
													  "DUP", "Sinn Féin", "Plaid Cymru", "SDLP", "UUP", "Alliance", "Independent");
	String simplifyParty() {
		String sp = "";
		if(m_position == 1) {
			// Keep the party name for winners
			sp = m_basicResult.m_partyIdentifier;
		} else {
			// Convert non-mainstream party names to 'Other'
			long matches = s_keepParties.stream().filter(s -> s.equalsIgnoreCase(m_basicResult.m_partyIdentifier)).count();
			if(matches == 1) {
				sp = m_basicResult.m_partyIdentifier;
			}
			else {
				sp = "Other";
			}
		}

		// Remove trailing word 'Party' if present
		sp = sp.trim();
		if(sp.endsWith("Party")) {
			sp = sp.replaceAll("Party", "").trim();
		}
		return sp;
	}
	
	// Generate CSV output for the augmented result records
	static String toCSVHeader() {
		StringBuilder sb = new StringBuilder();
		// Output same heaer fields as in the original file, separated by commas
		sb.append("ONS Code").append(",");
		sb.append("PANO").append(",");
		sb.append("Constituency").append(",");
		sb.append("Surname").append(",");
		sb.append("First name").append(",");
		sb.append("Full Party").append(",");
		sb.append("Valid votes").append(",");
				
		// Append augmented fields
		sb.append("Position").append(",");
		sb.append("Outcome").append(",");
		sb.append("Share").append(",");
		sb.append("Majority").append(",");
		sb.append("Candidate count").append(",");
		sb.append("Party").append(",");
		sb.append("Country");

		return sb.toString();
	}
	
	String toCSV() {
		StringBuilder sb = new StringBuilder();
		CandidateResult r = m_basicResult;
		// Output same fields as in the original file, separated by commas, protected by doublequotes if the item contains a comma
		sb.append(protect(r.m_ONSCode)).append(",");
		sb.append(protect(r.m_PANO)).append(",");
		sb.append(protect(r.m_constituency)).append(",");
		sb.append(protect(r.m_surname)).append(",");
		sb.append(protect(r.m_firstname)).append(",");
		sb.append(protect(r.m_partyIdentifier)).append(",");
		sb.append(r.m_votes).append(",");
				
		// Append augmented fields
		sb.append(m_position).append(",");
		sb.append(m_outcome).append(",");
		Formatter fmt = new Formatter(); sb.append(fmt.format("%.3f",  m_voteShare).toString()).append(","); fmt.close();
		sb.append(m_majority).append(",");
		sb.append(m_numCandidates).append(",");
		sb.append(m_simplifiedParty).append(",");
		sb.append(m_country);

		return sb.toString();
	}
	
	static char dq = '\"';
	static String protect(String in) {
		String out = in.trim();
		if(out.indexOf(dq) != -1) {
			// OK if just at start and end, otherwise trouble
			if(out.charAt(0) == dq && out.charAt(out.length()-1) == dq && out.replaceAll(dq+"", "").length() == out.length()-2) {
				// Retain double quotes
			}
			else {
				System.err.println("Internal double quote in field: " + in);
			}				
		}
		else if(in.indexOf(",") != -1) {
			out = dq + in + dq;
		}
		return out;
	}
	
}

enum Country {
	
	ENGLAND("England"), SCOTLAND("Scotland"), WALES("Wales"), NORTHERN_IRELAND("Northern Ireland");
	
	String m_label; 
	Country(String label) {
		m_label = label;
	}
	
	public String toString() {
		return m_label;
	}
}

class Constituency {
	
	static Constituency asConstituency(String constituencyName, List<CandidateResult> lResults) {
		return new Constituency(constituencyName, lResults);
	}
	
	String m_name;
	Country m_country;
	List<CandidateResult> m_results;		// Sorted by votes, highest first
	int m_totalVotes;
	String m_winningParty;
	String m_winningCandidate;
	int m_winningVotes;
	int m_majority;
	double m_winningShare;					// Proportion of votes for the winner
	double m_losingShare;					// Proportion of votes for last place

	Country country() { return m_country; }
	String winningParty() { return m_winningParty; }
	int winningVotes() { return m_winningVotes; }
	int majority() { return m_majority; }
	double winningShare() { return m_winningShare; }
	double losingShare() { return m_losingShare; }
	
	Constituency(String constituencyName, List<CandidateResult> lResults) {
		m_name = constituencyName;
		m_results = new ArrayList<>(lResults);
		m_results.sort((x,y) -> y.m_votes - x.m_votes);
		
		m_totalVotes = m_results.stream().collect(Collectors.summingInt(CandidateResult::votes));
		CandidateResult winner = m_results.get(0); 
		m_winningParty = winner.partyIdentifier();
		m_winningCandidate = winner.surname() + ", " + winner.firstName();
		m_winningVotes = winner.votes();
		m_majority = winner.votes() - ((m_results.size() > 1) ? m_results.get(1).votes() : 0);
		m_winningShare = winner.votes() * 100.0 / m_totalVotes;	
		m_losingShare = m_results.get(m_results.size()-1).votes() * 100.0 / m_totalVotes;	
		
		assignCountry();
	}
	
	void assignCountry() {
		m_country = null;
		// Assign a country based on indicative know party identifiers
		m_results.stream().map(cr -> cr.m_partyIdentifier).forEachOrdered(pi -> checkCountry(pi));
	}
	
	void checkCountry(String partyIdentifier) {
		Country c = null;
		String pi = partyIdentifier.toLowerCase();
		// Assume nationalist sit in all seats 
		if(pi.equalsIgnoreCase("snp")) {
			c = Country.SCOTLAND;
		}
		else if(pi.equalsIgnoreCase("plaid cymru")) {
			c = Country.WALES;
		}
		else if(pi.equalsIgnoreCase("dup") || pi.equalsIgnoreCase("sdlp")  || pi.equalsIgnoreCase("uup") || pi.equalsIgnoreCase("sinn féin")) {
			c = Country.NORTHERN_IRELAND;
		}

		if(c == null) {
			if(m_country == null) {
				m_country = Country.ENGLAND;
			}
		}
		else if(m_country == null) {
			m_country = c;
		}
		else if(m_country == Country.ENGLAND) {
			// Override default value
			m_country = c;
		}
		else if(m_country == c) {
			// Already assigned
		}
		else {
			System.err.println("Unexpected country combination in constituency " + m_name);
		}	
	}
	
	public String toString() {
		return m_name + " (" + m_country + ") : total votes " + m_totalVotes + ", " + m_results.size() + " candidates : won by " + m_winningParty + " (" + m_winningCandidate + ")" + 
					" : " + m_winningVotes + " votes, maj " + m_majority + ", share " + Math.round(m_winningShare) + " %"; 
	}
}

class PartyResult {
	
	String m_name;
	int m_wins;
	int m_seconds;
	int m_thirds;
	int m_contested;
	int m_votes;
	
	PartyResult(String name) {
		m_name = name;
		m_wins = 0;
		m_seconds = 0;
		m_thirds = 0;
		m_contested = 0;
		m_votes = 0;
	}
	
	void addConstituency(Constituency c, CandidateResult cr, int position) {
		// System.out.println("Combining consituency " + c);
		m_contested++;
		m_votes += cr.m_votes;
		if(position == 1) m_wins++; 
		if(position == 2) m_seconds++; 
		if(position == 3) m_thirds++; 
	}

	void mergeWith(PartyResult other) {
		// System.out.println("Merging " + this.toString() + " and " + other.toString());
		this.m_wins += other.m_wins;
		this.m_seconds += other.m_seconds;
		this.m_thirds += other.m_thirds;
		this.m_contested += other.m_contested;
		this.m_votes += other.m_votes;
	}
	
	public String toString() {
		return m_name + " : wins=" + m_wins + ", seconds=" + m_seconds + ", thirds=" + m_thirds + ", contested=" + m_contested + ", votes=" + m_votes + ", votes per win=" + (m_wins==0? m_votes : m_votes/m_wins); 
	}

	// Our bespoke collector implementation, providing supplier, accumulator and combiner 
	static class ResultCollector implements
			Supplier<Map<String, PartyResult>>, 
			BiConsumer<Map<String, PartyResult>, Constituency>, 
			BinaryOperator<Map<String, PartyResult>>
	{		
		static Collector<Constituency, ?, Map<String, PartyResult>> getCollector() {
			ResultCollector rc = new ResultCollector();
			return Collector.of(rc,  rc,  rc, Collector.Characteristics.UNORDERED);
		}

		ResultCollector() {
		}
		
		// Supplier interface
		public Map<String, PartyResult> get() {
			return new HashMap<String, PartyResult>();
		}
		
		// BiConsumer interface
		public void accept(Map<String, PartyResult> m, Constituency c) {
			int position = 0;
			for(CandidateResult cr : c.m_results) {
				position++;
				PartyResult p = m.get(cr.partyIdentifier());
				if(p == null) {
					p = new PartyResult(cr.partyIdentifier());
					m.put(cr.partyIdentifier(), p);
				}
				p.addConstituency(c, cr, position);
			}
		}

		// BinaryOperator interface
		public Map<String, PartyResult> apply(Map<String, PartyResult> m1, Map<String, PartyResult> m2) {
			// Invoked when doing collect on a parallel stream.
			// Combine results from the two maps into a single map (can be a new map or one of the passed-in ones)
			// Update m1 map to include everything in m2 map.
			for(Map.Entry<String, PartyResult> entry : m1.entrySet()) {
				PartyResult other = m2.get(entry.getKey());
				if(other != null) {
					entry.getValue().mergeWith(other);
				}
			}
			
			// Add in anything in map m2 with no entry in m1
			for(Map.Entry<String, PartyResult> entry : m2.entrySet()) {
				PartyResult other = m1.get(entry.getKey());
				if(other == null) {
					m1.put(entry.getKey(), entry.getValue());
				}
			}
			
			return m1;
		}
	}
}

// ==========================================================

class CollectorsExercise {
	
	// Invoke various Collectors to provide example usage
	static void exerciseCollectors(List<CandidateResult> results) {
		// Exercise a few specific collectors
		System.out.println();		
		System.out.println("====================================================================");
		System.out.println();
		// Show the collect(Collectors.counting()) produces the same as .count()
		long candidateNamesByCollector = results.stream().map(cr -> cr.firstName() + " " + cr.surname()).distinct().collect(Collectors.counting());
		System.out.println("Distinct names II:     " + candidateNamesByCollector);

		// Generate an average vote using averaging collector
		double avgVotes = results.stream().collect(Collectors.averagingInt(CandidateResult::votes)); 
		System.out.println("Average votes:     " + avgVotes);

		// Generate a summarizing structure as an alternative
		IntSummaryStatistics iss = results.stream().collect(Collectors.summarizingInt(CandidateResult::votes));
		System.out.println();
		System.out.println("Summary count: " + iss.getCount());
		System.out.println("Summary sum:   " + iss.getSum());
		System.out.println("Summary avg:   " + iss.getAverage());
		System.out.println("Summary max:   " + iss.getMax());
		System.out.println("Summary min:   " + iss.getMin());

		// Max and min elements based on a specified comparator		
		Comparator<CandidateResult> votesComparator = (x,y) -> x.m_votes - y.m_votes;
		Optional<CandidateResult> maxVotes = results.stream().collect(Collectors.maxBy(votesComparator));
		System.out.println("Max votes: " + maxVotes.get().toString());
		Optional<CandidateResult> minVotes = results.stream().collect(Collectors.minBy(votesComparator));
		System.out.println("Min votes: " + minVotes.get().toString());

		// Put elements in List/Set/Collection
		List<String> lConstituencies = results.stream().map(cr -> cr.constituency()).collect(Collectors.toList());	// Allows dups
		Set<String> setConstituencies = results.stream().map(cr -> cr.constituency()).collect(Collectors.toSet());	// No dups
		Collection<String> collConstituencies = results.stream().map(cr -> cr.constituency()).collect(Collectors.toCollection(ArrayList::new));	// No dups
		System.out.println();
		System.out.println("List of " + lConstituencies.size() + " constituencies, set of " + setConstituencies.size() + " constituencies");
		System.out.println("Collection of " + collConstituencies.size() + " constituencies");	
		
		// Set up a map of candidate names to constituency.
		// Throws an exception because some surnames appear more than once
		try {
			Map<String, String> map1 = results.stream().collect(Collectors.toMap(CandidateResult::surname, CandidateResult::constituency));
		} catch(Exception e) {
			System.out.println("Exception caught running toMap: " + e);
		}

		// Use toMap with a merge function to handle this. Just concatenate consituency names
		Map<String, String> map2 = results.stream().collect(Collectors.toMap(CandidateResult::surname, CandidateResult::constituency, (c1, c2) -> c1 + " / " + c2));
		// Show consituencies with someone called 'MAY', 'CORBYN' and 'THATCHER'
		System.out.println("Map of name 'MAY' = " + map2.get("MAY"));
		System.out.println("Map of name 'CORBYN' = " + map2.get("CORBYN"));
		System.out.println("Map of name 'THATCHER' = " + map2.get("THATCHER"));
		
		// Use joining collector to concatenate strings. NB Without distinct simple joining() returns an empty string ? too long presumably.
		String joinedParties = results.stream().map(cr -> cr.partyIdentifier()).distinct().collect(Collectors.joining());
		String joinedDelimitedParties = results.stream().map(cr -> cr.partyIdentifier()).distinct().collect(Collectors.joining(" / "));
		String joinedPrefixedDelimitedParties = results.stream().map(cr -> cr.partyIdentifier()).distinct().collect(Collectors.joining(" / ", "<<< ", " >>>"));
		System.out.println();
		System.out.println("Parties: [" + joinedParties + "]");					// Empty string ????
		System.out.println("Parties: [" + joinedDelimitedParties + "]");		// OK
		System.out.println("Parties: [" + joinedPrefixedDelimitedParties + "]");		// OK
		
		// Partition the stream by a predicate, producing a Map of True/False to two lists
		Map<Boolean,List<CandidateResult>> isLabourMap = results.stream().collect(Collectors.partitioningBy(cr -> cr.partyIdentifier().equals("Labour")));
		System.out.println("Labour candidates: " + isLabourMap.get(Boolean.TRUE).size());
		System.out.println("Other candidates: " + isLabourMap.get(Boolean.FALSE).size());
				
		Map<Boolean,IntSummaryStatistics> isLabourStatsMap = 
				results.stream().collect(Collectors.partitioningBy(
							cr -> cr.partyIdentifier().equals("Labour"), 
							Collectors.summarizingInt(CandidateResult::votes)));
		System.out.println("Labour candidates avg: " + isLabourStatsMap.get(Boolean.TRUE).getAverage() + ", max: " + isLabourStatsMap.get(Boolean.TRUE).getMax());
		System.out.println("Other candidates avg: " + isLabourStatsMap.get(Boolean.FALSE).getAverage() + ", max: " + isLabourStatsMap.get(Boolean.FALSE).getMax());

		// Group by party, list parties with 10+ candidates
		System.out.println();
		Map<String,List<CandidateResult>> byPartyMap = results.stream().collect(Collectors.groupingBy(cr -> cr.partyIdentifier()));
		for(Map.Entry<String, List<CandidateResult>> entry : byPartyMap.entrySet()) {
			if(entry.getValue().size() >= 10) {
				System.out.println("Party: " + entry.getKey() + " - candidates: " + entry.getValue().size());
			}
		}
		
		System.out.println();
		Map<String,IntSummaryStatistics> byPartyStatsMap = results.stream().collect(
						Collectors.groupingBy(cr -> cr.partyIdentifier(),
						Collectors.summarizingInt(CandidateResult::votes)));
		for(Map.Entry<String, IntSummaryStatistics> entry : byPartyStatsMap.entrySet()) {
			if(entry.getValue().getCount() >= 10) {
				System.out.println("Party: " + entry.getKey() + " - candidates: " + entry.getValue().getCount() + ", votes = " + entry.getValue().getSum());
			}
		}
		
		// Reducing operation applied after groupingBy collector
		System.out.println();
		Map<String,Optional<CandidateResult>> byPartyHighestVote = results.stream().collect(
						Collectors.groupingBy(cr -> cr.partyIdentifier(),
						Collectors.reducing(BinaryOperator.maxBy(votesComparator))));
		for(Map.Entry<String, Optional<CandidateResult>> entry : byPartyHighestVote.entrySet()) {
			if(entry.getValue().get().votes() >= 1000) {
				System.out.println("Party: " + entry.getKey() + " - max votes: " + entry.getValue().get().votes() + ", for " + entry.getValue().get().toString());
			}
		}
		
		// Use Collectors.mapping to allow second collector to act on something different from the main stream elements (a field of the element in this 
		// contrived case - could be done using just a partition.
		System.out.println();
		Map<Boolean, List<String>> containsAMap = 
					results.stream().collect(Collectors.mapping(cr -> cr.m_surname, 
								 			 Collectors.partitioningBy(s -> s.indexOf("A") != -1)));
		System.out.println("Candidates with a 'A' in the surname: " + containsAMap.get(Boolean.TRUE).size() + " : eg " + containsAMap.get(Boolean.TRUE).get(0).toString());
		System.out.println("Other candidates: " + containsAMap.get(Boolean.FALSE).size() + " : eg " + containsAMap.get(Boolean.FALSE).get(0).toString());		

		// Second example of mapping, changing focus of collecting after initial groupingBy, to produce a set listing all surnames for each party
		System.out.println();
		Map<String, Set<String>> byPartySurnames = results.stream().collect(
				Collectors.groupingBy(cr -> cr.partyIdentifier(),
				Collectors.mapping(cr -> cr.m_surname, 
								   Collectors.toSet())));
		for(Map.Entry<String, Set<String>> entry : byPartySurnames.entrySet()) {
			System.out.println("Party: " + entry.getKey() + " - surnames: " + entry.getValue().size() + ", e.g. " + entry.getValue().stream().findFirst().get().toString());
		}

		// collectingAndThen to apply a final function to the collected output, e.g. just produce a sublist of 2 items
		List<CandidateResult> lAndThen = results.stream().collect(Collectors.collectingAndThen(Collectors.toList(), l -> l.subList(2, 4)));
		System.out.println();
		System.out.println("List size: " + lAndThen.size());
	}
}