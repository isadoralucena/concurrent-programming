import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

class Buffer {
    private static final int CAPACIDADE = 50;

    private final List<Integer> data = new ArrayList<>();

    // mutex: garante que so uma thread mexe na lista por vez
    private final Semaphore mutex = new Semaphore(1);
    // vagas: quantas posicoes livres o buffer ainda tem (comeca cheio de vagas)
    private final Semaphore vagas = new Semaphore(CAPACIDADE);
    // itens: quantos itens estao disponiveis para consumo (comeca com zero)
    private final Semaphore itens = new Semaphore(0);

    public void put(int value) throws InterruptedException {
        vagas.acquire();   // se o buffer estiver cheio, o produtor espera aqui
        mutex.acquire();
        data.add(value);
        System.out.println("Inserted: " + value + " | Buffer size: " + data.size());
        mutex.release();
        itens.release();   // avisa que tem mais um item para consumir
    }

    public int remove() throws InterruptedException {
        itens.acquire();   // se o buffer estiver vazio, o consumidor espera aqui
        mutex.acquire();
        int value = data.remove(0);
        System.out.println("Removed: " + value + " | Buffer size: " + data.size());
        mutex.release();
        vagas.release();   // avisa que abriu mais uma vaga no buffer
        return value;
    }
}
