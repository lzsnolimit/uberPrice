package UberPriceQuery;

public class main {

	public static void startCurrencyRateThreads(int size) {
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

	public static void startUberRequestThreads(int size) {
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

	public static void main(String[] args) throws Exception {

		UberRequest.getAllCities();
		startUberRequestThreads(5);
		CurrencyRate.ParseRateBySymbol();
		System.out.println(CurrencyRate.cities.size() + " cities");
		CurrencyRate.setPosition();
		startCurrencyRateThreads(3);
		// UberRequest.write();

	}
}
