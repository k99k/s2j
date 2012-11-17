/**
 * 
 */
package com.k99k.smali;

import java.util.ArrayList;


/**
 * @author keel
 *
 */
public class Tool {

	/**
	 * 
	 */
	public Tool() {
	}
	
	
	/**
	 * 从多个参数的String中解析出多个参数
	 * @param propStr
	 * @return
	 */
	public static final ArrayList<String> fetchObjects(String propStr){
		ArrayList<String> ls = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < propStr.length(); i++) {
			char c = propStr.charAt(i);
			if (c == '[') {
				sb.append(c);
				continue;
			}
			if (Character.isUpperCase(c)) {
				String s = String.valueOf(c);
				if (c == 'L') {
					int end = propStr.indexOf(';',i);
					s = propStr.substring(i,end+1);
					i = end;
				}
				ls.add(sb.append(s).toString());
			}
			sb = new StringBuilder();
		}
		return ls;
	}
	
	/**
	 * 转换处理smali的对象
	 * @param smaliObj
	 * @return
	 */
	public static final String parseObject(String smaliObj){
		if (smaliObj.startsWith("L") && smaliObj.endsWith(";")) {
			String s = smaliObj.substring(1,smaliObj.length()-1);
			s = s.replaceAll("\\/", "\\.");
			//处理掉java.lang
			s = s.replaceAll("java\\.lang\\.", "");
			//处理掉$符
			s = s.replaceAll("\\$", "\\.");
			return s;
		}
		else if (smaliObj.equals("V")) {
			return "void";
		}
		else if (smaliObj.equals("I")) {
			return "int";
		}
		else if (smaliObj.equals("Z")) {
			return "boolean";
		}
		else if (smaliObj.equals("F")) {
			return "float";
		}else if (smaliObj.equals("S")) {
			return "short";
		}else if (smaliObj.equals("J")) {
			return "long";
		}else if (smaliObj.equals("D")) {
			return "double";
		}else if (smaliObj.equals("B")) {
			return "byte";
		}else if (smaliObj.startsWith("[")) {
			int arrLen = smaliObj.lastIndexOf("[")+1;
			StringBuilder sb = new StringBuilder(parseObject(smaliObj.substring(arrLen)));
			for (int i = 0; i < arrLen; i++) {
				sb.append("[]");
			}
			return sb.toString();
		}
		return "UnknownObj";
	}
	
	/**
	 * 获取某行的key
	 * @param nextLine 不可为null
	 * @return key String
	 */
	public static final String getKey(String line){
		if (line.startsWith("#")) {
			return StaticUtil.COMM;
		}
		int keyEnd = line.indexOf(" ");
		if (keyEnd > -1) {
			String key = line.substring(0, keyEnd).trim();
			return key;
		}
		String s = line.trim();
		if (!s.equals("")) {
			return s;
		}
		return null;
	}
	
	/**
	 * 将String按行转换成ArrayList,删除空行,trim处理
	 * @param str
	 * @return ArrayList
	 */
	public static final ArrayList<String> strToLine(String str) {
		String[] arr = str.split("\n");
		ArrayList<String> ls = new ArrayList<String>(arr.length);
		for (int i = 0; i < arr.length; i++) {
			if (!arr[i].trim().equals("")) {
				ls.add(arr[i].trim());
			}
		}
		return ls;
	}
	
	/**
	 * 读取第n行.                                      
	 * @param smaliTxt
	 * @param line 目标行号
	 * @return
	 */
	public static final String readLine(String smaliTxt,int line){
		
		int c = -1;
		int l = 1;
		while (true) {
			int n = smaliTxt.indexOf("\n", c+1);
			if (l == line) {
				String str = smaliTxt.substring(c,n);
				return str.trim();
			}
			if (n == -1) {
				return null;
			}
			c = n;
			l++;
		}
	}
	
	public static void main(String[] args) {
//		try {
//			String t = IO.readTxt("h:/AppDetails.java", "utf-8");
//			String s = readLine(t,153);
//			System.out.println(s);
//			String[] arr = strToLine(t);
//			System.out.println(arr.length);
//			System.out.println(arr[23]);
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		String s = "[[[Ljava.lang.String;";
		System.out.println(parseObject(s));
		s = "I";
		System.out.println(parseObject(s));
	}

}
