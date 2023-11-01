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
			names//.peek(System.out::println)
				 .filter(OwnerParser::isCorporate)
//				 .map(OwnerParser::parseCorporateName)
				 .map(OwnerParser::parseCorporateOwner)
//				 .filter(e -> Objects.isNull(e.get("owner_country_acronym")))
				 //.forEach(System.out::println);
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
		Map<String, String> corporateData = new HashMap<>();
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
		// TODO:
		// {owner_street=družstvo pre výstavbu a správu garáží, owner_name=Bratislavské garážové družstvo, owner_corporate_id=2704}
		String firstNamePart = corporateParts.get(0);
		String potentialSecondNamePart = corporateParts.get(1);

		String toEval = potentialSecondNamePart.replace(" ", "");
		if (toEval.contains("spol.") 
			|| toEval.contains("s.r.o") 
			|| toEval.contains("a.s")) {
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
		// TODO:
		// Detský fond Slovenskej republiky, občianske združenie, street 1, city, PSČ 000 00, SR, IČO: 00000
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
	 */
	private static Map<String, String> parsePhysicalPersonOwner(String personLine) {
		try {
			String[] nameParts = personLine.split(", ");
		String fullName = nameParts[0].split(" r\\. ")[0].trim();
		int varIndex = 1; 
		String street = nameParts[varIndex];
		while (isTitle(street)) {
			street = nameParts[++varIndex];
		}
		++varIndex;
		String city = nameParts[varIndex]; // ., sposobi, ze sa tu ocitne niekedy Ing. -> tak ak to konci bodkou, tak je to cast mena a musim ist dalej // neni lepsie pouzit potom iterator?
//		if (city.endsWith(".")) {
//			city = nameParts[++varIndex];
//		}
		++varIndex;
		String psc = nameParts[varIndex].replace("PSČ ", "");
		++varIndex;
		// tento akronym tam je len v pripade, ze ide o SR.. ten vyjebany rakusan, tam nema nic a ma tam rovno birth date
		String countryAcronym = nameParts[varIndex];
		String birthDate = null;
		if (countryAcronym.contains("narodenia")) {
//			System.out.println(countryAcronym);
			birthDate = countryAcronym.split(": ")[1]; // toto zafunguje pri svajciarsku...inak nie
			countryAcronym = null;
		} else {
			++varIndex;
			birthDate = nameParts[varIndex];
			if (birthDate.contains("narodenia")) {
//				System.out.println(birthDate);
				birthDate = birthDate.split(": ")[1]; // toto funguje len tam kde to uz je...ak tam nie je datum narodenia este, tak treba pozriet hlbsie
			}
		}
		Map<String, String> data = new LinkedHashMap<>(6);
		data.put("owner_name", fullName);
		data.put("owner_street", street);
		data.put("owner_city", city);// ., sa ulozi do city aj ked to ma byt cast mena.. toto Ing., mozem zahodit
		data.put("owner_psc", psc);
		data.put("owner_country_code", countryAcronym);
		data.put("owner_birthDate", birthDate);
		return data;
		} catch(Exception ex) {
			return Collections.emptyMap();
		}
	}

	private static final List<String> TITLES = Arrays.asList("MUDr.", "Ing.", "Mgr.", "PhDr.", "JUDr.", "Bc.", "s.r.o.", "Dr.");

	private static boolean isTitle(String line) {
		return TITLES.stream().anyMatch(e -> line.contains(e));
	}

}
