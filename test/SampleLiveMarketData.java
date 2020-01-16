import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import LiveMarketData.LiveMarketData;
import OrderManager.Order;
import Ref.Instrument;

//TODO this should really be in its own thread
public class SampleLiveMarketData implements LiveMarketData {

    private static final Random RANDOM_NUM_GENERATOR = new Random();
    private static Map<String, Double> marketPrices = new HashMap<String, Double>();

    public void setPrice(Order o) {

        String currentOrderRic = o.instrument.ric.ric;
        if (marketPrices.containsKey(currentOrderRic)) {
            double exPrice = marketPrices.get(currentOrderRic) + RANDOM_NUM_GENERATOR.nextDouble() * 4;
            o.initialMarketPrice = exPrice;

            System.out.println("Price set for " + currentOrderRic + " of " + exPrice);
        } else {
            double price = 500  * RANDOM_NUM_GENERATOR.nextDouble();
            o.initialMarketPrice = price;
            marketPrices.put(currentOrderRic, price);

            System.out.println("Price set for " + currentOrderRic + " of " + price);
        }

    }

}
