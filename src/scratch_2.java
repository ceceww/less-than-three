import OrderClient.Client;
import OrderClient.NewOrderSingle;
import OrderClient.Trader;
import OrderManager.Order;
import OrderRouter.Router;
import TradeScreen.TradeScreen;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OrderManager {

    public Map<Integer, Order> orders = new HashMap<>();
    private int lastID = 0;
    public List<Socket> clientSockets;
    public List<Socket> routerSockets;
    Socket traderSocket;

    private Order currentOrder;

    public void newOrder(int clientId, int clientOrderId, NewOrderSingle nos) throws IOException {

        orders.put(lastID, new Order(clientId, clientOrderId, nos.instrument, nos.size));
        ObjectOutputStream os = new ObjectOutputStream(clientSockets.get(clientId).getOutputStream());

        os.writeObject("11=" + clientOrderId + ";35=A;39=A;");
        os.flush();

        sendOrderToTrader(lastID, orders.get(lastID), TradeScreen.api.newOrder);

        lastID++;
    }

    /**
     * Accept order from trader and send to price
     * @param id
     */
    public void acceptOrder(int id) throws IOException {

        currentOrder = orders.get(id);

        if (currentOrder.OrdStatus != 'A') {
            System.out.println("error accepting order that has already been accepted");
            return;
        }

        currentOrder.OrdStatus = '0'; //New
        ObjectOutputStream os = new ObjectOutputStream(clientSockets.get(currentOrder.clientid).getOutputStream());
        os.writeObject("11=" + currentOrder.clientOrderID + ";35=A;39=0");
        os.flush();

        price(id, currentOrder);
    }

    public void slice(int id, int sliceSize) {

        currentOrder = orders.get(id);
        //slice the order. We have to check this is a valid size.
        //Order has a list of slices, and a list of fills, each slice is a childorder and each fill is associated
        // with either a child order or the original order
        if (sliceSize > o.sizeRemaining() - o.sliceSizes()) {
            System.out.println("error sliceSize is bigger than remaining size to be filled on the order");
            return;
        }

        int sliceId = o.newSlice(sliceSize);
        Order slice = o.slices.get(sliceId);
        internalCross(id, slice);
        int sizeRemaining = o.slices.get(sliceId).sizeRemaining();
        if (sizeRemaining > 0) {
            routeOrder(id, sliceId, sizeRemaining, slice);
        }

    }

    /**
     *
     * @param id of order to be cancelled
     */
    public void cancel(int id) {

        currentOrder = orders.get(id);
        // 4 = Canceled
        currentOrder.OrdStatus = '4'; //TODO make an enum for FIX codes (see https://www.onixs.biz/fix-dictionary/4.2/tagNum_39.html)
        ObjectOutputStream os = new ObjectOutputStream(clients[o.clientid].getOutputStream());
        ObjectOutputStream os = new ObjectOutputStream(clients[o.clientid].getOutputStream());

        for (Socket r : routerSockets) {
            ObjectOutputStream os = new ObjectOutputStream(r.getOutputStream());
            os.writeObject(Router.api.sendCancel);
            os.writeInt(id);
            os.writeInt(sliceId);
            os.writeInt(order.sizeRemaining());
            os.writeObject(order.instrument);
            os.flush();
        }

    }

    public void newFill() {

    }

    private void price(int id, Order o) {

    }

    private void cancelReplace() {

    }

    private void sendOrderToTrader(int id, Order o, Object method) throws IOException {
        ObjectOutputStream ost = new ObjectOutputStream(traderSocket.getOutputStream());
        ost.writeObject(method);
        ost.writeInt(id);
        ost.writeObject(o);
        ost.flush();
    }

}