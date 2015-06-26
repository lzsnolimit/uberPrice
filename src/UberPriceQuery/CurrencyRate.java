package UberPriceQuery;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CurrencyRate extends Thread {
	public static Vector<String> cities = new Vector<String>();
	public static Vector<String> wikiLinks = new Vector<String>();
	public static Map<String, String> cityVScountry = new HashMap<String, String>();
	public static Map<String, String> countryVSsymbol = new HashMap<String, String>();
	public static Map<String, Float> symbolVSrate = new HashMap<String, Float>();
	public static Map<String, Float> cityVSrate = new HashMap<String, Float>();
	private static String wikiBase = "https://en.wikipedia.org";
	public static int currentPosition;

	// public static void readFile() {
	// File wikilinks = new File("WikiLinks.txt");
	// if (wikilinks.exists()) {
	// try {
	// String cityName = "";
	// String link = "";
	// BufferedReader in = new BufferedReader(
	// new FileReader(wikilinks));
	// while (cityName != null && link != null) {
	// try {
	// cityName = in.readLine();
	// link = in.readLine();
	// if (cityName != null && link != null) {
	// cities.addElement(cityName);
	// wikiLinks.addElement(link);
	// }
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// }
	// } catch (FileNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// currentPosition = cities.size() - 1;
	// }

	/**
	 * multiple threads entrance
	 */
	public void run() {
		String link = "";
		String city = "";

		while (currentPosition >= 0) {
			synchronized (CurrencyRate.class) {
				link = wikiLinks.get(currentPosition);
				city = cities.get(currentPosition);
				currentPosition--;
			}
			ParseWikiCity(link, city);
		}
	}

	/**
	 * set current position
	 */
	public static void setPosition() {
		currentPosition = cities.size() - 1;
	}

	/**
	 * Parse wiki city by link,
	 * 
	 * @param wiki
	 *            city link
	 * @param city
	 *            name
	 */
	public static void ParseWikiCity(String link, String city) {
		Connection con = Jsoup.connect(link);// 获取连接
		con.header("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");// 配置模拟浏览器
		Response rs;
		try {
			rs = con.execute();
			if (rs.statusCode() == 200) {
				Document domTree = Jsoup.parse(rs.body());// 转换为Dom树
				Elements thLinks = domTree.getElementsByTag("th");
				String countryLink = "";
				String countryName = "";
				for (Element result : thLinks) {
					if (result.text().contains("Sovereign state")
							|| result.text().contains("Country")) {
						try {
							countryName = result.nextElementSibling().text()
									.trim();
						} catch (Exception e) {
							countryName = result.parent().nextElementSibling()
									.text().trim();
						}
						try {
							countryLink = result.nextElementSibling()
									.getElementsByTag("a").get(0).attr("href")
									.trim();
						} catch (Exception e) {
							countryLink = "";
							// e.printStackTrace();
						}

						break;
					}
				}

				if (!countryName.equals("")) {
					cityVScountry.put(city, countryName);
				} else {
					cityVScountry.put(city, link);
				}

				if (!countryLink.equals("")) {
					ParseWikiCountry(wikiBase + countryLink, countryName);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(link);
			e.printStackTrace();
		}
	}

	/**
	 * Parse wiki country by link,
	 * 
	 * @param wiki
	 *            country link
	 * @param country
	 *            name
	 */
	public static void ParseWikiCountry(String link, String country) {
		Connection con = Jsoup.connect(link);// 获取连接
		con.header("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");// 配置模拟浏览器
		Response rs;
		if (!countryVSsymbol.containsKey(country)) {
			try {
				rs = con.execute();
				if (rs.statusCode() == 200) {
					Document domTree = Jsoup.parse(rs.body());// 转换为Dom树
					Elements symbolElements = domTree
							.getElementsByAttributeValue("title", "ISO 4217");
					if (!symbolElements.isEmpty()) {
						String symbol = symbolElements.get(0).text().trim();
						countryVSsymbol.put(country, symbol);
					}

				}
			} catch (IOException e) {
				System.out.println(link);
				e.printStackTrace();
			}
		}
	}

	/**
	 * parse rate by currency symbol using Yahoo finance API
	 */
	public static void ParseRateBySymbol() {
		String link = "http://finance.yahoo.com/webservice/v1/symbols/allcurrencies/quote";
		Connection con = Jsoup.connect(link);// 获取连接
		con.header("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");// 配置模拟浏览器
		Response rs;
		String name, temName;
		Float value;
		try {
			rs = con.execute();
			if (rs.statusCode() == 200) {
				Document domTree = Jsoup.parse(rs.body());// 转换为Dom树
				Elements resoures = domTree.getElementsByTag("resource");
				for (Element resource : resoures) {
					temName = resource.child(0).text().trim();
					name = temName.substring(temName.indexOf("/") + 1);
					value = 1 / Float.parseFloat(resource.child(1).text()
							.trim());
					symbolVSrate.put(name, value);
					// System.out.println(name + value);
				}
			}
		} catch (IOException e) {
			System.out.println(link);
			e.printStackTrace();
		}
	}

	/**
	 * get currency rate
	 * 
	 * @param city
	 *            name
	 */
	public static void getCurrencyRate(String city) {
		byBing(city);
		//byGoogle(city);
	}

	/**
	 * get wikipedia city page from bing
	 * 
	 * @param cityName
	 * @return wiki url
	 */
	public static String byBing(String cityName) {
		String bingBase = "https://www.bing.com/search?q=";
		Connection con = Jsoup.connect(bingBase + cityName + " wikipedia");// 获取连接
		con.header("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");// 配置模拟浏览器
		Response rs;
		try {
			rs = con.execute();
			if (rs.statusCode() == 200) {

				Document domTree = Jsoup.parse(rs.body());// 转换为Dom树
				Elements SearchResults = domTree.getElementsByClass("b_algo");
				// System.out.println(city);
				String wikiLink = "";
				for (Element result : SearchResults) {
					if (result.child(0).text().contains("Wikipedia")) {
						// System.out.println(result.child(0).attr("href"));
						wikiLink = result.child(0).child(0).attr("href");
						break;
					}
				}

				if (wikiLink.length() == 0) {
					cityVScountry.put(cityName, "");
				} else {
					File writename = new File("WikiLinks.txt");

					cities.addElement(cityName);
					wikiLinks.addElement(wikiLink);

					if (!writename.exists()) {
						try {
							writename.createNewFile();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					try {
						BufferedWriter out = new BufferedWriter(new FileWriter(
								writename, true));
						out.write(cityName + "\r\n");
						out.write(wikiLink + "\r\n");
						out.flush(); // 把缓存区内容压入文件
						out.close(); // 最后记得关闭文件
					} catch (IOException e) {
						System.out.println(cityName);
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(cityName);
			e.printStackTrace();
		}
		return cityName;
	}

	/**
	 * get wikipedia city page from Google Google blocks my IP
	 * 
	 * @param cityName
	 * @return wiki url
	 */
	public static String byGoogle(String cityName) {
		String googleBase = "https://www.google.com/search?q=";
		Connection con = Jsoup.connect(googleBase + cityName);// 获取连接
		con.header("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");// 配置模拟浏览器
		Response rs;
		try {
			rs = con.execute();
			if (rs.statusCode() == 200) {

				Document domTree = Jsoup.parse(rs.body());// 转换为Dom树
				Elements SearchResults = domTree.getElementsByClass("r");
				// System.out.println(city);
				String wikiLink = "";
				for (Element result : SearchResults) {
					if (result.text().contains("Wikipedia")) {
						// System.out.println(result.child(0).attr("href"));
						wikiLink = result.child(0).attr("href");
						break;
					}
				}

				if (wikiLink.length() == 0) {
					System.out.println(cityName);
				} else {

					System.out.println(wikiLink);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(cityName);
			e.printStackTrace();
		}
		return cityName;
	}

	/**
	 * Connect the currency and rate Output the rate in FinalRates.txt
	 * 
	 * @return
	 **/
	public static void summary() throws IOException {
		File writename = new File("FinalRates.txt");
		BufferedWriter out = new BufferedWriter(new FileWriter(writename, true));
		String cityName;
		String countryName;
		String symbol;
		for (Entry<String, String> city : cityVScountry.entrySet()) {
			cityName = city.getKey();
			// out.write(cityName + "|");
			countryName = city.getValue();
			String outStr = "";

			if (countryName.equals("")) {
				outStr = "  " + "\r\n";
			} else {
				if (countryVSsymbol.containsKey(countryName)) {
					symbol = countryVSsymbol.get(countryName);
					if (symbol.equals("USD")) {
						outStr = "1.0";
					} else if (symbolVSrate.containsKey(symbol)) {
						outStr = symbolVSrate.get(symbol).toString();
					} else {
						outStr = symbol;
					}

				} else {
					outStr = countryName;
				}
			}
			System.out.println(cityName);
			System.out.println(outStr);
			out.write(cityName + "\r\n");
			out.write(outStr + "\r\n");
		}
	}

	/**
	 * Read currency rate from FinalRates.txt
	 */
	public static void readRates() {
		File wikilinks = new File("FinalRates.txt");
		if (wikilinks.exists()) {
			try {
				String cityName = "";
				Float rate;
				BufferedReader in = new BufferedReader(
						new FileReader(wikilinks));
				while (cityName != null) {
					try {
						cityName = in.readLine();
						if (cityName != null) {
							rate = Float.parseFloat(in.readLine());
							cityVSrate.put(cityName, rate);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						System.out.println(cityName);
						e.printStackTrace();
					}

				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * start threads
	 * @param size of threads
	 */
	public static void start(int size) {
		
		CurrencyRate[] threads = new CurrencyRate[size];
		for (int i = 0; i < size; i++) {
			threads[i] = new CurrencyRate();
			threads[i].start();
		}
		for (int i = 0; i < size; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
