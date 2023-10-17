package com.mk.garaze;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TDParser {

//	public static TitleDeed parseHtml(String titleDeedUrl) {
//		return new TitleDeed();
//	}

	// TODO: vyjebat vsetko co sa da...nech jedna mapa zodpoveda celemu listu vlastnictva...
	// potom si to budem sam filtrovat podla typu priestoru
	public static List<Map<String, String>> parseTitleDeed(File htmlFile) throws IOException {

		List<Map<String, String>> titleDeed = new ArrayList<>();
		Map<String, String> titleDeedItem;

		Document doc = Jsoup.parse(htmlFile);
		String number = resolveTitleDeedNumber(doc);
		String municipality = extractMunicipality(doc);
		Elements tables = getMainPartTables(doc);

		// this loop is usefull only when it comes to jump statements
		for (Iterator<Element> tableIterator = tables.iterator(); tableIterator.hasNext();) {
			titleDeedItem = new HashMap<>();
			titleDeedItem.put("obec", municipality);
			titleDeedItem.put("cislo", number);

			Elements tableRows = tableIterator.next().getElementsByTag("tr");
			List<String> rowValues = tableRows.get(0).getElementsByTag("div").eachText();

			// section 1
			if (rowValues.isEmpty() || rowValues.size() < 8) {
				continue;
			}
			titleDeedItem.put("vchod", rowValues.get(1));
			titleDeedItem.put("poschodie", rowValues.get(3));
			titleDeedItem.put("cislo_priestoru", rowValues.get(5));
			titleDeedItem.put("podiel_priestoru", rowValues.get(7));

			// section 2
			rowValues = tableRows.get(1).getElementsByTag("div").eachText();
			titleDeedItem.put("supisne_cislo", rowValues.get(1));
			titleDeedItem.put("miestna_cast", rowValues.get(3));
			if (rowValues.size() > 5) { // tento element tam nie je pri bytoch
				String type = rowValues.get(5); // TODO: proper resolving, introduce enum for this
				titleDeedItem.put("druh_nebytoveho_priestoru", "2".equals(type) ? "Garáž" : type);
			}

			// section 3 - ine udaje/plomby 1
			rowValues = tableRows.get(2).getElementsByTag("div").eachText();
			if (rowValues.isEmpty()) {
				continue;
			}
			titleDeedItem.put("ine_udaje", rowValues.get(1));

			// fields from 2.table
			// section 1 --> tieto sekcie mozu byt niekedy dve az tri po sebe
			tableRows = tableIterator.next().getElementsByTag("tr");
			rowValues = tableRows.get(1).getElementsByTag("td").eachText();
			titleDeedItem.put("vlastnik", rowValues.get(1)); // split this..set only the first person ?
			titleDeedItem.put("spoluvlastnicky_podiel", rowValues.get(2));
			// section 2
			rowValues = tableRows.get(3).getElementsByTag("td").eachText();
			titleDeedItem.put("titul_nadobudnutia", rowValues.get(0));
			// section 3 ine udaje/plomby 2
			rowValues = tableRows.get(5).getElementsByTag("td").eachText();
			titleDeedItem.put("ine_udaje_2", rowValues.get(0));

			titleDeed.add(titleDeedItem);
			skipUnwantedBottomTables(tableIterator);
		}

		return titleDeed;
	}

	private static String resolveTitleDeedNumber(Document doc) {
		// TODO: handle NPEs
		return doc.title().replace("LV - ", "");
	}

	private static String extractMunicipality(Document doc) {
		// TODO: handle NPEs
		return doc.getElementsByTag("tbody").get(0) // they reside in the first table
			.getElementsByTag("tr").get(1) // second table row
			.getElementsByTag("td").get(3) // and fourth column
			.text();
	}

	private static Elements getMainPartTables(Document doc) {
		// TODO: handle NPEs and empty list
		Elements allTables = doc.getElementsByTag("tbody");
		// omit first 3 metadata tables and load every table except the last section with no relevant data
		return new Elements(allTables.subList(3, allTables.size() - 3));
	}

	private static void skipUnwantedBottomTables(Iterator<Element> tableIterator) {
		for (int i = 0; i < 4 && tableIterator.hasNext(); i++) {
			tableIterator.next();
		}
	}
}
