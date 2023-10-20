package com.mk.garaze;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NameParser {

	public static void main(String[] args) throws IOException {
		try (Stream<String> names = Files.lines(Paths.get("src/main/resources/to_parse"))) {
			names//.peek(System.out::println)
				 .map(NameParser::parseName)
				 //.forEach(System.out::println);
				 .forEach(System.out::println);
		}
	}

	public static Map<String, String> parseName(String nameLine) {
		try {
			String[] nameParts = nameLine.split(", ");
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
			System.out.println(nameParts[varIndex-1]);
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
