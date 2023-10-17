package com.mk.garaze;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mk.garaze.utils.DocxUtil;
import com.mk.garaze.utils.XlsxUtil;

public class Main {

	// skuist toto: https://kevcodez.de/posts/2015-07-18-jsoup-tutorial/
	public static void main(String[] args) throws Exception {
		List<Map<String, String>> titleDeeds = new ArrayList<>(500);

		File[] lvs = new File("/home/UX/mkrajcovicux/Documents/lv_nebytove_priestory").listFiles();
		for (File html : lvs) {
			System.out.println("processing: " + html.getName());
			titleDeeds.addAll(TDParser.parseTitleDeed(html));
		}
		Comparator<Map<String, String>> byMunicipality = comparing(m -> m.get("obec"));
		Comparator<Map<String, String>> byStreet = comparing(m -> m.get("vchod"));

		// wrap it for transformer
		Map<String, Object> data = new HashMap<>(1);
		data.put("table", titleDeeds.stream()
				.filter(Main::onlyGarages)
				.distinct()
				.sorted(byMunicipality.thenComparing(byStreet))
				.collect(toList())
		);
		XlsxUtil.generateXlsx("templates/xlsx/garages_report.xlsx", data, "output_report.xlsx");
		// this is meant to be created for all the lines that have been marked as chosen in excel
		DocxUtil.generateDocx("templates/docx/garages_letter.docx", titleDeeds.get(0), "output_letter.docx");
	}

	private static boolean onlyGarages(Map<String, String> map) {
		return "Garáž".equals(map.get("druh_nebytoveho_priestoru"));
	}
}
