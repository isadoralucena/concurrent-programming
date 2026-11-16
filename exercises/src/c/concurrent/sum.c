#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

int do_sum(const char *path) {
    FILE *file = fopen(path, "rb");  // open in binary mode
    if (file == NULL) {
        return -1;  // indicate error
    }

    int sum = 0;
    int byte;

    while ((byte = fgetc(file)) != EOF) {
        sum += byte;
    }

    fclose(file);
    return sum;
}

// dados de cada thread: o arquivo que ela processa e o resultado
typedef struct {
    const char *path;
    int sum;
} thread_args;

// funcao executada por cada thread
void *thread_sum(void *arg) {
    thread_args *args = (thread_args *) arg;
    args->sum = do_sum(args->path);
    return NULL;
}

int main(int argc, char *argv[]) {
    int n = argc - 1;

    pthread_t *threads = malloc(n * sizeof(pthread_t));
    thread_args *args = malloc(n * sizeof(thread_args));

    // cria uma thread para cada arquivo
    for (int i = 0; i < n; i++) {
        args[i].path = argv[i + 1];
        pthread_create(&threads[i], NULL, thread_sum, &args[i]);
    }

    // espera cada thread terminar e imprime o resultado
    for (int i = 0; i < n; i++) {
        pthread_join(threads[i], NULL);

        if (args[i].sum >= 0) {
            printf("%s : %d\n", args[i].path, args[i].sum);
        }
    }

    free(threads);
    free(args);
    return 0;
}
