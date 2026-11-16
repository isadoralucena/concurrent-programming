import sys
import time
import threading

def do_sum(path):
    time.sleep(1) # emulating adittional delay of 1 second
    _sum = 0
    with open(path, 'rb',buffering=0) as f:
        byte = f.read(1)
        while byte:
            _sum += int.from_bytes(byte, byteorder='big', signed=False)
            byte = f.read(1)
        return _sum

threads = []
def thread_function(i, path, resultado):
    resultado[i] = do_sum(path)


if __name__ == "__main__":
    paths = sys.argv[1:]
    resultado = [0] * len(paths)
    for i in range(len(paths)):
        t = threading.Thread(target=thread_function, args=(i, paths[i], resultado))
        t.start()
        threads.append(t)
    
    for i in range(len(paths)):
        threads[i].join()
        print(paths[i] + " : " + str(resultado[i]))