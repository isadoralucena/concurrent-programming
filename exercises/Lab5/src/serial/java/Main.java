public class Main {
    public static void main(String[] args) {
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
        Thread[] producers = new Thread[numProducers];
        Thread[] consumers = new Thread[numConsumers];

        // Cria e inicia os consumidores
        for (int i = 0; i < numConsumers; i++) {
            Consumer consumer = new Consumer(i + 1, buffer, consumingTime);

            consumers[i] = new Thread(() -> consumer.process());
            consumers[i].start();
        }

        // Cria e inicia os produtores
        for (int i = 0; i < numProducers; i++) {
            Producer producer = new Producer(i + 1, buffer, maxItemsPerProducer, producingTime);

            producers[i] = new Thread(() -> producer.produce());
            producers[i].start();
        }

        try {
            // Espera todos os produtores terminarem
            for (Thread producer : producers) {
                producer.join();
            }

            // Envia um sinal de encerramento para cada consumidor
            for (int i = 0; i < numConsumers; i++) {
                buffer.put(-1);
            }

            // Espera todos os consumidores terminarem
            for (Thread consumer : consumers) {
                consumer.join();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
