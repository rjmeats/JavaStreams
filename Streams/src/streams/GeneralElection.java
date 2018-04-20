package streams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeneralElection {

	public static void main(String argv[]) {

		// Data file from http://www.football-data.co.uk/englandm.php - see notes.txt in data folder		
		List<CandidateResult> results = readResultsFile("data\\UKGeneralElection2017.csv");
		
		if(results == null) return;
		
		System.out.println("Read in " + results.size() + " candidate results");
		
		long constituencies = results.stream().map(CandidateResult::constituency).distinct().count();
		long parties = results.stream().map(CandidateResult::partyIdentifier).distinct().count();
		long candidateSurnames = results.stream().map(CandidateResult::surname).distinct().count();
		long candidateFirstNames = results.stream().map(CandidateResult::firstName).distinct().count();
		long candidateNames = results.stream().map(cr -> cr.firstName() + " " + cr.surname()).distinct().count();
		
		System.out.println();
		System.out.println("Constituencies:        " + constituencies);
		System.out.println("Parties:               " + parties);
		System.out.println("Distinct surnames:     " + candidateSurnames);
		System.out.println("Distinct first names:  " + candidateFirstNames);
		System.out.println("Distinct names:        " + candidateNames);
		
		int totalVotes = results.stream().collect(Collectors.summingInt(CandidateResult::votes));

		System.out.println();
		System.out.println("Total votes : " + totalVotes);

		System.out.println();
		results.stream().map(CandidateResult::partyIdentifier).distinct().sorted().forEachOrdered(System.out::println);
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
		
		cr.m_constituency = fields[2];
		cr.m_surname = fields[3];
		cr.m_firstname = fields[4];
		cr.m_party = fields[5];
		cr.m_partyIdentifier = fields[6];
		try {
			cr.m_votes = Integer.parseInt(fields[7]);
		} catch(Exception e) {
			System.err.println("Error parsing data line - invalid data: " + line);
			return null;
		}		
		
		return cr;
	}
	
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
}
