 package com.mk.garaze;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class OwnerParser {

	public static void main(String[] args) throws IOException {
		try (Stream<String> names = Files.lines(Paths.get("src/main/resources/to_parse"))) {
			names.filter(e -> !isCorporate(e))
//				 .map(OwnerParser::parsePhysicalPersonOwner)
				 .forEach(System.out::println);
		}
	}

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
	 * @return
	 * TODO: TOTAL REFACTOR
	 */
	private static Map<String, String> parsePhysicalPersonOwner(String personLine) {
		List<String> personParts = stream(personLine.split(",")).map(String::trim).collect(toList());

		Map<String, String> data = new LinkedHashMap<>(6);
//		data.put("owner_name", fullName);
//		data.put("owner_street", street);
//		data.put("owner_city", city);// ., sa ulozi do city aj ked to ma byt cast mena.. toto Ing., mozem zahodit
//		data.put("owner_postal_code", psc);
//		data.put("owner_country_code", countryAcronym);
//		data.put("owner_birthDate", birthDate);
		return data;
	}

	private static final List<String> TITLES = Arrays.asList("MUDr.", "Ing.", "Mgr.", "PhDr.", "JUDr.", "Bc.", "s.r.o.", "Dr.");

	private static boolean isTitle(String line) {
		return TITLES.stream().anyMatch(e -> line.contains(e));
	}

}
