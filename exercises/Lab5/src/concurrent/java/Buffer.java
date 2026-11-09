import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

class Buffer {
    private static final int CAPACIDADE = 50;
    private final List<Integer> data = new ArrayList<>();
    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore vazio = new Semaphore(CAPACIDADE);
    private final Semaphore dadosDisponiveis = new Semaphore(0);

    public void put(int value) throws InterruptedException {
        vazio.acquire();
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            vazio.release();
            throw e;
        }

        try {
            data.add(value);
            System.out.println("Inserted: " + value + " | Buffer size: " + data.size());
        } finally {
            mutex.release();
        }
        dadosDisponiveis.release();
    }

    public int remove() throws InterruptedException {
        dadosDisponiveis.acquire();
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            dadosDisponiveis.release();
            throw e;
        }

        final int value;
        try {
            value = data.remove(0);
            System.out.println("Removed: " + value + " | Buffer size: " + data.size());
        } finally {
            mutex.release();
        }
        vazio.release();
        return value;
    }
}
