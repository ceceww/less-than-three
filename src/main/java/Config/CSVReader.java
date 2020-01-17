package Config;

import Config.Config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {

    public static List<Config> parseCSV(String fileName, String cvsSplitBy) throws IOException {

        String line = "";
        List<Config> cfgs = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {

            while ((line = br.readLine()) != null) {
                String[] row = line.split(cvsSplitBy);
                Config cfg = new Config(row[0], row[1], Integer.parseInt(row[2]));
                cfgs.add(cfg);
            }

        } catch (IOException e) {
            throw e; //new IOException("Using default values");
        }

        return cfgs;
    }

    public static List<Config> parseCSV(String fileName) throws IOException {
        return parseCSV(fileName, ",");
    }

    public static void main(String[] args) throws IOException {
        var rows = parseCSV("config/config.csv");
        for (Config c : rows) {
            System.out.println(c.hostName);
            System.out.println(c.name);
            System.out.println(c.port);
        }
    }

}
