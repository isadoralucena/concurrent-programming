import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

class Buffer {
    private final List<Integer> data = new ArrayList<>();
    private static final int CAPACIDADE = 50;

    // Controla o acesso exclusivo ao buffer
    private final Semaphore mutex = new Semaphore(1);

    // Controla as posições livres
    private final Semaphore vazio = new Semaphore(CAPACIDADE);

    // Controla os itens disponíveis para consumo
    private final Semaphore dadosDisponiveis = new Semaphore(0);

    // Adiciona um item ao buffer
    public void put(int value) throws InterruptedException {
        // Espera até existir uma posição livre
        vazio.acquire();

        try {
            data.add(value);
            System.out.println("Inserted: " + value + " | Buffer size: " + data.size());
        } finally {
            // Libera o acesso ao buffer
            mutex.release();
        }

        // Informa que existe mais um item disponível
        dadosDisponiveis.release();
    }
    
    // Remove um item do buffer
    public int remove() throws InterruptedException {
        // Espera até existir um item disponível
        dadosDisponiveis.acquire();
        // Espera até conseguir acesso exclusivo ao buffer
        mutex.acquire();
        
        int value;

        try {
            // Remove o primeiro item inserido (FIFO)
            value = data.remove(0);
            System.out.println("Removed: " + value + " | Buffer size: " + data.size());

        } finally {
            // Libera o acesso ao buffer
            mutex.release();
        }

        // Informa que existe uma posição livre
        vazio.release();
        return value;
    }
}
