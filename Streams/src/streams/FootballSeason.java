package streams;

// https://docs.oracle.com/javase/8/docs/api/java/util/stream/package-summary.html
// https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html
// https://docs.oracle.com/javase/8/docs/api/java/util/stream/Collectors.html
// https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html

import java.io.IOException;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.util.function.Predicate;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Iterator;
import java.util.IntSummaryStatistics;

public class FootballSeason {

	public static void main(String argv[]) {

		// Data file from http://www.football-data.co.uk/englandm.php - see notes.txt in data folder		
		List<FootballMatch> matches = readResultsFile("data\\EnglishPremierLeagueResults2016-17.csv");
		
		if(matches == null) return;
		
		System.out.println("Read in " + matches.size() + " matches");
		
		long numberOfHomeTeams = matches.stream().map(FootballMatch::homeTeam).distinct().count();
		long numberOfAwayTeams = matches.stream().map(FootballMatch::awayTeam).distinct().count();

		int totalNumberOfHomeGoals = matches.stream().collect(Collectors.summingInt(FootballMatch::homeScore));
		int totalNumberOfAwayGoals = matches.stream().collect(Collectors.summingInt(FootballMatch::awayScore));

		String ss = "abc";
		String ss2 = ss.toLowerCase();
		
		System.out.println("- contains " + numberOfHomeTeams + " home teams and " + numberOfAwayTeams + " away teams");
		System.out.println("- " + totalNumberOfHomeGoals + " home goals and " + totalNumberOfAwayGoals + " away goals");
		System.out.println("- average score " + Math.round(totalNumberOfHomeGoals*100.0/matches.size())/100.0 + "-" + Math.round(totalNumberOfAwayGoals*100.0/matches.size())/100.0 + "");

		long totalNumberOfHomeWins = matches.stream().filter(fm -> fm.result() == FootballMatch.HorAResultType.HOME_WIN).count();
		long totalNumberOfAwayWins = matches.stream().filter(fm -> fm.result() == FootballMatch.HorAResultType.AWAY_WIN).count();
		long totalNumberOfDraws    = matches.stream().filter(fm -> fm.result() == FootballMatch.HorAResultType.DRAW).count();

		System.out.println("- " + totalNumberOfHomeWins + " home wins, " + totalNumberOfAwayWins + " away wins and " + totalNumberOfDraws + " draws");

		String team = "Leicester";
		Predicate<FootballMatch> pHome = fm -> fm.homeTeam().equalsIgnoreCase(team); 
		Predicate<FootballMatch> pAway = fm -> fm.awayTeam().equalsIgnoreCase(team);
		Predicate<FootballMatch> pHomeOrAway = pHome.or(pAway);
		long teamHomeWins = matches.stream().filter(pHome).filter(fm -> fm.result() == FootballMatch.HorAResultType.HOME_WIN).count();
		long teamAwayWins = matches.stream().filter(pAway).filter(fm -> fm.result() == FootballMatch.HorAResultType.AWAY_WIN).count();
		long teamDraws    = matches.stream().filter(pHomeOrAway).filter(fm -> fm.result() == FootballMatch.HorAResultType.DRAW).count();

		System.out.println("- " + team + " : " + teamHomeWins + " home wins, " + teamAwayWins + " away wins and " + teamDraws + " draws");
		
		Map<Integer, List<FootballMatch>> homeScoreMap = matches.stream().collect(Collectors.groupingBy(FootballMatch::homeScore));
		Map<Integer, List<FootballMatch>> awayScoreMap = matches.stream().collect(Collectors.groupingBy(FootballMatch::awayScore));
		Map<String, List<FootballMatch>> matchScoreMap = matches.stream().collect(Collectors.groupingBy(FootballMatch::matchScore));

		System.out.println();
		System.out.println("Home goals scored frequencies: ");		
		homeScoreMap.entrySet().stream().forEachOrdered(e -> System.out.println(e.getKey() + " goals : " + e.getValue().size() + " matches"));
		
		System.out.println();
		System.out.println("Away goals scored frequencies: ");
		awayScoreMap.entrySet().stream().forEachOrdered(e -> System.out.println(e.getKey() + " goals : " + e.getValue().size() + " matches"));

		System.out.println();
		System.out.println("Match score frequencies: ");
		matchScoreMap.entrySet().stream().forEachOrdered(e -> System.out.println("Score " + e.getKey() + " : " + e.getValue().size() + " matches"));		

		List<TeamSeason> lts = 
		matches.stream()
			.map(FootballMatch::teamResults)
			.flatMap(x -> x.stream())
			.collect(Collectors.groupingBy(FootballMatch.TeamResult::team))
			.entrySet().stream()
			.map(x -> TeamSeason.asTeamSeason(x.getKey(), x.getValue()))
			.collect(Collectors.toList());

		League league = new League("English Premier League", lts);

		System.out.println();
		league.printTable();

		System.out.println();
		System.out.println("Top of table:");
		league.printTopTable(5);

		System.out.println();
		System.out.println("Bottom of table:");
		league.printBottomTable(5);

		System.out.println();
		System.out.println("Stats:");
		OptionalDouble odPoints = league.m_leaguePositions.stream().mapToInt(p -> p.teamSeason().m_points).average();
		int sumPoints = league.m_leaguePositions.stream().mapToInt(p -> p.teamSeason().m_points).sum();
		IntSummaryStatistics statsSummary = league.m_leaguePositions.stream().mapToInt(p -> p.teamSeason().m_points).summaryStatistics();
		System.out.println("- avg points: " + odPoints.orElse(-999.0));
		System.out.println("- sum points: " + sumPoints);
		System.out.println("- summary points count: " + statsSummary.getCount());
		System.out.println("- summary points average: " + statsSummary.getAverage());
		System.out.println("- summary points sum: " + statsSummary.getSum());
		System.out.println("- summary points max: " + statsSummary.getMax());
		System.out.println("- summary points min: " + statsSummary.getMin());
		

		// Exercise some other Stream functions using the league data
		Exerciser.exerciseStreamGeneration(league);
	}
	
	static List<FootballMatch> readResultsFile(String path) {

		List<FootballMatch> l = null;
		
		try (Stream<String> stream = Files.lines(Paths.get(path))) {

//			l = stream.map(FootballMatch::fromLine).filter(fm -> fm != null).collect(Collectors.toList());		// Works
			l = stream.map(FootballMatch::fromLine).filter(Objects::nonNull).collect(Collectors.toList());		// Also works
			// Could also use skip to avoid first line
			// But don't get a line number for error reporting using this method. If the source was a List, could use a stream based on  IntStream.range(0, list.size) to drive
			// things. Also how to easily check for rejected lines when others are OK, apart from by checking resulting list size is the expected size.

		} catch (IOException e) {			
			System.err.println("Failed to load data from file: " + e.getMessage());
			return null;
		}
		
		return l;
	}
}

class FootballMatch {
	
	enum HorAResultType {
		HOME_WIN, AWAY_WIN, DRAW
	}

	String m_league;
	String m_date;
	String m_homeTeam;
	String m_awayTeam;
	int m_fullTimeHomeScore;
	int m_fullTimeAwayScore;
	HorAResultType m_fullTimeResult;
	
	String homeTeam() { return m_homeTeam; }
	String awayTeam() { return m_awayTeam; }
	int homeScore() { return m_fullTimeHomeScore; }
	int awayScore() { return m_fullTimeAwayScore; }
	String matchScore() { return homeScore() + "-" + awayScore(); }
	HorAResultType result() { return m_fullTimeResult; }
	
	public String toString() {
		return m_date + " " + m_homeTeam + " " + m_fullTimeHomeScore + "-" + m_fullTimeAwayScore + " " + m_awayTeam;
	}	
	static class TeamResult {
		
		enum ResultType {
			WIN, LOSE, DRAW
		}

		FootballMatch m_match;
		String m_team;
		boolean m_home;
		int m_goalsFor;
		int m_goalsAgainst;
		ResultType m_result;
		
		TeamResult(FootballMatch match, boolean isHome) {

			m_match = match;
			m_home = isHome;
			m_team = isHome ? match.m_homeTeam : match.m_awayTeam;
			m_goalsFor = isHome ? match.m_fullTimeHomeScore : match.m_fullTimeAwayScore;
			m_goalsAgainst = isHome ? match.m_fullTimeAwayScore : match.m_fullTimeHomeScore;
			m_result = m_goalsFor > m_goalsAgainst ? ResultType.WIN :
							(m_goalsFor < m_goalsAgainst ? ResultType.LOSE : ResultType.DRAW); 
		}
		
		String team() { return m_team; }
		int goalsFor() { return m_goalsFor; }
		int goalsAgainst() { return m_goalsAgainst; }
		
		int points() {
			switch(m_result) {
				case WIN : return 3;
				case DRAW : return 1;
				default: return 0;
			}
		}
		
		public String toString() {
			return m_team + " " + m_result.toString() + "   " + m_match;
		}
	}
	
	List<TeamResult> teamResults() {
		List<TeamResult> l = new ArrayList<>();
		l.add(new TeamResult(this, true));
		l.add(new TeamResult(this, false));
		return l;
	}
	
	// Div,Date,HomeTeam,AwayTeam,FTHG,FTAG,FTR,HTHG,HTAG,HTR,Referee,...
	// E0,13/08/16,Burnley,Swansea,0,1,A,0,0,D,J Moss,...
	// E0,13/08/16,Crystal Palace,West Brom,0,1,A,0,0,D,...

	static FootballMatch fromLine(String line) {
		// Ignore headings and blank lines
		
		if(line.trim().length() == 0) return null;
		if(line.startsWith("Div,Date")) return null;
		
		String fields[] = line.split(",");	// Assume no commas in data, no quotes around values, no trimming of fields needed
		if(fields.length < 10) {
			System.err.println("Error - data line with insufficient fields: " + line);
			return null;
		}
		
		FootballMatch fm = new FootballMatch();
		fm.m_league = fields[0];
		fm.m_date = fields[1];
		fm.m_homeTeam = fields[2];
		fm.m_awayTeam = fields[3];
		try {
			fm.m_fullTimeHomeScore = Integer.parseInt(fields[4]);
			fm.m_fullTimeAwayScore = Integer.parseInt(fields[5]);
		} catch(Exception e) {
			System.err.println("Error parsing data line - invalid score " + line);
			return null;
		}
		
		switch(fields[6]) {
			case "H" : fm.m_fullTimeResult = HorAResultType.HOME_WIN; break;
			case "A" : fm.m_fullTimeResult = HorAResultType.AWAY_WIN; break;
			case "D" : fm.m_fullTimeResult = HorAResultType.DRAW; break;
			default : fm.m_fullTimeResult = null;
		};
		
		if(fm.m_fullTimeResult == null) {
			System.err.println("Error parsing data line - invalid result" + line);
			return null;			
		}
		
		return fm;
	}
	
}

class TeamSeason {
	String m_team;
	int m_played;
	int m_points;
	int m_for;
	int m_against;
	
	// Add in wins, losses, draws, to produce a complete summary for the team

	String team() { return m_team; }
	int played() { return m_played; }
	int points() { return m_points; }
	int goalDifference() { return m_for - m_against; }
	
	public String toString() {
		return m_team + " played = " + played() + " points=" + points() + " gd=" + goalDifference();
	}
	
	static TeamSeason asTeamSeason(String team, List<FootballMatch.TeamResult> lResults) {
		TeamSeason ts = new TeamSeason();
		ts.m_team = team;
		// Combine collection below rather than three separate steps.
		ts.m_played = (int)lResults.stream().count();
		ts.m_points = lResults.stream().collect(Collectors.summingInt(FootballMatch.TeamResult::points));
		ts.m_for = lResults.stream().collect(Collectors.summingInt(FootballMatch.TeamResult::goalsFor));
		ts.m_against = lResults.stream().collect(Collectors.summingInt(FootballMatch.TeamResult::goalsAgainst));		
		return ts;
	}
}

class League {

	// Add league ordering comparator
	
	static String tableHeading() {
		Formatter fmt = new Formatter();
		fmt.format("%3.3s %-20.20s %10.10s %10.10s %10.10s", "Pos", "Team", "Played", "Goal diff", "Points");
		String s = fmt.toString();
		fmt.close();
		return s;
	}
	
	static String tableRow(TeamSeasonPosition tsp) {
		int pos = tsp.m_position;
		TeamSeason ts = tsp.m_teamSeason;
		Formatter fmt = new Formatter();
		fmt.format("%3d %-20.20s %10d %10d %10d", pos, ts.team(), ts.played(), ts.goalDifference(), ts.points());
		String s = fmt.toString();
		fmt.close();
		return s;
	}

	static class TeamSeasonPosition implements Comparable<TeamSeasonPosition> {
		TeamSeason m_teamSeason;
		int m_position;
		
		TeamSeasonPosition(TeamSeason ts, int position) {
			m_teamSeason = ts;
			m_position = position;
		}
		
		int position() { return m_position; }
		TeamSeason teamSeason() { return m_teamSeason; }
		
		public String toString() {
			return "Position = " + position() + " : " + m_teamSeason.toString();
		}
		
		public int compareTo(TeamSeasonPosition tsp2) {
			return this.position() - tsp2.position();
		}
	}
	
	String m_name;
	List<TeamSeasonPosition> m_leaguePositions;
	static Comparator<TeamSeason> s_leagueOrdering = Comparator.comparing(TeamSeason::points).thenComparing(TeamSeason::goalDifference).reversed();
	
	League(String name, List<TeamSeason> ts) {
		// Sort the league, and put in order into a list
		List<TeamSeason> lSorted = ts.stream().sorted(s_leagueOrdering).collect(Collectors.toList());		
		m_leaguePositions = IntStream.rangeClosed(1, lSorted.size()).mapToObj(pos -> new TeamSeasonPosition(lSorted.get(pos-1), pos)).collect(Collectors.toList());
	}

	void printTable() {
		System.out.println(League.tableHeading());
		m_leaguePositions.stream().forEachOrdered(lp -> System.out.println(League.tableRow(lp)));		
	}

	void printTopTable(int length) {
		System.out.println(League.tableHeading());
		m_leaguePositions.stream().limit(length).forEachOrdered(lp -> System.out.println(League.tableRow(lp)));		
	}

	void printBottomTable(int length) {
		System.out.println(League.tableHeading());
		m_leaguePositions.stream().skip(m_leaguePositions.size() - length).forEachOrdered(lp -> System.out.println(League.tableRow(lp)));		
	}
}

// Try out various other stream interface methods, using the league data for example content
class Exerciser {

	static void exerciseStreamGeneration(League league) {

		List<League.TeamSeasonPosition> positions = league.m_leaguePositions;
		if(positions.size() < 10) {
			System.out.println("Not enough league positions");
			return;
		}

		System.out.println();
		System.out.println(".............................................................................");
		System.out.println();
		System.out.println("Stream generation examples");
		
		// Creating an empty stream with Stream.empty()
		System.out.println();
		Stream<League.TeamSeasonPosition> emptyStream = Stream.empty();
		System.out.println("- empty stream count: " + emptyStream.count());
		
		// Creating a stream with a single specific element using Stream.of()
		System.out.println();
		Stream<League.TeamSeasonPosition> oneElementStream = Stream.of(positions.get(0));
		System.out.println("- one element stream count: " + oneElementStream.count());
		
		// Creating a stream with a variable number of specific elements using Stream.of(...)
		System.out.println();
		Stream<League.TeamSeasonPosition> variableLengthElementStream = Stream.of(positions.get(0), positions.get(3), positions.get(8));
		System.out.println("- variable length element stream count: " + variableLengthElementStream.count());
		// NB Stream is consumed doing count operation, so recreate it to do try out forEachOrdered on it
		variableLengthElementStream = Stream.of(positions.get(0), positions.get(3), positions.get(8));
		variableLengthElementStream.forEachOrdered(ts -> System.out.println("  - " + ts.m_teamSeason.m_team + " in position " + ts.m_position));
		
		// Creating a stream by concatenating two existing streams using Stream.concat()
		System.out.println();
		Stream<League.TeamSeasonPosition> concatInputStream1 = Stream.of(positions.get(0), positions.get(2));
		Stream<League.TeamSeasonPosition> concatInputStream2 = Stream.of(positions.get(0), positions.get(4));
		Stream<League.TeamSeasonPosition> concatOutputStream = Stream.concat(concatInputStream1, concatInputStream2);
		System.out.println("- concat stream count: " + concatOutputStream.count());
		// NB Input streams are consumed doing concat operation, so can't use them again afterwards - regenerate them to try out forEachOrdered
		concatInputStream1 = Stream.of(positions.get(0), positions.get(2));
		concatInputStream2 = Stream.of(positions.get(0), positions.get(4));
		concatOutputStream = Stream.concat(concatInputStream1, concatInputStream2);
		concatOutputStream.forEachOrdered(ts -> System.out.println("  - " + ts.m_teamSeason.m_team + " in position " + ts.m_position));
		
		// Creating a stream by using a Stream.Builder
		System.out.println();
		Stream.Builder<League.TeamSeasonPosition> builder = Stream.builder();
		builder.add(positions.get(0)).add(positions.get(5)).add(positions.get(6));
		Stream<League.TeamSeasonPosition> builtStream = builder.build(); 
		System.out.println("- built stream count: " + builtStream.count());
		
		// Creating a stream of characters (as ints) from a String 
		System.out.println();
		String str = "A CharSequence is a readable sequence of char values";
		IntStream is = str.chars();
		System.out.println("- .chars() produced a stream of " + is.count() + " integers from the string [" + str + "]");
		
		// Creating a stream from an Array
		System.out.println();
		League.TeamSeasonPosition a[] = positions.toArray(new League.TeamSeasonPosition[positions.size()]);
		Stream<League.TeamSeasonPosition> streamFromArray = Arrays.stream(a);
		System.out.println("- stream from array count: " + streamFromArray.count());
		
		// Peeking at a stream before and after filtering
		positions.stream()
				.limit(6)
				.peek(tsp -> System.out.println("Pre-filter peek:  " + tsp))
				.filter(tsp -> tsp.position() < 3)
				.peek(tsp -> System.out.println("Post-filter peek: " + tsp))
				.count();	// Need a terminal operation for anything to run
		
		// NB .sorted() throws a ClassCastException if League.TeamSeasonPosition does not implement Ccomparable
		System.out.println();
		positions.stream().sorted().forEachOrdered(System.out::println);
		
		// Sort specifying a comparator, e.g. as a lambda directly or stored as a comparator
		System.out.println();
		positions.stream().sorted((x,y) -> y.position() - x.position()).forEachOrdered(System.out::println);	// Sorts by position reversed.		
		System.out.println();
		Comparator<League.TeamSeasonPosition> reverseOrder = (x,y) -> y.position() - x.position();	// Also sorts by position reversed.
		positions.stream().sorted(reverseOrder).forEachOrdered(System.out::println);
		
		// Demonstrate some 'Match' and 'Find' terminal operations
		System.out.println();
		printTrueFalse("- allMatch positions < 15", positions.stream().allMatch(p -> p.position() < 15));
		printTrueFalse("- allMatch positions < 20", positions.stream().allMatch(p -> p.position() < 20));
		printTrueFalse("- allMatch positions < 21", positions.stream().allMatch(p -> p.position() < 21));
		printTrueFalse("- anyMatch positions > 18", positions.stream().anyMatch(p -> p.position() > 18));
		printTrueFalse("- anyMatch positions > 19", positions.stream().anyMatch(p -> p.position() > 19));
		printTrueFalse("- anyMatch positions > 20", positions.stream().anyMatch(p -> p.position() > 20));
		printTrueFalse("- noneMatch positions > 18", positions.stream().noneMatch(p -> p.position() > 18));
		printTrueFalse("- noneMatch positions > 19", positions.stream().noneMatch(p -> p.position() > 19));
		printTrueFalse("- noneMatch positions > 20", positions.stream().noneMatch(p -> p.position() > 20));
		printTrueFalse("- empty stream: allMatch positions < 15", positions.stream().limit(0).allMatch(p -> p.position() < 15));
		printTrueFalse("- empty stream: anyMatch positions < 15", positions.stream().limit(0).anyMatch(p -> p.position() < 15));
		printTrueFalse("- empty stream: noneMatch positions < 15", positions.stream().limit(0).noneMatch(p -> p.position() < 15));
		
		printOptional("- findFirst found", positions.stream().findFirst());
		printOptional("- findAny found", positions.stream().findAny());
		printOptional("- empty stream: findFirst found", positions.stream().limit(0).findFirst());
		printOptional("- empty stream: findAny found", positions.stream().limit(0).findAny());
		
		// max and min terminal operations
		System.out.println();
		Comparator<League.TeamSeasonPosition> teamNameOrder = (x,y) -> x.teamSeason().m_team.compareTo(y.teamSeason().m_team);	// Also sorts by position reversed.
		printOptional("- max team name", positions.stream().max(teamNameOrder));
		printOptional("- min team name", positions.stream().min(teamNameOrder));
		printOptional("- empty stream : max team name", positions.stream().limit(0).max(teamNameOrder));
		printOptional("- empty stream : min team name", positions.stream().limit(0).min(teamNameOrder));
		
		// Further Terminal operations for numeric streams
		// Create an array of objects from stream elements
		System.out.println();
		Object a1[] = positions.stream().limit(5).toArray();
		System.out.println("- object array size " + a1.length + ", element 1 = " + a1[1].toString());		
		// Create an array of typed from stream elements, with generator function allocating the array
		League.TeamSeasonPosition[] a2 = positions.stream().limit(7).toArray(League.TeamSeasonPosition[]::new);
		System.out.println("- typed array size " + a2.length + ", points 5 = " + a2[5].teamSeason().m_points);

		// Create an iterator over the stream elements
		System.out.println();
		Iterator<League.TeamSeasonPosition> it = positions.stream().limit(4).iterator();
		while(it.hasNext()) {
			League.TeamSeasonPosition tsp = it.next();
			System.out.println("- iter : " + tsp.toString());
		}
		
		// Parallel and sequential
		System.out.println();
		Stream<League.TeamSeasonPosition> parseq1 = positions.stream().sequential();
		Stream<League.TeamSeasonPosition> parseq2 = positions.stream().parallel();
		printTrueFalse("- sequential stream isParallel ?", parseq1.isParallel());
		printTrueFalse("- parallel stream isParallel ?", parseq2.isParallel());
		// forEach is not ordered, so parallel stream produces out of order listing
		parseq1.forEach(p -> System.out.println(" Seq: " + p.toString()));
		parseq2.forEach(p -> System.out.println(" Par: " + p.toString()));		
	}
	
	static void printTrueFalse(String s, boolean b) {
		System.out.println(s + " " + (b ? "true" : "false"));
	}
	
	static void printOptional(String s, Optional<League.TeamSeasonPosition> op) {
		if(op.isPresent()) {
			System.out.println(s + " " + op.get().toString());			
		}
		else {
			System.out.println(s + " " + " [no value]");			
		}
			
	}
}