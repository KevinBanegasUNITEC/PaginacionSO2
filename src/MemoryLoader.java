import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class MemoryLoader {
    private ArrayList<Reference> references;
    private File file;

    public MemoryLoader(String filePath) {
        this.file = new File(filePath);
        this.references = new ArrayList<>();
        loadReferences();
    }

    public ArrayList<Reference> getReferences() {
        return references;
    }

    public void loadReferences() {
        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return;
        }

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split("\\s+");
                    if (parts.length == 2) {
                        String register = parts[0];
                        char type = parts[1].charAt(0);
                        String page = addressToPage(register);
                        references.add(new Reference(page, type));
                    } else {
                        System.out.println("Invalid line format: " + line);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    public int getReferenceCount() {
        return references.size();
    }

    public void printReferences() {
        if (references.isEmpty()) {
            System.out.println("No references loaded.");
            return;
        }
        System.out.println("Register | Type");
        for (Reference ref : references) {
            System.out.println(ref);
        }
    }

    public String addressToPage(String address) {
        // Assuming page size is 4096 bytes (4KB)
        int pageSize = 4096;
        long addr = Long.parseLong(address, 16);
        long pageNumber = addr / pageSize;
        return String.format("%08x", pageNumber);
    }
}
