
public class Coin {
	public String name = "";
	public int h;
	public int l;
	public int c;
	public double average;
	public double stdev;
	public int bollinger_low;
	public boolean buy_check;

	public Coin(String name) {
		this.name = name;
		this.h = -1;
		this.l = -1;
		this.c = -1;
		average = -1;
		stdev = -1;
		bollinger_low = -1;
		buy_check = false;
	}

	public void Initialization() {
		h = -1;
		l = -1;
		c = -1;
	}

	public void Update_Bollinger() {
		if (average > 0 && stdev > 0)
			bollinger_low = (int) (average - 2 * stdev);
	}
}
