import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import org.json.*;

public class YongStar {
	private Api_Client api;
	private Minute_Unit m[] = new Minute_Unit[20];
	private int current_minute;
	private int minute_index;
	private int count;
	private String buyName;
	private double buyUnits;
	private int availableKRW;
	static boolean buyOk;

	final static double bollinger_buy = 0.999;
	final static double bollinger_sell = 1.002;
	private int price_limit = 0;
	private int available_limit = 120000;

	public YongStar(String api_key, String secret_key) {
		api = new Api_Client(api_key, secret_key);
		count = 0;
		for (int i = 0; i < 20; i++) {
			m[i] = new Minute_Unit(api_key, secret_key);
		}
		long time = System.currentTimeMillis();
		SimpleDateFormat dayTime = new SimpleDateFormat("mm");
		String str = dayTime.format(new Date(time));

		current_minute = Integer.parseInt(str);
		minute_index = current_minute % 20;

		buyOk = true;
		buyName = "";
		buyUnits = -1;
		availableKRW = 0;
	}

	public void AutoStart() {
		long time;
		int before_minute;
		SimpleDateFormat dayTime = new SimpleDateFormat("mm");
		SimpleDateFormat dayTime2 = new SimpleDateFormat("yyyy-MM-dd kk:mm");
		String str_time = "";
		String str_date = "";

		while (true) {
			time = System.currentTimeMillis();
			str_date = dayTime.format(new Date(time));
			str_time = dayTime2.format(new Date(time)); // 현재 시간 알아보는 용 (출력용)

			availableKRW = getMyAvailableKRW();

			before_minute = current_minute;
			current_minute = Integer.parseInt(str_date);
			minute_index = current_minute % 20;
			//	System.out.println(str_time + "\tcount : " + count + "\t\tavailableKRW = " + availableKRW);

			if (current_minute != before_minute) {
				count++;
				System.out.println(str_time);
				for(int i=0;i<12;i++)
					System.out.println(m[(minute_index+19)%20].coin[i].name+"\t\th = "+m[(minute_index+19)%20].coin[i].h+"\tl = "+m[(minute_index+19)%20].coin[i].l+"\t"+count);
				m[minute_index].Initialization();
			}

			m[minute_index].Update();
			if (count > 20) {
				Mean();
				StandardDeviation();
				m[minute_index].Update_Bollinger();
			}
			double c;
			double l;

			if (count > 21) {
				for (int i = 0; i < 12; i++) {
					c = m[minute_index].coin[i].c;
					l=m[minute_index].coin[i].l;
					if (buyOk && c > 0 && c < (m[minute_index].coin[i].bollinger_low * bollinger_buy)&&c>l) {
						if (m[minute_index].coin[i].buy_check == true) {
							buyCheck(i);
						} else {
							if (buyOrNot(i) && availableKRW > available_limit) {
								BuySell bs = new BuySell(i);
								bs.start();
								try {
									Thread.sleep(1500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}

			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void Mean() {
		double sum[] = new double[12];
		for (int i = 0; i < 12; i++)
			sum[i] = 0.0;

		for (int i = 0; i < 12; i++) {
			for (int j = 0; j < 20; j++) {
				sum[i] += ((m[j].coin[i].h + m[j].coin[i].c + m[j].coin[i].l) / 3);
			}
			m[minute_index].coin[i].average = sum[i] / 20;
		}
	}

	public void StandardDeviation() {
		double sum[] = new double[12];
		double diff = 0.0;
		double mean[] = new double[12];

		for (int i = 0; i < 12; i++) {
			sum[i] = 0.0;
			mean[i] = m[minute_index].coin[i].average;
		}

		for (int i = 0; i < 12; i++) {
			for (int j = 0; j < 20; j++) {
				diff = mean[i] - (m[j].coin[i].h + m[j].coin[i].c + m[j].coin[i].l) / 3;
				sum[i] += diff * diff;
			}
			m[minute_index].coin[i].stdev = Math.sqrt(sum[i] / 20);
		}
	}

	public int BuyCoin(int coinNum) {
		String coinName = m[minute_index].coin[coinNum].name;
		double coinUnits = 0.0;
		int bp = 0;

		if (coinName.equals("BTC"))
			coinUnits = 0.006;
		else if (coinName.equals("ETH"))
			coinUnits = 0.07;
		else if (coinName.equals("DASH"))
			coinUnits = 0.08;
		else if (coinName.equals("LTC"))
			coinUnits = 0.4;
		else if (coinName.equals("ETC"))
			coinUnits = 2;
		else if (coinName.equals("XRP"))
			coinUnits = 47;
		else if (coinName.equals("BCH"))
			coinUnits = 0.04;
		else if (coinName.equals("XMR"))
			coinUnits = 0.22;
		else if (coinName.equals("ZEC"))
			coinUnits = 0.14;
		else if (coinName.equals("QTUM"))
			coinUnits = 1.9;
		else if (coinName.equals("BTG"))
			coinUnits = 0.4;
		else if (coinName.equals("EOS"))
			coinUnits = 7;

		HashMap<String, String> rgParams = new HashMap<String, String>();
		rgParams.put("units", Double.toString(coinUnits));
		rgParams.put("currency", coinName);

		String statusCode = "";
		String result = "";

		try {
			result = api.callApi("/trade/market_buy", rgParams);
			// System.out.println(result);
			JSONObject obj = new JSONObject(result);
			statusCode = obj.getString("status");
			bp = Integer.parseInt(obj.getJSONArray("data").getJSONObject(0).getString("price"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (statusCode.equals("0000")) {
			buyName = coinName;
			buyUnits = coinUnits;
			m[minute_index].coin[coinNum].buy_check = true;
			return bp;
		} else {
			return -1;
		}
	}

	public boolean SellCoin(int coinNum, int buyPrice) {
		int askingPriceUnit = 0;
		JSONObject obj;
		String status = "";

		if (buyName.equals("BTC"))
			askingPriceUnit = 1000;
		else if (buyName.equals("ETH"))
			askingPriceUnit = 1000;
		else if (buyName.equals("DASH"))
			askingPriceUnit = 1000;
		else if (buyName.equals("LTC"))
			askingPriceUnit = 100;
		else if (buyName.equals("ETC"))
			askingPriceUnit = 10;
		else if (buyName.equals("XRP"))
			askingPriceUnit = 1;
		else if (buyName.equals("BCH"))
			askingPriceUnit = 1000;
		else if (buyName.equals("XMR"))
			askingPriceUnit = 100;
		else if (buyName.equals("ZEC"))
			askingPriceUnit = 500;
		else if (buyName.equals("QTUM"))
			askingPriceUnit = 50;
		else if (buyName.equals("BTG"))
			askingPriceUnit = 100;
		else if (buyName.equals("EOS"))
			askingPriceUnit = 10;

		int tempPrice = (int) ((double) buyPrice * bollinger_sell);

		int askingPrice = tempPrice - (tempPrice % askingPriceUnit);

		if (getClosingPrice(coinNum) > askingPrice) {
			if (marketSell(coinNum, buyUnits)) {
				System.out.println(m[minute_index].coin[coinNum].name + " 시장가 매도");
				return true;
			} else
				return false;
		}

		HashMap<String, String> rgParams = new HashMap<String, String>();
		rgParams.put("order_currency", buyName);
		rgParams.put("Payment_currency", "KRW");
		rgParams.put("units", Double.toString(buyUnits));
		rgParams.put("price", Integer.toString(askingPrice));
		rgParams.put("type", "ask");

		try {
			String result = api.callApi("/trade/place", rgParams);
			System.out.println(result);
			obj = new JSONObject(result);
			status = obj.getString("status");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (status.equals("0000")) {
			System.out.println(m[minute_index].coin[coinNum].name + " " + askingPrice + " 매도 주문");
			return true;
		} else
			return false;
	}

	public void buyCheck(int coinNum) {
		if (m[minute_index].coin[coinNum].c > m[minute_index].coin[coinNum].bollinger_low) {
			m[minute_index].coin[coinNum].buy_check = false;
		}
	}

	class BuySell extends Thread {
		int coinNum;
		int buyPrice;
		int count;

		BuySell(int coinNum) {
			count = 0;
			this.coinNum = coinNum;
		}

		public synchronized void run() {
			count = 0;
			try {
				if ((buyPrice = BuyCoin(coinNum)) >= 0) {
					buyOk = false;
					System.out.println(m[minute_index].coin[coinNum].name + " " + buyPrice + "매수");
					Thread.sleep(12000);
					while (true) {
						if (count > 20)
							break;
						Thread.sleep(2000);
						if (SellCoin(coinNum, buyPrice)) {
							buyOk = true;
							break;
						}
						count++;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private int getMyAvailableKRW() {
		HashMap<String, String> rgParams = new HashMap<String, String>();
		int availableKRW = 0;

		try {
			String result = api.callApi("/info/balance", rgParams);
			JSONObject obj = new JSONObject(result);
			availableKRW = obj.getJSONObject("data").getInt("available_krw");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return availableKRW;
	}

	public boolean setPriceLimit(int value) {
		price_limit = value;
		System.out.println("구매 설정 가격 : " + price_limit + "원");
		if (price_limit > 0)
			return true;
		else
			return false;
	}

	private boolean buyOrNot(int n) {
		/*
		 * if((m[minute_index].coin[n].c/m[(minute_index+19)%20].coin[n].c)>1.015)
		 * return false;
		 */

		int i1 = (minute_index + 15) % 20;
		int i2 = minute_index;
		int before = (m[i1].coin[n].h + m[i1].coin[n].c + m[i1].coin[n].l) / 3;
		int after = (m[i2].coin[n].h + m[i2].coin[n].c + m[i2].coin[n].l) / 3;

		if (before < after) {
			for (int i = 15; i < 19; i++) {
				i1 = (minute_index + i - 1) % 20;
				i2 = (minute_index + i) % 20;
				before = (int) ((m[i1].coin[n].h + m[i1].coin[n].c + m[i1].coin[n].l) / 3);
				after = (int) ((m[i2].coin[n].h + m[i2].coin[n].c + m[i2].coin[n].l) / 3);
				if (before > after)
					return true;
			}
		} else
			return true;
		/*
		 * else { for (int i = 15; i < 20; i++) { i1 = (minute_index + i - 1) % 20; i2 =
		 * (minute_index + i) % 20; before = (int) ((m[i1].coin[n].h + m[i1].coin[n].c +
		 * m[i1].coin[n].l) / 3); after = (int) ((m[i2].coin[n].h + m[i2].coin[n].c +
		 * m[i2].coin[n].l) / 3); if (before < after) return true; } }
		 */
		return false;
	}

	private int getClosingPrice(int coinNum) {
		HashMap<String, String> rgParams = new HashMap<String, String>();
		int closingPrice = 0;

		try {
			String result = api.callApi("/public/ticker/" + m[minute_index].coin[coinNum].name, rgParams);
			JSONObject obj = new JSONObject(result);
			closingPrice = Integer.parseInt(obj.getJSONObject("data").getString("closing_price"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return closingPrice;
	}

	private boolean marketSell(int coinNum, double unit) {
		HashMap<String, String> rgParams = new HashMap<String, String>();
		rgParams.put("units", Double.toString(unit));
		rgParams.put("currency", m[minute_index].coin[coinNum].name);

		String status = "";

		try {
			String result = api.callApi("/trade/market_sell", rgParams);
			JSONObject obj = new JSONObject(result);
			status = obj.getString("status");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (status.equals("0000"))
			return true;
		else
			return false;
	}
}
