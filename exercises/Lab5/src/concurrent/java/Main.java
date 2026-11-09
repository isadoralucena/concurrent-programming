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

        for (int i = 0; i < numConsumers; i++) {
            Consumer consumer = new Consumer(i + 1, buffer, consumingTime);
            consumers[i] = new Thread(consumer::process, "consumer-" + (i + 1));
            consumers[i].start();
        }
        for (int i = 0; i < numProducers; i++) {
            Producer producer = new Producer(i + 1, buffer, maxItemsPerProducer, producingTime);
            producers[i] = new Thread(producer::produce, "producer-" + (i + 1));
            producers[i].start();
        }

        boolean interrupted = false;
        for (Thread producer : producers) {
            interrupted |= joinUninterruptibly(producer);
        }

        // Só após o fim de todos os produtores entram as sentinelas FIFO.
        for (int i = 0; i < numConsumers; i++) {
            boolean inserted = false;
            while (!inserted) {
                try {
                    buffer.put(-1);
                    inserted = true;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        }

        for (Thread consumer : consumers) {
            interrupted |= joinUninterruptibly(consumer);
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    private static boolean joinUninterruptibly(Thread thread) {
        boolean interrupted = false;
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        return interrupted;
    }
}
