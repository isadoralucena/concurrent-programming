#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>

int do_sum(const char *path) {
    sleep(1); // emulating an adittional delay of 1 second
	      //
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

void *aux(void *arg) {
    char *path = (char *)arg;
    int *sum = malloc(sizeof(int));
    *sum = do_sum(path);
    pthread_exit((void *) sum);
}

int main(int argc, char *argv[]) {
    pthread_t threads[argc];


    for (int i = 1; i < argc; i++) {
        const char *path = argv[i];
        pthread_create(&threads[i], NULL, aux, (void*)path);        
    }
    
    int *retorno = malloc(sizeof(int));

    for (int i = 1; i < argc; i++){
        const char *path = argv[i];
        pthread_join(threads[i], (void **)&retorno);
        printf("%s : %d\n", path, *retorno);
        
    }
    free(retorno);
    return 0;
}
