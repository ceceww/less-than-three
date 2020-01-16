import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import LiveMarketData.LiveMarketData;
import OrderManager.Order;
import Ref.Instrument;

//TODO this should really be in its own thread
public class SampleLiveMarketData implements LiveMarketData {

    private static final Random RANDOM_NUM_GENERATOR = new Random();
    private static Map<String, Double> marketPrices = new HashMap<>();

    public void setPrice(Order o) {

        String currentOrderRic = o.instrument.ric.ric;
        if (marketPrices.containsKey(currentOrderRic)) {
            o.initialMarketPrice = marketPrices.get(currentOrderRic) + RANDOM_NUM_GENERATOR.nextDouble() * 3;
        } else {
            o.initialMarketPrice = 199 * RANDOM_NUM_GENERATOR.nextDouble();
            marketPrices.put(currentOrderRic, o.initialMarketPrice);

        }
        System.out.println("Price set for " + currentOrderRic + " of " + o.initialMarketPrice);
    }

}
