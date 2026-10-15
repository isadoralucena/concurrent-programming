import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class DnaConcurrentMain {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java DnaConcurrentMain DIRETORIO_ARQUIVOS PADRAO");
            System.err.println("Exemplo: java DnaConcurrentMain dna_inputs CGTAA");
            System.exit(1);
        }

        String dirName = args[0];
        String pattern = args[1];

        File dir = new File(dirName);
        if (!dir.isDirectory()) {
            System.err.println("Caminho não é um diretório: " + dirName);
            System.exit(2);
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.err.println("Nenhum arquivo .txt encontrado em: " + dirName);
            System.exit(3);
        }

        // Uma thread por núcleo disponível, nunca mais threads do que arquivos.
        int nThreads = Math.min(Runtime.getRuntime().availableProcessors(), files.length);

        Worker[] workers = new Worker[nThreads];
        Thread[] threads = new Thread[nThreads];

        // Divide os arquivos em blocos contíguos: cada thread fica com uma fatia.
        int chunk = (int) Math.ceil((double) files.length / nThreads);
        for (int t = 0; t < nThreads; t++) {
            int start = t * chunk;
            int end = Math.min(start + chunk, files.length);
            workers[t] = new Worker(files, start, end, pattern);
            threads[t] = new Thread(workers[t]);
            threads[t].start();
        }

        // A thread principal espera todas as threads filhas terminarem.
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Cada worker guardou seu resultado parcial; aqui somamos com segurança.
        long total = 0;
        for (Worker w : workers) {
            total += w.getCount();
        }

        System.out.println("Sequência " + pattern + " foi encontrada " + total + " vezes.");
    }

    // Classe de trabalho: conta o padrão na fatia de arquivos [start, end).
    static class Worker implements Runnable {
        private final File[] files;
        private final int start;
        private final int end;
        private final String pattern;
        private long count = 0;

        Worker(File[] files, int start, int end, String pattern) {
            this.files = files;
            this.start = start;
            this.end = end;
            this.pattern = pattern;
        }

        @Override
        public void run() {
            long local = 0;
            for (int i = start; i < end; i++) {
                try {
                    local += countInFile(files[i], pattern);
                } catch (IOException e) {
                    System.err.println("Erro ao ler arquivo " + files[i] + ": " + e.getMessage());
                }
            }
            this.count = local;
        }

        public long getCount() {
            return count;
        }
    }

    public static long countInFile(File file, String pattern) throws IOException {
        long total = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    total += countInSequence(line, pattern);
                }
            }
        }
        return total;
    }

    public static long countInSequence(String sequence, String pattern) {
        if (sequence == null || pattern == null) {
            return 0;
        }
        int n = sequence.length();
        int m = pattern.length();
        if (m == 0 || n < m) {
            return 0;
        }
        long count = 0;
        for (int i = 0; i <= n - m; i++) {
            if (sequence.regionMatches(false, i, pattern, 0, m)) {
                count++;
            }
        }
        return count;
    }

}
