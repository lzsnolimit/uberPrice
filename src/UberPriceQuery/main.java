package UberPriceQuery;


public class main {

	public static void main(String[] args) throws Exception {
		
		// UberRequest.getAllCities();
		// startUberRequestThreads(5);
		// CurrencyRate.ParseRateBySymbol();
		// System.out.println(CurrencyRate.cities.size() + " cities");
		// CurrencyRate.setPosition();
		// startCurrencyRateThreads(2);
		// try {
		// CurrencyRate.summary();
		// } catch (Exception e) {
		// // TODO: handle exception
		// e.printStackTrace();
		// }

		// UberRequest.readJson();
		// CurrencyRate.readRates();
		// UberRequest.DataCompline();
		// UberRequest.write();
		// UberRequest.DataAnalysis();
		// for (Entry<Float, String> price : UberRequest.prices.entrySet()) {
		// System.out.println(price.getValue()+"  "+price.getKey());
		// }

		// String addr = GoogleMap.GetAddr("31.875676", "117.3094928");
		// System.out.println(addr);
		
		Hbase.setStrings("Uber", "info");  //set hbase
		Hbase.createTable();  			//create table
		CurrencyRate.readRates();		//read currency rates and symbols
		UberRequest.start(5);	//start threads
		UberRequest.readJson();		//read json from table
		UberRequest.DataAnalysis(10,30);	//analysis data
	}
}
