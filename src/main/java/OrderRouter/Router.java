package OrderRouter;

import java.io.IOException;

import Ref.Instrument;

public interface Router {
    enum api {routeOrder, sendCancel, priceAtSize}

    void routeOrder(int id, int sliceId, int size, Instrument i) throws IOException, InterruptedException;

    void sendCancel(int id) throws IOException;

    void priceAtSize(int id, int sliceId, int size, Instrument i) throws IOException;

}
