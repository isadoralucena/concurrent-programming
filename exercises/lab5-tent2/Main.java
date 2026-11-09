public class Main {
    public static void main(String[] args) throws InterruptedException {
        if (args.length != 5) {
            System.out.println("Use: java Main <num_producers> <max_items_per_producer> <producing_time> <num_consumers> <consuming_time>");
            return;
        }

        int numProducers = Integer.parseInt(args[0]);
        int maxItemsPerProducer = Integer.parseInt(args[1]);
        int producingTime = Integer.parseInt(args[2]);
        int numConsumers = Integer.parseInt(args[3]);
        int consumingTime = Integer.parseInt(args[4]);

        Buffer buffer = new Buffer();

        Producer[] producers = new Producer[numProducers];
        Consumer[] consumers = new Consumer[numConsumers];

        // inicia todos os produtores e consumidores ao mesmo tempo
        for (int i = 0; i < numProducers; i++) {
            producers[i] = new Producer(i + 1, buffer, maxItemsPerProducer, producingTime);
            producers[i].start();
        }

        for (int i = 0; i < numConsumers; i++) {
            consumers[i] = new Consumer(i + 1, buffer, consumingTime);
            consumers[i].start();
        }

        // espera todos os produtores terminarem
        for (int i = 0; i < numProducers; i++) {
            producers[i].join();
        }

        // coloca um -1 para cada consumidor, avisando que nao vai chegar mais nada.
        // como a lista e uma fila, os -1 so sao lidos depois dos itens reais.
        for (int i = 0; i < numConsumers; i++) {
            buffer.put(-1);
        }

        // espera todos os consumidores terminarem
        for (int i = 0; i < numConsumers; i++) {
            consumers[i].join();
        }

        System.out.println("Fim da execucao.");
    }
}
