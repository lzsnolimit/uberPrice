package UberPriceQuery;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * UberRequest class, request price information write
 **/

public class UberRequest extends Thread {
	public static Vector<String> citiesUrl = new Vector<String>();
	private static int currentPosition = 0;
	private static String baseStr = "https://www.uber.com";
	public static Map<String, Map<String, String>> cities = new HashMap<String, Map<String, String>>();


	/**
	 * Static code block
	 */
	static {
		getAllCities();
	}

	/**
	 * map to json
	 * @param map
	 * @return String json
	 * @author Zhongshan Lu
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

	/*
	 * generate url then call ParseCity function
	 * 
	 * @see java.lang.Thread#run()
	 */
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

	/**
	 * get all cities from the page https://www.uber.com/cities/ return null
	 */
	public static void getAllCities() {
		Connection con = Jsoup.connect(baseStr + "/cities/");// 获取连接
		con.header("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");// 配置模拟浏览器
		Response rs;
		try {
			rs = con.execute();
			if (rs.statusCode() == 200) {
				Document domTree = Jsoup.parse(rs.body());// 转换为Dom树
				Elements cities = domTree.getElementsByClass("cities-list")
						.get(0).getElementsByTag("li");
				for (Element city : cities) {
					// System.out.println(baseStr+city.getElementsByTag("a").get(0).attr("href"));
					citiesUrl.add(baseStr
							+ city.getElementsByTag("a").get(0).attr("href"));
					// System.out.println(city.getElementsByTag("a").get(0).text());

				}

			}
			currentPosition = citiesUrl.size() - 1;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Parse a city information by url return null
	 * 
	 * @param url
	 */
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
				// CurrencyRate.getCurrencyRate(citiName);

				Elements prices = domTree.getElementsByClass("vehicle-pricing")
						.get(0).getElementsByTag("section");
				boolean singleItem = true;
				Elements titles = new Elements();
				String cityJson = domTree
						.getElementsByAttributeValue("type", "text/javascript")
						.get(0).toString();
				cityJson = cityJson.substring(cityJson.indexOf("{"),
						cityJson.lastIndexOf("}") + 1);
				JSONObject coordinateJsonObject = new JSONObject(cityJson);
				String cityAddrInfo = GoogleMap.GetAddr(
						coordinateJsonObject.getString("lat"),
						coordinateJsonObject.getString("lng"));

				Hbase.addData(citiName, "cityJson", cityJson);
				Hbase.addData(citiName, "cityAddrInfo", cityAddrInfo);
				// System.out.println(cityJson);
				if (prices.size() > 1) {
					titles = domTree.getElementsByClass("vehicle-pricing")
							.get(0).getElementsByTag("nav").get(0).children();
					singleItem = false;
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
						// get vehicles samples
						String vehicles = "";
						for (Element sampleVehicles : prices.get(i).getElementsByClass("uberx-sample-vehicles")) {
							for (Element sampleVehicle : sampleVehicles
									.getElementsByTag("span")) {
								vehicles += sampleVehicle.text() + ",";
							}
						}

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
						temMap.put("currencyRate",
								CurrencyRate.cityVSrate.get(citiName)
										.toString());
						temMap.put("vehicles", vehicles);
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
							Hbase.addData(citiName, title, mapToJson(temMap));
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println(url);
			e.printStackTrace();
			// TODO: handle exception
		}
	}

	/**
	 * Parse the currency symbol from string return the currency symbol
	 * 
	 * @param Fare
	 *            String
	 * @return symbol
	 */
	public static String getSymbol(String FareStr) {
		return FareStr.replaceAll("[0-9.,٫]", "").trim();
	}

	/**
	 * Parse the price from string return the price or 0 if error
	 * 
	 * @param FareStr
	 * @param currencyRate
	 * @return
	 * @throws Exception
	 */
	public static float getNumber(String FareStr, Float currencyRate)
			throws Exception {
		Float realFare = (float) 0;
		if (1 / currencyRate > 2000) {
			realFare = Float.parseFloat(FareStr.replaceAll("[^0-9,]", "")
					.replace(",", "."));
		} else {
			try {
				realFare = Float.parseFloat(FareStr.replaceAll("[^0-9.,]", ""));
			} catch (Exception e) {
				FareStr = FareStr.replaceAll("[^0-9٫,]", "");
				FareStr = FareStr.replaceAll("[٫|,]", ".");
				realFare = Float.parseFloat(FareStr);
			}
		}

		return realFare;
	}

	/**
	 * read json from hbase to cities return null
	 * 
	 * @throws IOException
	 */

	public static void readJson() throws IOException {
		ResultScanner results = Hbase.getAllData();
		for (Result result : results) {
			String rowName = new String(result.getRow());
			Map<String, String> city = new HashMap<String, String>();
			for (Cell cell : result.rawCells()) {
				city.put(new String(CellUtil.cloneQualifier(cell)), new String(
						CellUtil.cloneValue(cell)));

			}
			cities.put(rowName, city);
		}
	}

	//
	// public static void DataCompline() {
	//
	// for (int i = 0; i < cities.size(); i++) {
	// if (cities.get(i) != null) {
	// JSONObject jsonObj = new JSONObject(cities.get(i));
	// // System.out.println(jsonObj.getString("City"));
	// if (!CurrencyRate.cityVSrate.containsKey(jsonObj
	// .getString("City"))) {
	// System.out.println(jsonObj.getString("City"));
	// continue;
	// }
	// jsonObj.put("currencyRate",
	// CurrencyRate.cityVSrate.get(jsonObj.getString("City"))
	// .toString());
	// cities.set(i, jsonObj.toString());
	// }
	// }
	// }

	/**
	 * Calculate the price
	 * 
	 * @param length
	 *            (Unit Kilometer)
	 * @param time
	 *            (Unit Minutes)
	 * @param json
	 *            string
	 * @return
	 */

	public static Float CalculatePrice(int length, int time, String jsonStr) {

		JSONObject jsonObj = new JSONObject(jsonStr);
		if (jsonObj.getString("City").equals("Seoul")) {
			Float price = (float) 0;
			if (length / (time / 30) > 18) {
				price = (float) (5000 + 1500 * length);
			} else {
				price = (float) (5000 + 300 * time);
			}
			price = price * Float.parseFloat(jsonObj.getString("currencyRate"));
			return price;
		}
		try {
			Float currencyRate = Float.parseFloat(jsonObj
					.getString("currencyRate"));
			Float baseFare = getNumber(jsonObj.getString("BaseFare"),
					currencyRate);
			Float distancePrice = getNumber(
					jsonObj.getString("PriceByDistance"), currencyRate)
					* length;

			if (jsonObj.getString("DistanceUnit").equals("mile")) {
				distancePrice = (float) (distancePrice / 1.6);
			}
			Float minutePrice = getNumber(jsonObj.getString("PriceByMinute"),
					currencyRate) * time;
			Float price = baseFare + distancePrice + minutePrice;
			if (price < getNumber(jsonObj.getString("Min fare"), currencyRate)) {
				price = getNumber(jsonObj.getString("Min fare"), currencyRate);
			}
			price = price * currencyRate;
			return price;

		} catch (JSONException e) {
			System.out.println(jsonObj.getString("City") + " error");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println(jsonObj.getString("City") + " error");
			e.printStackTrace();
		}

		return null;
	}
	/**
	 * Analysis price
	 * @param length (kilometer)
	 * @param time (minute)
	 */
	public static void DataAnalysis(int length,int time) {

		for (Entry<String, Map<String, String>> city : cities.entrySet()) {
			System.out.println(city.getKey());
			for (Entry<String, String> pair : ((Map<String, String>) city
					.getValue()).entrySet()) {
				// System.out.println(pair.getKey() + "   " + pair.getValue());
				if (!pair.getKey().equals("cityJson")&&!pair.getKey().equals("cityAddrInfo")) {
					System.out.println(pair.getKey() + "  "
							+ CalculatePrice(length, time, pair.getValue()));
				}
			}
		}
	}

	/**
	 * start threads
	 * @param size of threads
	 */
	
	public static void start(int size) {
		UberRequest[] threads = new UberRequest[size];
		for (int i = 0; i < size; i++) {
			threads[i] = new UberRequest();
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