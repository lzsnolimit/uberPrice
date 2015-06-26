package UberPriceQuery;

import java.io.IOException;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

/*
 *@Zhongshan Lu
 */
public class GoogleMap {
	private static String key = "AIzaSyAVC6l37MiKQJJjuBA2x3AQOF7HahKSJZg";
	private static String baseUrl = "https://maps.googleapis.com/maps/api/geocode/json?";

	/**
	 * Get address by coordinate
	 * 
	 * @param latitude
	 * 
	 * @param longitude
	 * 
	 * @return address
	 */
	public static String GetAddr(String latitude, String longitude) {
		String link = baseUrl + "latlng=" + latitude + "," + longitude
				+ "&key=" + key;

		// https://maps.googleapis.com/maps/api/geocode/json?latlng=40.714224,-73.961452&key=AIzaSyAVC6l37MiKQJJjuBA2x3AQOF7HahKSJZg
		Connection con = Jsoup.connect(link);// 获取连接
		con.ignoreContentType(true);
		con.header("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");// 配置模拟浏览器
		Response rs;
		try {
			rs = con.execute();
			if (rs.statusCode() == 200) {
				return rs.body();
			} else {
				System.out.println(link);
				return null;
			}
		} catch (IOException e) {
			System.out.println(link);
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * Get coordinate by address
	 * 
	 * @param address
	 * 
	 * @return coordinate
	 */
	public static String getCoordinate(String addr) {
		// https://maps.googleapis.com/maps/api/geocode/json?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA&key=AIzaSyAVC6l37MiKQJJjuBA2x3AQOF7HahKSJZg
		String link = baseUrl + "address=" + addr + "&key=" + key;
		Connection con = Jsoup.connect(link);// 获取连接
		con.ignoreContentType(true);
		con.header("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");// 配置模拟浏览器
		Response rs;
		try {
			rs = con.execute();
			if (rs.statusCode() == 200) {
				return rs.body();
			} else {
				System.out.println(link);
				return null;
			}
		} catch (IOException e) {
			System.out.println(link);
			e.printStackTrace();
			return null;
		}

	}
}