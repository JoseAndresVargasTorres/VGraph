#include <stdio.h>
#include <math.h>
#include <unistd.h>
#include "graphics.h"

int main() {
    if (init_framebuffer() != 0) {
        printf("Error: No se pudo inicializar el framebuffer\n");
        return 1;
    }

    clear_screen();

int x, y, t;
char* c;
// === FRAME START ===
t = 0;
while (t < 360) {
    x = 320 + t * cos(t * 3.1416 / 180);
    y = 240 + t * sin(t * 3.1416 / 180);
    if ((t % 3 == 0)) {
        c = "rojo";
    } else if ((t % 3 == 1)) {
        c = "azul";
    } else {
        c = "verde";
    }
    setcolor(c);
    pixel(x, y);
    wait_seconds(1);
    t = t + 5;
}
// === FRAME END ===

    wait_seconds(3);
    cleanup_framebuffer();
    return 0;
}
