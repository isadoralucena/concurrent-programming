class Consumer {
    private final Buffer buffer;
    private final int sleepTime;
    private final int id;
    
    public Consumer(int id, Buffer buffer, int sleepTime) {
        this.id = id;
        this.buffer = buffer;
        this.sleepTime = sleepTime;
    }
    
    public void process() {
        boolean interrupted = false;
        while (true) {
            try {
                int item = buffer.remove();
                if (item == -1) {
                    if (interrupted) Thread.currentThread().interrupt();
                    break;
                }
                System.out.println("Consumer " + id + " consumed item " + item);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // Continua atendendo para não deixar a sentinela sem consumidor.
                interrupted = true;
            }
        }
    }
}