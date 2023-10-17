package com.mk.garaze.utils;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import org.docx4j.model.datastorage.migration.VariablePrepare;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

public class DocxUtil {

	public static void generateDocx(String template, Map<String, String> dataSource, String outputFileName) throws Exception {
		InputStream iStream = DocxUtil.class.getClassLoader().getResourceAsStream(template);
		WordprocessingMLPackage WProcessor = WordprocessingMLPackage.load(iStream);

		HeaderPart headerPart = WProcessor.getHeaderFooterPolicy().getHeader(1);
		MainDocumentPart mainDocPart = WProcessor.getMainDocumentPart();
		VariablePrepare.prepare(WProcessor);

//		dataSource.put("sender_name", "Martin Krajcovic");
//		dataSource.put("sender_address", "Galvaniho 17/C, 821 04, Bratislava");
//		dataSource.put("sender_mobile", "0903 458 756");
//		dataSource.put("sender_email", "mkrajcovic@gratex.com");
//		dataSource.put("sender_psc", "821 04");
//		dataSource.put("sender_street", "Galvaniho 17/C");
//		dataSource.put("sender_city", "Bratislava");
//
//		dataSource.put("recipient_name", "Gratex International");
//		dataSource.put("recipient_surname", "International");
//		dataSource.put("recipient_psc", "821 07");
//		dataSource.put("recipient_city", "Bratislava");
//		dataSource.put("recipient_street", "Pestovatelská 9");
//		dataSource.put("garage_street", "Bieloruská");

		headerPart.variableReplace(dataSource);
		mainDocPart.variableReplace(dataSource);
		WProcessor.save(new FileOutputStream(outputFileName));
	}
}
