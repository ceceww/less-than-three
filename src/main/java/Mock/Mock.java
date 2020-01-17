package Mock;

public class Mock implements MockI {
    public static void show(String out) {
        System.err.println(Thread.currentThread().getName() + ":" + out);
    }
}