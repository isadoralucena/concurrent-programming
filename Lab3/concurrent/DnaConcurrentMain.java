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
        int numThreads = files.length;
        WorkFile[] workFiles = new WorkFile[numThreads];
        Thread[] threadsArquivos = new Thread [numThreads];

        for (int i = 0; i < numThreads; i++){
            workFiles[i] = new WorkFile(files[i], pattern);
            threadsArquivos[i] = new Thread(workFiles[i]);
            threadsArquivos[i].start();
        }

        try {
            for(Thread t : threadsArquivos){
                t.join();
            }
            
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }

        long total = 0;
        for(WorkFile w : workFiles){
            total += w.count;
        }

        System.out.println("Sequência " + pattern + " foi encontrada " + total + " vezes.");
    }

    static class WorkFile implements Runnable {
        File file;
        String pattern;
        long count = 0;

        WorkFile (File file, String pattern){
            this.file = file;
            this.pattern = pattern;
        }
        @Override
        public void run() {
            try {
                count += countInFile(this.file, this.pattern);
            } catch (IOException e) {
                System.err.println("Erro de IO");
            }
        }
    }

    public static long countInFile(File file, String pattern) throws IOException {
        long total = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // simulando delay de processamento
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

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
