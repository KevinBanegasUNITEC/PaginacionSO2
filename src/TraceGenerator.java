
import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class TraceGenerator {
    public void generateTrace(String outputFile, int size, int limit) {
        Random rand = new Random();
        ArrayList<Integer> registers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int direccion = rand.nextInt();
            registers.add(direccion);
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            int mean = registers.size() / 2; // e.g., index 5
            double stdDev = 2.0;
            for (int i = 0; i < limit; i++) {
                int index;
                do {
                    index = (int) (rand.nextGaussian() * stdDev + mean);
                } while (index < 0 || index >= registers.size());
                int register = registers.get(index);
                String hex = String.format("%08x", register);
                char type = rand.nextDouble() < 0.9 ? 'R' : 'W';
                writer.write(hex + " " + type + "\n");
            }

            System.out.println("New Trace File: " + outputFile);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        printSortedTrace(sortTrace(outputFile));

    }

    public ArrayList<Reference> sortTrace(String inputFile) {
        File file = new File(inputFile);
        ArrayList<Reference> references = new ArrayList<>();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split("\\s+");
                    if (parts.length == 2) {
                        String register = parts[0];
                        char type = parts[1].charAt(0);
                        references.add(new Reference(register, type));
                    } else {
                        System.out.println("Invalid line format: " + line);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        references.sort((r1, r2) -> {
            int cmp = r1.getPage().compareTo(r2.getPage());
            if (cmp == 0) {
                return Character.compare(r1.getType(), r2.getType());
            }
            return cmp;
        });
        return references;
    }

    public void printSortedTrace(ArrayList<Reference> references) {
        if (references.isEmpty()) {
            System.out.println("No references loaded.");
            return;
        }
        System.out.println("Register | Type");
        for (Reference ref : references) {
            System.out.println(ref);
        }
    }
}
