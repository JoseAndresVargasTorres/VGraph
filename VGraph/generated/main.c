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

int x = 200;
int y = 150;
int radio = 75;
setcolor("azul");
circle(x, y, radio);

    wait_seconds(3);
    cleanup_framebuffer();
    return 0;
}
