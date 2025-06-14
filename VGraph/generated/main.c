#include <stdio.h>
#include <math.h>
#include <unistd.h>
#include "graphics.h"

// Global variables
int t = 100;

int main() {
    if (init_framebuffer() != 0) {
        printf("Error: No se pudo inicializar el framebuffer\n");
        return 1;
    }

    clear_screen();


    wait_seconds(3);
    cleanup_framebuffer();
    return 0;
}
