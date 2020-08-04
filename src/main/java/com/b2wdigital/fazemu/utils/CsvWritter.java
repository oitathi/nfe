package com.b2wdigital.fazemu.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvWritter {
	
	public byte[] createCsvArrBytes(List<String[]> dataLines) {
		FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {

            File file = createTempCsvFile(dataLines);
            bytesArray = new byte[(int) file.length()];

            //read file into bytes[]
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return bytesArray;
	}
	
	private File createTempCsvFile(List<String[]> dataLines) throws Exception {
		File temp = File.createTempFile("test.csv", ".csv");
		PrintStream fileStream = new PrintStream(temp);
		String line;
		for(String[] arr: dataLines) {
			line = convertToCSV(arr);
			fileStream.println(line);
		}
		fileStream.close();
		return temp;
	}
	
	private String convertToCSV(String[] data) {
	    return Stream.of(data)
	      .map(this::escapeSpecialCharacters)
	      .collect(Collectors.joining(";"));
	} 
	
	
	private String escapeSpecialCharacters(String data) {
	    String escapedData = data.replaceAll("\\R", " ");
	    if (data.contains(",") || data.contains("\"") || data.contains("'")) {
	        data = data.replace("\"", "\"\"");
	        escapedData = "\"" + data + "\"";
	    }
	    return escapedData;
	}

}
