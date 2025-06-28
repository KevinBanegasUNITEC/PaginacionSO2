import java.util.*;

public class MemoryManager {

    private enum strategy {
        FIFO, LRU, OPT
    }

    private strategy currentStrategy;
    private int frameCount;
    private Queue<String> pagesFIFO = new LinkedList<>();
    private Deque<String> pagesLRU = new ArrayDeque<>();
    private ArrayList<String> pagesOPT = new ArrayList<>();
    private MemoryLoader memoryLoader;

    // Enhanced statistics tracking
    private int pageFaults = 0;
    private int replacements = 0;
    private int diskWrites = 0;
    private int totalMemoryAccesses = 0;
    private long startTime = System.nanoTime();
    private long endTime = System.nanoTime();

    // Timing constants (in nanoseconds)
    private static final long MEMORY_ACCESS_TIME = 100; // 100ns per memory access
    private static final long DISK_ACCESS_TIME = 10_000_000; // 10ms for disk I/O (typical)

    // Track dirty pages for each algorithm
    private Set<String> dirtyPagesFIFO = new HashSet<>();
    private Set<String> dirtyPagesLRU = new HashSet<>();
    private Set<String> dirtyPagesOPT = new HashSet<>();

    public MemoryManager(String filePath, int frameCount, String strategy) {
        this.memoryLoader = new MemoryLoader(filePath);
        this.frameCount = frameCount;
        setStrategy(strategy);
    }

    public void setStrategy(String strategy) {
        switch (strategy.toUpperCase()) {
            case "FIFO":
                this.currentStrategy = MemoryManager.strategy.FIFO;
                break;
            case "LRU":
                this.currentStrategy = MemoryManager.strategy.LRU;
                break;
            case "OPT":
                this.currentStrategy = MemoryManager.strategy.OPT;
                break;
            default:
                throw new IllegalArgumentException("Invalid strategy: " + strategy);
        }
    }

    public strategy getCurrentStrategy() {
        return currentStrategy;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public Queue<String> getPagesFIFO() {
        return pagesFIFO;
    }

    public MemoryLoader getMemoryLoader() {
        return memoryLoader;
    }

    public int getPageFaults() {
        return pageFaults;
    }

    public int getReplacements() {
        return replacements;
    }

    public int getDiskWrites() {
        return diskWrites;
    }

    public double getEffectiveAccessTime() {
        if (totalMemoryAccesses == 0) return 0.0;

        double missRate = (double) pageFaults / totalMemoryAccesses;

        // EAT = hit_rate * memory_access_time + miss_rate * (memory_access_time + disk_access_time)
        // Simplified: EAT = memory_access_time + miss_rate * disk_access_time
        return MEMORY_ACCESS_TIME + (missRate * DISK_ACCESS_TIME);
    }

    public void addPage(String page, boolean isWrite) {
        totalMemoryAccesses++;

        switch (currentStrategy) {
            case FIFO:
                handleFIFO(page, isWrite);
                break;
            case LRU:
                handleLRU(page, isWrite);
                break;
            // OPT is handled separately in OPT() method
        }
    }

    private void handleFIFO(String page, boolean isWrite) {
        boolean found = pagesFIFO.contains(page);

        if (found) {
            // Page hit - mark as dirty if it's a write
            if (isWrite) {
                dirtyPagesFIFO.add(page);
            }
            return;
        }

        // Page fault
        pageFaults++;

        if (pagesFIFO.size() >= frameCount) {
            // Need to replace a page
            String victimPage = pagesFIFO.poll();
            replacements++;

            // Check if victim page is dirty (needs to be written to disk)
            if (dirtyPagesFIFO.contains(victimPage)) {
                diskWrites++;
                dirtyPagesFIFO.remove(victimPage);
            }
        }

        pagesFIFO.offer(page);

        // Mark new page as dirty if it's a write
        if (isWrite) {
            dirtyPagesFIFO.add(page);
        }
    }

    private void handleLRU(String page, boolean isWrite) {
        if (pagesLRU.contains(page)) {
            // Page hit - move to end (most recently used)
            pagesLRU.remove(page);
            pagesLRU.addLast(page);

            // Mark as dirty if it's a write
            if (isWrite) {
                dirtyPagesLRU.add(page);
            }
            return;
        }

        // Page fault
        pageFaults++;

        if (pagesLRU.size() >= frameCount) {
            // Need to replace a page
            String victimPage = pagesLRU.pollFirst();
            replacements++;

            // Check if victim page is dirty (needs to be written to disk)
            if (dirtyPagesLRU.contains(victimPage)) {
                diskWrites++;
                dirtyPagesLRU.remove(victimPage);
            }
        }

        pagesLRU.addLast(page);

        // Mark new page as dirty if it's a write
        if (isWrite) {
            dirtyPagesLRU.add(page);
        }
    }

    public void OPT() {
        pagesOPT.clear();
        dirtyPagesOPT.clear();
        pageFaults = 0;
        replacements = 0;
        diskWrites = 0;
        totalMemoryAccesses = 0;

        for (int i = 0; i < memoryLoader.getReferenceCount(); i++) {
            totalMemoryAccesses++;
            Reference register = memoryLoader.getReferences().get(i);
            boolean isWrite = register.getType() == 'W';
            String page = register.getPage();

            // Check if the page is already in memory
            boolean found = pagesOPT.contains(page);

            if (found) {
                // Page hit - mark as dirty if it's a write
                if (isWrite) {
                    dirtyPagesOPT.add(page);
                }
                continue;
            }

            // Page fault
            pageFaults++;

            // Add the page to OPT list if it is not full
            if (pagesOPT.size() < frameCount) {
                pagesOPT.add(page);
                if (isWrite) {
                    dirtyPagesOPT.add(page);
                }
            } else {
                // Need to replace a page
                String victim = findOptimalVictim(i);
                replacements++;

                // Check if victim page is dirty (needs to be written to disk)
                if (dirtyPagesOPT.contains(victim)) {
                    diskWrites++;
                    dirtyPagesOPT.remove(victim);
                }

                pagesOPT.remove(victim);
                pagesOPT.add(page);

                if (isWrite) {
                    dirtyPagesOPT.add(page);
                }

                if (pagesOPT.size() > frameCount) {
                    throw new IllegalStateException("OPT pages size exceeded frame count");
                }
            }

            // Print progress every 1000 references
            if (i % Math.max(1, memoryLoader.getReferenceCount() / 10) == 0) {
                double percentage = (double) i / memoryLoader.getReferenceCount() * 100;
                System.out.printf("Preprocessing: %.1f%% (%d/%d references)%n",
                        percentage, i, memoryLoader.getReferenceCount());
            }
        }
    }

    public String findOptimalVictim(int startIndex) {
        int maxDistance = -1;
        String victimPage = null;

        for (String p : pagesOPT) {
            int nextUseIndex = findNextUse(p, startIndex + 1);

            if (nextUseIndex == -1) {
                // If the page is never used again, it is the best candidate for replacement
                return p;
            }

            int distance = nextUseIndex - startIndex;
            if (distance > maxDistance) {
                maxDistance = distance;
                victimPage = p;
            }
        }
        return victimPage;
    }

    public int findNextUse(String page, int startIndex) {
        for (int i = startIndex; i < memoryLoader.getReferences().size(); i++) {
            if (memoryLoader.getReferences().get(i).getPage().equals(page)) {
                return i;
            }
        }
        return -1; // Page never used again
    }

    public void run() {
        // Reset statistics
        startTime = System.nanoTime();
        pageFaults = 0;
        replacements = 0;
        diskWrites = 0;
        totalMemoryAccesses = 0;

        if (currentStrategy == MemoryManager.strategy.OPT) {
            OPT();
        } else {
            for (Reference ref : memoryLoader.getReferences()) {
                boolean isWrite = ref.getType() == 'W';
                addPage(ref.getPage(), isWrite);
            }
        }
        endTime = System.nanoTime();

        // Print comprehensive statistics
        printStatistics();
    }

    private void printStatistics() {
        System.out.println("\n========== MEMORY MANAGEMENT STATISTICS ==========");
        System.out.println("Algorithm: " + currentStrategy);
        System.out.println("Frame Count: " + frameCount);
        System.out.println("Total Memory Accesses: " + totalMemoryAccesses);
        System.out.println("Page Faults: " + pageFaults);
        System.out.println("Replacements: " + replacements);
        System.out.println("Disk Writes: " + diskWrites);

        if (totalMemoryAccesses > 0) {
            double hitRate = 1.0 - ((double) pageFaults / totalMemoryAccesses);
            double missRate = (double) pageFaults / totalMemoryAccesses;

            System.out.printf("Hit Rate: %.2f%% (%.0f hits)\n",
                    hitRate * 100, hitRate * totalMemoryAccesses);
            System.out.printf("Miss Rate: %.2f%% (%d misses)\n",
                    missRate * 100, pageFaults);
            System.out.printf("Effective Access Time: %.2f ns\n", getEffectiveAccessTime());
            long runTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            System.out.println("Running Time: " + runTime + " ms");
        }

        System.out.println("==================================================\n");
    }
}