package OrderManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Database.Database;
import LiveMarketData.LiveMarketData;
import OrderClient.NewOrderSingle;
import OrderRouter.Router;
import Ref.FIXCodes;
import TradeScreen.TradeScreen;

public class OrderManager {

    private static LiveMarketData liveMarketData;
    private HashMap<Integer, Order> orders = new HashMap<>();
    //debugger will do this line as it gives state to the object
    //currently recording the number of new order messages we get. TODO why? use it for more?
    private int id = 0; //debugger will do this line as it gives state to the object
    private Socket[] orderRouters; //debugger will skip these lines as they dissapear at compile time into 'the object'/stack
    private Socket[] clients;
    private Socket trader;

    private Socket connect(InetSocketAddress location) throws InterruptedException {
        boolean connected = false;
        int tryCounter = 0;
        while (!connected && tryCounter < 600) {
            try {
                Socket s = new Socket(location.getHostName(), location.getPort());
                s.setKeepAlive(true);
                return s;
            } catch (IOException e) {
                Thread.sleep(1000);
                tryCounter++;
            }
        }
        System.out.println("Failed to connect to " + location.toString());
        return null;
    }

    //@param args the command line arguments

    //TODO refactor this mess
    /**
     *
     * @param orderRouters
     * @param clients
     * @param trader
     * @param liveMarketData
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public OrderManager(List<InetSocketAddress> orderRouters, List<InetSocketAddress> clients, InetSocketAddress trader,
                        LiveMarketData liveMarketData) throws IOException, ClassNotFoundException, InterruptedException {

        OrderManager.liveMarketData = liveMarketData;
        this.trader = connect(trader);
        //for the router connections, copy the input array into our object field.
        //but rather than taking the address we create a socket+ephemeral port and connect it to the address

        this.orderRouters = new Socket[orderRouters.size()];
        int i = 0; //need a counter for the output array
        for (InetSocketAddress location : orderRouters) {
            this.orderRouters[i] = connect(location);
            i++;
        }

        //repeat for the client connections
        this.clients = new Socket[clients.size()];
        i = 0;
        for (InetSocketAddress location : clients) {
            this.clients[i] = connect(location);
            i++;
        }

        int clientId, routerId;
        ObjectInputStream is;
        String method;

        while (true) { //main loop, wait for a message, then process it

            //TODO this is pretty cpu intensive, use a more modern polling/interrupt/select approach
            //we want to use the arrayindex as the clientId, so use traditional for loop instead of foreach
            for (clientId = 0; clientId < this.clients.length; clientId++) { //check if we have data on any of the sockets
                processClientMessage(clientId);
            }
            for (routerId = 0; routerId < this.orderRouters.length; routerId++) { //check if we have data on any of the sockets
                processRouterMessage(routerId);
            }

            if (0 < this.trader.getInputStream().available()) {
                is = new ObjectInputStream(this.trader.getInputStream());
                method = (String) is.readObject();
                System.out.println(Thread.currentThread().getName() + " calling " + method);
                switch (method) {
                    case "acceptOrder":
                        acceptOrder(is.readInt());
                        break;
                    case "sliceOrder":
                        sliceOrder(is.readInt(), is.readInt());
                }
            }
        }
    }

    private  void processClientMessage(int clientId) throws IOException, ClassNotFoundException {
        Socket client;
        ObjectInputStream is;
        String method;
        InputStream cis;

        client = this.clients[clientId];
        if (client != null) {
            cis = client.getInputStream(); // for each client get input stream

            if (0 < cis.available()) { //if we have part of a message ready to read, assuming this doesn't fragment messages
                is = new ObjectInputStream(cis); //create an object inputstream, this is a pretty stupid way of doing it,

                //  TO DO why not create it once rather than every time around the loop
                method = (String) is.readObject();

                System.out.println(Thread.currentThread().getName() + " calling " + method);
                //determine the type of message and process it
                //call the newOrder message with the clientId and the message (clientMessageId,NewOrderSingle)
                if ("newOrderSingle".equals(method)) {
                    newOrder(clientId, is.readInt(), (NewOrderSingle) is.readObject());
                } else {
                    System.err.println("Unknown message type");
                }
            }
        }
    }
    private  void processRouterMessage(int routerId) throws IOException, ClassNotFoundException {
        Socket router;
        ObjectInputStream is;
        String method;
        InputStream ris;
        router = this.orderRouters[routerId];
        if (router != null) {
            ris = router.getInputStream();

            if (0 < ris.available()) { //if we have part of a message ready to read, assuming this doesn't fragment messages
                is = new ObjectInputStream(ris);
                //TO DO why not create it once rather than every time around the loop
                method = (String) is.readObject();
                System.out.println(Thread.currentThread().getName() + " calling " + method);
                switch (method) { //determine the type of message and process it
                    case "bestPrice":
                        int OrderId = is.readInt();
                        int SliceId = is.readInt();
                        Order slice = orders.get(OrderId).slices.get(SliceId);
                        slice.bestPrices[routerId] = is.readDouble();
                        slice.bestPriceCount += 1;
                        if (slice.bestPriceCount == slice.bestPrices.length)
                            reallyRouteOrder(SliceId, slice);
                        break;
                    case "newFill":
                        newFill(is.readInt(), is.readInt(), is.readInt(), is.readDouble());
                        break;
                }
            }
        }
    }

    private void newOrder(int clientId, int clientOrderId, NewOrderSingle nos) throws IOException {
        orders.put(id, new Order(clientId, clientOrderId, nos.instrument, nos.size));
        //send a message to the client with 39=A; //OrdStatus is Fix 39, 'A' is 'Pending New'
        ObjectOutputStream os = new ObjectOutputStream(clients[clientId].getOutputStream());
        //newOrderSingle acknowledgement
        //ClOrdId is 11=
        os.writeObject(FIXCodes.ClOrdID + "=" + clientOrderId + ";" + FIXCodes.MsgType + "=A;" + FIXCodes.OrdStatus + "=A;");
        os.flush();
        sendOrderToTrader(id, orders.get(id), TradeScreen.api.newOrder);
        //send the new order to the trading screen
        //don't do anything else with the order, as we are simulating high touch orders and so need to wait for the trader to accept the order
        id++;
    }

    private void sendOrderToTrader(int id, Order o, Object method) throws IOException {
        ObjectOutputStream ost = new ObjectOutputStream(trader.getOutputStream());
        ost.writeObject(method);
        ost.writeInt(id);
        ost.writeObject(o);
        ost.flush();
    }

    public void acceptOrder(int id) throws IOException {
        Order o = orders.get(id);
        if (o.OrdStatus != 'A') { //Pending New
            System.out.println("error accepting order that has already been accepted");
            return;
        }
        o.OrdStatus = '0'; //New
        ObjectOutputStream os = new ObjectOutputStream(clients[o.clientid].getOutputStream());
        //newOrderSingle acknowledgement
        os.writeObject(FIXCodes.ClOrdID + "=" + o.clientOrderID + FIXCodes.MsgType + "=A;" + FIXCodes.OrdStatus +
                "=" + o.OrdStatus + ";");
        os.flush();

        price(id, o);
    }

    public void sliceOrder(int id, int sliceSize) throws IOException {
        Order o = orders.get(id);
        //slice the order. We have to check this is a valid size.
        //Order has a list of slices, and a list of fills, each slice is a
        // childorder and each fill is associated with either a child order or the original order
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

    private void internalCross(int id, Order o) throws IOException {

        for (Map.Entry<Integer, Order> entry : orders.entrySet()) {

            if (entry.getKey().intValue() == id) continue;
            Order matchingOrder = entry.getValue();
            if (!(matchingOrder.instrument.equals(o.instrument) && matchingOrder.initialMarketPrice == o.initialMarketPrice))
                continue;
            //TODO add support here and in Order for limit orders
            int sizeBefore = o.sizeRemaining();
            o.cross(matchingOrder);
            if (sizeBefore != o.sizeRemaining()) {
                sendOrderToTrader(id, o, TradeScreen.api.cross);
            }
        }
    }

    private void cancelOrder(int id) throws IOException {
        Order o = orders.get(id);
        ObjectOutputStream os;
        // 4 = Cancelled
        o.OrdStatus = '4';
        os = new ObjectOutputStream(clients[o.clientid].getOutputStream());
        os.writeObject(FIXCodes.ClOrdID + "=" + o.clientid + ";" + FIXCodes.MsgType + "=A;" + FIXCodes.OrdStatus
                + "=" + o.OrdStatus + ";");
        os.flush();

        for (Socket r : orderRouters) {
            os = new ObjectOutputStream(r.getOutputStream());
            os.writeObject(Router.api.sendCancel);
            os.writeInt(id);
            os.flush();
        }

        // sendCancel(o.);
    }

    private void newFill(int id, int sliceId, int size, double price) throws IOException {
        Order o = orders.get(id);
        o.slices.get(sliceId).createFill(size, price);
        if (o.sizeRemaining() == 0) {
            Database.write(o);
        }
        sendOrderToTrader(id, o, TradeScreen.api.fill);
    }

    private void routeOrder(int id, int sliceId, int size, Order order) throws IOException {
        for (Socket r : orderRouters) {
            ObjectOutputStream os = new ObjectOutputStream(r.getOutputStream());
            os.writeObject(Router.api.priceAtSize);
            os.writeInt(id);
            os.writeInt(sliceId);
            os.writeInt(order.sizeRemaining());
            os.writeObject(order.instrument);
            os.flush();
        }
        //need to wait for these prices to come back before routing
        order.bestPrices = new double[orderRouters.length];
        order.bestPriceCount = 0;
    }

    private void reallyRouteOrder(int sliceId, Order o) throws IOException {
        //TODO this assumes we are buying rather than selling
        int minIndex = 0;
        double min = o.bestPrices[0];
        for (int i = 1; i < o.bestPrices.length; i++) {
            if (min > o.bestPrices[i]) {
                minIndex = i;
                min = o.bestPrices[i];
            }
        }
        ObjectOutputStream os = new ObjectOutputStream(orderRouters[minIndex].getOutputStream());
        os.writeObject(Router.api.routeOrder);
        os.writeInt(o.id);
        os.writeInt(sliceId);
        os.writeInt(o.sizeRemaining());
        os.writeObject(o.instrument);
        os.flush();
    }

    private void sendCancel(int id) throws IOException {
        Order o = orders.get(id);
        // orderRouter.sendCancel(o);
        //order.orderRouter.writeObject(order);
    }

    private void price(int id, Order o) throws IOException {
        liveMarketData.setPrice(o);
        sendOrderToTrader(id, o, TradeScreen.api.price);
    }
}