import os
import sys
import threading

def do_sum(path):
    _sum = 0
    with open(path, 'rb',buffering=0) as f:
        byte = f.read(1)
        while byte:
            _sum += int.from_bytes(byte, byteorder='big', signed=False)
            byte = f.read(1)
        return _sum

# funcao executada por cada thread: guarda o sum na posicao i da lista
def worker(i, path, results):
    results[i] = do_sum(path)

if __name__ == "__main__":
    paths = sys.argv[1:]
    results = [0] * len(paths)
    threads = []

    # cria e inicia uma thread para cada arquivo
    for i in range(len(paths)):
        t = threading.Thread(target=worker, args=(i, paths[i], results))
        t.start()
        threads.append(t)

    # espera cada thread terminar e imprime o resultado
    for i in range(len(paths)):
        threads[i].join()
        print(paths[i] + " : " + str(results[i]))
