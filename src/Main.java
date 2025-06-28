public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java Main <trace_file> <frame_count> <strategy>");
            return;
        }
        MemoryManager memoryManager = new MemoryManager(args[0], Integer.parseInt(args[1]), args[2]);
        memoryManager.run();
    }
}