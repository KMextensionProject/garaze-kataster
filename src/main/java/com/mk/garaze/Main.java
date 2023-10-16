package com.mk.garaze;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {

	public static void main(String[] args) {
		String url_1 = "https://kataster.skgeodesy.sk/Portal/api/Bo/GeneratePrfPublic?prfNumber=3677&cadastralUnitCode=870293&outputType=html"; // toryska pg3
		String url_2 = "https://kataster.skgeodesy.sk/Portal/api/Bo/GeneratePrfPublic?prfNumber=3275&cadastralUnitCode=870293&outputType=html";
		String url_3 = "https://kataster.skgeodesy.sk/Portal/api/Bo/GeneratePrfPublic?prfNumber=6572&cadastralUnitCode=870293&outputType=html";
		try {
			Document doc = Jsoup.parse(new File("/home/martom/Desktop/list_vlastnictva.html"));
			Elements elements = doc.getAllElements();

			// jebat bude to normalna mapa, ktoru budem potom vediet tresknut do excelu..
//			TitleDeed td = new TitleDeed();
//			td.setTitle(elements.select("title").text());
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("title", elements.select("title").text());

			// prva tabulka ma v sebe katastralne uzemie, resp. obec
			Elements tables = elements.select("tbody");
//			System.out.println("tables: " + tables.size());
			Elements rows = tables.first().select("tr").get(1).select("td");
//			System.out.println("rows: " + rows.size());
//			System.out.println(rows.get(3).text());

			data.put("municipality", rows.get(3).text()); // obec

			System.out.println(data);
			
//			for (Element table : tables) {
//				
//			}
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}
}
