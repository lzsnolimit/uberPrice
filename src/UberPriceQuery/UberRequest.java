package UberPriceQuery;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class UberRequest extends Thread {
	public static Vector<String> citiesUrl = new Vector<String>();
	private static int currentPosition = 0;
	private static String baseStr = "https://www.uber.com";
	public static Vector<String> cities = new Vector<String>();

	// public static Vector<Map<String, Map<String, String>>> cities = new
	// Vector<Map<String, Map<String, String>>>();

	public UberRequest() {

	}

	/**
	 * 
	 * map转换json. <br>
	 * 详细说明
	 * 
	 * @param map
	 *            集合
	 * @return
	 * @return String json字符串
	 * @throws
	 * @author slj
	 */
	public static String mapToJson(Map<String, String> map) {
		Set<String> keys = map.keySet();
		String key = "";
		String value = "";
		StringBuffer jsonBuffer = new StringBuffer();
		jsonBuffer.append("{");
		for (Iterator<String> it = keys.iterator(); it.hasNext();) {
			key = (String) it.next();
			value = map.get(key);
			jsonBuffer.append(key + ":" + "\"" + value + "\"");
			if (it.hasNext()) {
				jsonBuffer.append(",");
			}
		}
		jsonBuffer.append("}");
		return jsonBuffer.toString();
	}

	public void run() {

		String url = "";
		while (currentPosition >= 0) {
			synchronized (UberRequest.class) {

				url = citiesUrl.get(currentPosition);
				currentPosition--;
			}

			ParseCity(url);
		}
	}

	public static void getAllCities() throws IOException {
		Connection con = Jsoup.connect(baseStr + "/cities/");// 获取连接
		con.header("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");// 配置模拟浏览器
		Response rs;
		rs = con.execute();
		if (rs.statusCode() == 200) {
			Document domTree = Jsoup.parse(rs.body());// 转换为Dom树
			Elements cities = domTree.getElementsByClass("cities-list").get(0)
					.getElementsByTag("li");
			for (Element city : cities) {
				// System.out.println(baseStr+city.getElementsByTag("a").get(0).attr("href"));
				citiesUrl.add(baseStr
						+ city.getElementsByTag("a").get(0).attr("href"));
				 //System.out.println(city.getElementsByTag("a").get(0).text());

			}

		}

		currentPosition = citiesUrl.size() - 1;
	}

	public static void ParseCity(String url) {
		Connection con = Jsoup.connect(url);
		con.header("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");// 配置模拟浏览器
		Response rs;
		try {

			rs = con.execute();
			if (rs.statusCode() == 200) {

				Document domTree = Jsoup.parse(rs.body());// 转换为Dom树
				String citiName = domTree.getElementsByClass("js-city-title")
						.get(0).text();
				CurrencyRate.getCurrencyRate(citiName);
				Elements prices = domTree.getElementsByClass("vehicle-pricing")
						.get(0).getElementsByTag("section");
				boolean singleItem = true;
				Elements titles = new Elements();
				if (prices.size() > 1) {
					titles = domTree.getElementsByClass("vehicle-pricing")
							.get(0).getElementsByTag("nav").get(0).children();
					singleItem = false;
				}
				if (prices.size() == 0) {
					// System.out.println(url);
					// System.out.println(domTree.getElementsByClass("vehicle-pricing")
					// .get(0).toString());
				}

				for (int i = 0; i < prices.size(); i++) {

					Map<String, String> temMap = new HashMap<String, String>();
					String title = "";
					if (singleItem) {
						title = domTree.getElementsByClass("sunrise").get(0)
								.text();
					} else {
						title = titles.get(i).text();
					}

					boolean containContents = true;
					if (prices.get(i).getElementsByTag("div").size() == 0) {
						containContents = false;
					}

					if (containContents) {
						String baseFarePrice = prices.get(i)
								.getElementsByTag("div").get(0)
								.getElementsByTag("p").get(1).text();
						String priceByMinute = prices.get(i)
								.getElementsByTag("div").get(1)
								.getElementsByTag("p").get(0).text();
						String priceByDistance = prices.get(i)
								.getElementsByTag("div").get(2)
								.getElementsByTag("p").get(0).text();

						String distanceUnit = prices.get(i)
								.getElementsByTag("div").get(2)
								.getElementsByTag("span").get(0).text();

						temMap.put("BaseFare", baseFarePrice);
						// System.out.println(citiName+" "+baseFarePrice+"  "+getNumber(baseFarePrice));

						temMap.put("PriceByMinute",
								priceByMinute.replace(" per minute", ""));
						temMap.put("PriceByDistance",
								priceByDistance.replace(" " + distanceUnit, ""));
						temMap.put("DistanceUnit",
								distanceUnit.replace("Per ", ""));
						temMap.put("Type", title);
						temMap.put("City", citiName);

						Elements extraPrices = prices.get(i)
								.getElementsByTag("div").get(3)
								.getElementsByTag("div");
						for (Element extraPrice : extraPrices) {
							temMap.put(extraPrice.getElementsByTag("p").get(0)
									.text(), extraPrice.getElementsByTag("p")
									.get(1).text());
						}
						// System.out.println(mapToJson(temMap));
						if (temMap.size() != 0) {

							cities.addElement(mapToJson(temMap));
						}

					}

				}
			}
		} catch (Exception e) {
			// System.out.println(url);
			// e.printStackTrace();
			// TODO: handle exception
		}
	}

	public static void write() {
		File writename = new File("UberPriceJson.txt");
		if (!writename.exists()) {
			try {
				writename.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));
			for (int i = 0; i < cities.size(); i++) {
				out.write(cities.get(i) + "\r\n");
			}
			out.flush(); // 把缓存区内容压入文件
			out.close(); // 最后记得关闭文件
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String getSymbol(String FareStr) {
		String currenctSymbol = "";
		for (int j = 0; j < FareStr.length(); j++) {
			if (!Character.isDigit(FareStr.charAt(j))) {
				currenctSymbol += FareStr.charAt(j);
			} else {
				break;
			}
		}

		if (currenctSymbol.trim().length() == 0) {
			for (int j = 0; j < FareStr.length(); j++) {

				if (Character.isSpaceChar(FareStr.charAt(j))) {
					currenctSymbol = FareStr.substring(j + 1);
					break;
				}
			}
		}
		return currenctSymbol;
	}

	public static float getNumber(String FareStr) {
		for (int i = 0; i < FareStr.length(); i++) {
			if (Character.isDigit(FareStr.charAt(i))) {
				for (int j = FareStr.length() - 1; j >= 0; j++) {
					if (Character.isDigit(FareStr.charAt(i))) {
						return Float.parseFloat(FareStr.substring(i, j + 1));
					}
				}
			}
		}
		return 0;
	}
}