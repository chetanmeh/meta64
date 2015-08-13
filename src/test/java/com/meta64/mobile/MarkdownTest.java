package com.meta64.mobile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.pegdown.PegDownProcessor;

/*
 * Takes one command line parameter which is the name of the file to convert, and then converts the file
 * to HTML and writes to the same file name but with .html appended.
 * 
 * Example:
 * Input: myfile.md 
 * Output: myfile.md.html
 */
public class MarkdownTest {

	public static void main(String[] args) {
		try {
			PegDownProcessor proc = new PegDownProcessor();
			String file = args[0];
			String input = readFile(file);
			String output = wrapInHtmlDoc(proc.markdownToHtml(input));
			writeFile(file+".html", output);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String wrapInHtmlDoc(String text) {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>");
		sb.append("<style>");
		sb.append("body {font-family: Tahoma, Verdana, Arial; margin: 2em;}");
		sb.append("</style>");
		sb.append("<body>");
		sb.append(text);
		sb.append("</body></html>");
		return sb.toString();
	}

	public static String readFile(String path) throws Exception {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8);
	}

	public static void writeFile(String path, String text) throws Exception {
		Files.write(Paths.get(path), text.getBytes());
	}
}
