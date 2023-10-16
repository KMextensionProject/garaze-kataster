package com.mk.garaze.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;

import net.sf.jett.transform.ExcelTransformer;

public class XlsxUtil {

	public static void generateXlsx(String template, Map<String, Object> dataSource, String targetName) throws IOException, InvalidFormatException {
		InputStream inputStream = XlsxUtil.class.getClassLoader().getResourceAsStream(template);
		ExcelTransformer transformer = new ExcelTransformer();
		Workbook workbook = transformer.transform(inputStream, dataSource);
		workbook.write(new FileOutputStream(targetName));
	}
}
