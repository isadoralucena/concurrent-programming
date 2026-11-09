class Consumer extends Thread {
    private final Buffer buffer;
    private final int sleepTime;
    private final int id;

    public Consumer(int id, Buffer buffer, int sleepTime) {
        this.id = id;
        this.buffer = buffer;
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {
        process();
    }

    public void process() {
        while (true) {
            try {
                int item = buffer.remove();
                // -1 e o sinal de parada colocado pelo Main quando os produtores terminam
                if (item == -1) break;
                System.out.println("Consumer " + id + " consumed item " + item);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
