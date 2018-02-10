import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.Date;
import java.util.HashMap;
import org.json.*;

public class Main {
	public static void main(String args[]) {
		String api_key = "";
		String secret_key = "";

		Scanner scan = new Scanner(System.in);
		System.out.print("Enter the Password : ");
		String name = scan.nextLine();
		
		if (name.equals("1313")) {
			api_key = "";
			secret_key = "";
		}
		else {
			System.out.println("Reconfirm your password");
		}
		while (true) {
			try {
				YongStar y = new YongStar(api_key, secret_key);
				// y.setPriceLimit(price_limit);
				y.AutoStart();
				Thread.sleep(100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}