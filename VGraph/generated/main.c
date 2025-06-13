#include <stdio.h>
#include <math.h>
#include <unistd.h>
#include "graphics.h"

// Global variables
int radioG, radioP, distancia;
double pi = 3.14;
int w = 1500;
int h = 800;
int x, y;
double t;
int i, paso;
int px, py;
char* c;

int main() {
    if (init_framebuffer() != 0) {
        printf("Error: No se pudo inicializar el framebuffer\n");
        return 1;
    }

    clear_screen();

// === FRAME START ===
radioG = 375;
radioP = 150;
distancia = 150;
i = 1;
while ((i < 5)) {
    if ((i == 3)) {
        c = "rojo";
    } else if ((i < 3)) {
        c = "azul";
    } else {
        c = "magenta";
    }
    if ((i == 3)) {
        setcolor(c);
        paso = i / 2;
        radioP = radioP + i * paso;
        t = 0.0;
        while ((t < 150)) {
            x = (radioG - radioP) * cos(t) + distancia * cos((radioG - radioP) * t / radioP);
            y = (radioG - radioP) * sin(t) + distancia * sin((radioG - radioP) * t / radioP);
            px = (w / 2) + x;
            py = (h / 2) + y;
            pixel(px, py);
            wait_seconds(0.1);
            t = t + 0.005;
        }
    }
    i = i + 1;
}
// === FRAME END ===

    wait_seconds(3);
    cleanup_framebuffer();
    return 0;
}
