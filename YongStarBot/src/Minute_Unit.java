import java.util.HashMap;
import org.json.*;

public class Minute_Unit {
	public int c;
	String result = "";
	int price;
	public Coin coin[] = new Coin[12];

	Api_Client api;

	public Minute_Unit(String api_key, String secret_key) {
		api = new Api_Client(api_key, secret_key);
		coin[0] = new Coin("BTC");
		coin[1] = new Coin("ETH");
		coin[2] = new Coin("DASH");
		coin[3] = new Coin("LTC");
		coin[4] = new Coin("ETC");
		coin[5] = new Coin("XRP");
		coin[6] = new Coin("BCH");
		coin[7] = new Coin("XMR");
		coin[8] = new Coin("ZEC");
		coin[9] = new Coin("QTUM");
		coin[10] = new Coin("BTG");
		coin[11] = new Coin("EOS");
	}

	public void Update() {
		HashMap<String, String> rgParams = new HashMap<String, String>();
		int c=-1;
		double c_d=-1.0;
		try {
			result = api.callApi("/public/ticker/ALL", rgParams);
			JSONObject obj = new JSONObject(result);
			for (int i = 0; i < 12; i++) {
				c_d = Double
						.parseDouble(obj.getJSONObject("data").getJSONObject(coin[i].name).getString("closing_price"));
				c=(int)c_d;
				coin[i].c = c;
				if (coin[i].h < 0 || coin[i].h < c)
					coin[i].h = c;
				if (coin[i].l < 0 || coin[i].l > c)
					coin[i].l = c;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*
		for (int i = 0; i < 12; i++) {
			int c_ = coin[i].c;
			int h_ = coin[i].h;
			int l_ = coin[i].l;

			System.out.println(coin[i].name + " c = " + c_ + "\th =  " + h_ + "l = " + l_ + "\t\tavg = " + coin[i].average
					+ "\tstdev = " + coin[i].stdev);
		}
		*/
	}

	public void Initialization() {
		for (int i = 0; i < 12; i++)
			coin[i].Initialization();
	}

	public void Update_Bollinger() {
		for (int i = 0; i < 12; i++)
			coin[i].Update_Bollinger();
	}
}
