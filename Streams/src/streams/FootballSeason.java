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
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;
import java.util.Formatter;

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
			System.err.println("Error parsing data line - invalid score" + line);
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

	static class TeamSeasonPosition {
		TeamSeason m_teamSeason;
		int m_position;
		
		TeamSeasonPosition(TeamSeason ts, int position) {
			m_teamSeason = ts;
			m_position = position;
		}
	}
	
	String m_name;
	List<TeamSeasonPosition> m_leaguePositions;
	static Comparator<TeamSeason> s_leagueOrdering = Comparator.comparing(TeamSeason::points).thenComparing(TeamSeason::goalDifference).reversed();
	
	League(String name, List<TeamSeason> ts) {
		List<TeamSeason> lSorted = ts.stream().sorted(s_leagueOrdering).collect(Collectors.toList());		
		m_leaguePositions = IntStream.rangeClosed(1, lSorted.size()).mapToObj(pos -> new TeamSeasonPosition(lSorted.get(pos-1), pos)).collect(Collectors.toList());
	}

	void printTable() {
		System.out.println(League.tableHeading());
		m_leaguePositions.stream().forEachOrdered(lp -> System.out.println(League.tableRow(lp)));		
	}
}
