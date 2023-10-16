package com.mk.garaze.utils;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import org.docx4j.model.datastorage.migration.VariablePrepare;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

public class DocxUtil {

	public static void generateDocx(String template, String outputFileName, Map<String, String> dataSource)throws Exception {
		InputStream iStream = DocxUtil.class.getClassLoader().getResourceAsStream(template);
		WordprocessingMLPackage WProcessor = WordprocessingMLPackage.load(iStream);
		MainDocumentPart mainDocPart = WProcessor.getMainDocumentPart();
		VariablePrepare.prepare(WProcessor);
		mainDocPart.variableReplace(dataSource);
		WProcessor.save(new FileOutputStream(outputFileName));
	}
}
