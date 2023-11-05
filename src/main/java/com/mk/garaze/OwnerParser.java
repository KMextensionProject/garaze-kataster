 package com.mk.garaze;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class OwnerParser {

	public static void main(String[] args) throws IOException {
		try (Stream<String> names = Files.lines(Paths.get("src/main/resources/to_parse"))) {
			names.filter(e -> !isCorporate(e))
//				.peek(System.out::println)
				 .map(OwnerParser::parsePhysicalPersonOwner)
//				 .filter(e -> e.get("owner_birthDate") == null)
				 .forEach(System.out::println);
		}
	}

	/**
	 * @param ownerLine
	 * @return
	 */
	public static Map<String, String> parseOwner(String ownerLine) {
		if (isCorporate(ownerLine)) {
			return parseCorporateOwner(ownerLine);
		}
		return parsePhysicalPersonOwner(ownerLine);
	}

	private static boolean isCorporate(String name) {
		return name.contains("IČO");
	}

	// Alt + arrow = move the line or highlighted lines in that direciton
	/**
	 *
	 * @param corporateLine
	 * @return
	 */
	private static Map<String, String> parseCorporateOwner(String corporateLine) {
		List<String> corporateParts = stream(corporateLine.split(",")).map(String::trim).collect(toList());
		Map<String, String> corporateData = new HashMap<>(6);
		/*
		 * The order must be maintained because all parse methods are mutating the list
		 * enriching it with empty values or shrinking it when some elements are
		 * undesired..this way the list has equal size for processing parts
		 */
		corporateData.put("owner_name", parseCorporateName(corporateParts));
		corporateData.put("owner_corporate_id", parseCorporateIdentification(corporateParts));
		// if the city owns it, there is not much of it.. handle with changing state?
		if (corporateParts.size() > 2) { 
			corporateData.put("owner_street", parseCorporateStreet(corporateParts));
			corporateData.put("owner_city", parseCorporateCity(corporateParts));
			corporateData.put("owner_postal_code", parseCorporatePostalCode(corporateParts));
			corporateData.put("owner_country_acronym", parseCorporateCountryAcronym(corporateParts));
		}
		return corporateData;
	}

	private static String parseCorporateName(List<String> corporateParts) {
//		Detský fond Slovenskej republiky, občianske združenie, street 1, city, PSČ 000 00, SR, IČO: 00000
//		{owner_street=družstvo pre výstavbu a správu garáží, owner_name=Bratislavské garážové družstvo, owner_corporate_id=2704}
		String firstNamePart = corporateParts.get(0);
		String potentialSecondNamePart = corporateParts.get(1);

		// TODO: do something with this
		String toEval = potentialSecondNamePart.replace(" ", "");
		if (toEval.contains("spol.") // can omit spol.?
			|| toEval.contains("s.r.o") 
			|| toEval.contains("a.s")
			|| toEval.contains("združenie")
			|| toEval.contains("družstvo")) {
			corporateParts.remove(1); // let others forget this splitted name with additional comma
			firstNamePart += " " + potentialSecondNamePart;
		}
		return firstNamePart;
	}

	private static String parseCorporateStreet(List<String> corporateParts) {
		// owner_street=sídlo, owner_name=BELT & AT - SLOVAKIA spol. s r.o.
		// => BELT & AT - SLOVAKIA, spol. s r.o., sídlo, street 1, city, PSČ 000 00, SR, IČO: xxxxxxx
		String street = corporateParts.get(1);
		if ("sídlo".equals(street)) {
			corporateParts.remove(1);
			street = corporateParts.get(1);
		}
		return street;
	}

	private static String parseCorporateCity(List<String> corporateParts) {
		// owner_name=And..ová - Salón AAA, owner_city=IČO: 00000000
		// => And..ová - Salón AAA, street 10, IČO: 00000000
		String city = corporateParts.get(2);
		if (city.contains("IČO") || city.contains("PSČ")) {
			// this means no city is present on this record so insert null
			// element to maintain the correct order for later parsers
			corporateParts.add(2, null);
			city = null;
		}
		return city;
	}

	private static String parseCorporatePostalCode(List<String> corporateParts) {
		String postalCode = corporateParts.get(3);
		if (!postalCode.contains("PSČ")) {
			corporateParts.add(3, null);
			postalCode = null;
		} else {
			postalCode = postalCode.replace("PSČ", "").trim();
		}
		return postalCode;
	}

	private static String parseCorporateCountryAcronym(List<String> corporateParts) {
		String country = corporateParts.get(corporateParts.size() - 2);
		// SK.. ak je to viac, mohlo sa tam zapliest aj psc alebo mesto
		if (country == null || country.length() > 3) {
			country = null;
		}
		return country;
	}

	private static String parseCorporateIdentification(List<String> corporateParts) {
		// this should be always present, this is the reason why it got
		// to this processing in the first place
		return corporateParts.get(corporateParts.size() - 1).replace("IČO: ", "");
	}

	/**
	 *
	 * @param personLine
	 * @return without TITLE before and after name
	 */
	private static Map<String, String> parsePhysicalPersonOwner(String personLine) {
		int openingBrace = personLine.indexOf('(');
		int closingBrace = personLine.lastIndexOf(')') + 1;
		String withoutInfo = personLine;
		if (openingBrace != -1 && closingBrace != 0) {
			withoutInfo = new StringBuilder(personLine).delete(openingBrace, closingBrace).toString();
		}
		List<String> personParts = stream(withoutInfo.split(",")).map(String::trim).collect(toList());
		Map<String, String> data = new LinkedHashMap<>(6);
		data.put("owner_name", parsePhysicalPersonName(personParts));
		data.put("owner_street", parsePhysicalPersonStreet(personParts));
		data.put("owner_city", parsePhysicalPersonCity(personParts));
		data.put("owner_postal_code", parsePhysicalPersonPostalCode(personParts));
		data.put("owner_country_code", parsePhysicalPersonCountryCode(personParts));
		data.put("owner_birthDate", parsePhysicalPersonBirthDate(personParts));
		return data;
	}

	private static final Pattern HAS_DIGITS = Pattern.compile("[0-9]");

	private static String parsePhysicalPersonName(List<String> personParts) {
		String fullName = personParts.get(0);

		// if there are two owners, pick the first one
		String delimeter = fullName.contains(".a ") ? ".a " : " a ";
		String[] aSplit = fullName.split(delimeter);
		if (aSplit.length > 1) {
			fullName = aSplit[0];
			// ak ta druha polka ma adresu, treba ju tam posunut
			if (HAS_DIGITS.matcher(aSplit[1]).find()) {
				String[] spaceSplit = aSplit[1].split(" ");
				int partsLength = spaceSplit.length;
				// parse and add street on the position where it belongs
				String street = spaceSplit[partsLength - 2]
							  + " "
							  + spaceSplit[partsLength - 1];
				personParts.add(1, street);	
			}
		}

		fullName = removeAllTitles(fullName);

		// we are interested only in the current name
		String[] rSplit = fullName.split("r\\.");
		if (rSplit.length > 1) {
			fullName = rSplit[0].trim();
		}

		// if also another part contains this birth name
		if (personParts.get(1).contains("r.")) {
			personParts.remove(1);
		}

		// following parts may be titles alone..
		TITLES.add("ing"); // can also occur without a dot.. cannot delete it from the name
		while (containsTitle(personParts.get(1))) {
			personParts.remove(1);
		}
		TITLES.remove("ing");

		// refactor with respect to the above usage
		if (HAS_DIGITS.matcher(fullName).find()) {
			String[] spaceSplit = fullName.split(" ");
			int partsLength = spaceSplit.length;
			// parse and add street on the position where it belongs
			String street = spaceSplit[partsLength - 2]
					      + " "
						  + spaceSplit[partsLength - 1];
			personParts.add(1, street);
			StringBuilder name = new StringBuilder();
			for (int i = 0; i < partsLength - 2; i++) {
				name.append(spaceSplit[i])
				    .append(" ");
			}
			fullName = name.toString().trim();
		}

		return fullName;
	}

	private static final List<String> TITLES = new ArrayList<>(Arrays.asList("mudr.", "ing.", "mgr", "phd", "judr.", "bc.", "dr.", "csc"));

	private static boolean containsTitle(String line) {
		return TITLES.stream().anyMatch(e -> line.toLowerCase().contains(e));
	}

	private static String removeAllTitles(String fullName) {
		StringBuilder name = new StringBuilder(fullName);
		int rmStartIndex;
		for (String title : TITLES) { // tu ak title je ze Ing, nedavat case insensitive + dat prec len vtedy, ak nim konci
			if ((rmStartIndex = name.toString().toLowerCase().indexOf(title)) != -1) {
				name.delete(rmStartIndex, (rmStartIndex + title.length()));
			}
		}
		return name.toString().trim();
	}

	private static String parsePhysicalPersonStreet(List<String> personParts) {
		String street = personParts.get(1);

		// ak uz hore je realne street, tak ju tam treba dat
		// toto dole, zoberie opat ten prvy element a pozre, ci je tam
		// cislo, ak ano, tak street ostane taka ako ma byt
		Iterator<String> parts = personParts.iterator();
		parts.next(); //0
		while (parts.hasNext()) {
			String part = parts.next();
			if (HAS_DIGITS.matcher(part).find()) {
				street = part;
				break;
			}
			parts.remove();
		}

		if (street.contains("narodenia")) {
			personParts.add(1, null); // street
			personParts.add(2, null); // city
			personParts.add(3, null); // psc
			personParts.add(4, null); // country
			personParts.add(5, street);
			street = null;
		}
		if (street != null) {
			return street.split(" a ")[0];
		}
		return street;
	}

	private static String parsePhysicalPersonCity(List<String> personParts) {
		String city = personParts.get(2);
		if (city != null) {
			city = city.split(" a ")[0];
			if (city.contains("narodenia")) {
				personParts.add(2, null);
				city = null;
			}
		}
		return city;
	}

	private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("^\\d{5}$|^\\d{3}\\s\\d{2}$");

	private static String parsePhysicalPersonPostalCode(List<String> personParts) {
		String postalCode = personParts.get(3);
		if (postalCode != null) {
			postalCode = postalCode.replace("PSČ", "").trim();
			if (!POSTAL_CODE_PATTERN.matcher(postalCode).find()) {
				// do something else with it?
				personParts.add(3, postalCode);
				postalCode = null;
			}
		}
		return postalCode;
	}

	// nemecka spolkova republika, datum narodenia
	private static String parsePhysicalPersonCountryCode(List<String> personParts) {
		String countryCode = personParts.get(4);
		if (countryCode != null) {
			if (countryCode.length() < 4) {
				return countryCode;
			} else if (countryCode.contains("narodenia")) {
				personParts.add(5, countryCode);
				countryCode = null;
			}
//			personParts.add(4, null);
		}
		return countryCode;
	}

	private static String parsePhysicalPersonBirthDate(List<String> personParts) {
		Optional<String> birthDate = personParts.stream().filter(Objects::nonNull).filter(e -> e.contains("narodenia")).findFirst();
		if (birthDate.isPresent()) {
			return birthDate.get().split(" ")[2];
		}
		return null;
	}
}
