import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import Config.Config;
import Config.CSVReader;
import LiveMarketData.LiveMarketData;
import OrderClient.SampleClient;
import OrderClient.Trader;
import OrderManager.OrderManager;

import Database.Database;

public class Main {
    public static void main(String[] args) {

        Database.dbConnect();

        List<Config> configs = new ArrayList<>();
        try {
            configs = CSVReader.parseCSV("config/config.csv");
        } catch (IOException e) {
            System.out.println("Using default values");
            configs.add(new Config("Client 1","localhost",2000));
            configs.add(new Config("Client 2","localhost",2001));
            configs.add(new Config("Router LSE","localhost",2010));
            configs.add(new Config("Router BATE","localhost",2011));
            configs.add(new Config("OrderClient.Trader James", "localhost", 2020));
        }

        System.out.println("TEST: this program tests order manager");

        List<InetSocketAddress> clients = new ArrayList<>();
        List<InetSocketAddress> routers = new ArrayList<>();
        List<InetSocketAddress> traders = new ArrayList<>();

        for (Config c :
                configs) {
            if (c.name.contains("Trader")) {
                new Trader(c.name, c.port).start();
                traders.add(new InetSocketAddress(c.hostName, c.port));
            } else if (c.name.contains("Router")) {
                new SampleRouter(c.name, c.port).start();
                routers.add(new InetSocketAddress(c.hostName, c.port));
            } else if (c.name.contains("Client")) {
                new MockClient(c.name, c.port).start();
                clients.add(new InetSocketAddress(c.hostName, c.port));
            }
        }

        LiveMarketData liveMarketData = new SampleLiveMarketData();
        (new MockOM("Order Manager", routers, clients, traders.get(0), liveMarketData)).start();

    }
}

class MockClient extends Thread {

    private int port;

    MockClient(String name, int port) {
        this.port = port;
        this.setName(name);
    }

    public void run() {
        try {
            SampleClient client = new SampleClient(port);
            if (port == 2000) {
                client.sendOrder();
                int id = client.sendOrder();
                //TODO client.sendCancel(id);
                client.messageHandler();
            } else {
                client.sendOrder();
                client.messageHandler();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}

class MockOM extends Thread {
    List<InetSocketAddress> clients;
    List<InetSocketAddress> routers;
    InetSocketAddress trader;
    LiveMarketData liveMarketData;

    MockOM(String name, List<InetSocketAddress> routers, List<InetSocketAddress> clients, InetSocketAddress trader,
           LiveMarketData liveMarketData) {
        this.clients = clients;
        this.routers = routers;
        this.trader = trader;
        this.liveMarketData = liveMarketData;
        this.setName(name);
    }

    @Override
    public void run() {
        try {
            //In order to debug constructors you can do F5 F7 F5
            new OrderManager(routers, clients, trader, liveMarketData);
        } catch (IOException | ClassNotFoundException | InterruptedException ex) {
            Logger.getLogger(MockOM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}