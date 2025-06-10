#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <linux/fb.h>
#include <math.h>
#include <string.h>
#include "graphics.h"

// Definición de las variables globales
struct framebuffer_info fb_info;
uint32_t current_color = 0;

// Definición de colores según tu gramática
vColor get_color_by_name(const char* color_name) {
    vColor color = {0, 0, 0}; // Default: negro
    
    if (strcmp(color_name, "negro") == 0) {
        color = (vColor){0, 0, 0};
    } else if (strcmp(color_name, "blanco") == 0) {
        color = (vColor){255, 255, 255};
    } else if (strcmp(color_name, "rojo") == 0) {
        color = (vColor){255, 0, 0};
    } else if (strcmp(color_name, "verde") == 0) {
        color = (vColor){0, 255, 0};
    } else if (strcmp(color_name, "azul") == 0) {
        color = (vColor){0, 0, 255};
    } else if (strcmp(color_name, "amarillo") == 0) {
        color = (vColor){255, 255, 0};
    } else if (strcmp(color_name, "cyan") == 0) {
        color = (vColor){0, 255, 255};
    } else if (strcmp(color_name, "magenta") == 0) {
        color = (vColor){255, 0, 255};
    } else if (strcmp(color_name, "marron") == 0) {
        color = (vColor){139, 69, 19};
    }
    
    return color;
}

// Función para inicializar el framebuffer
int init_framebuffer() {
    fb_info.fb = open("/dev/fb0", O_RDWR);
    if (fb_info.fb == -1) {
        perror("Error opening framebuffer device");
        return -1;
    }

    if (ioctl(fb_info.fb, FBIOGET_FSCREENINFO, &fb_info.finfo) == -1) {
        perror("Error reading fixed info");
        close(fb_info.fb);
        return -1;
    }

    if (ioctl(fb_info.fb, FBIOGET_VSCREENINFO, &fb_info.vinfo) == -1) {
        perror("Error reading variable info");
        close(fb_info.fb);
        return -1;
    }

    printf("Resolution: %dx%d, %d bpp\n", fb_info.vinfo.xres, fb_info.vinfo.yres, fb_info.vinfo.bits_per_pixel);

    fb_info.screensize = fb_info.vinfo.yres_virtual * fb_info.finfo.line_length;
    fb_info.fbp = (uint8_t *)mmap(0, fb_info.screensize, PROT_READ | PROT_WRITE, MAP_SHARED, fb_info.fb, 0);
    
    if (fb_info.fbp == MAP_FAILED) {
        perror("Error mapping frame buffer");
        close(fb_info.fb);
        return -1;
    }

    // Inicializar con color negro
    current_color = 0;
    return 0;
}

// Función para limpiar y cerrar el framebuffer
void cleanup_framebuffer() {
    munmap(fb_info.fbp, fb_info.screensize);
    close(fb_info.fb);
}

// Función para crear un color RGB (compatible con vColor)
uint32_t create_color_rgb(uint8_t r, uint8_t g, uint8_t b) {
    return (r << fb_info.vinfo.red.offset) | 
           (g << fb_info.vinfo.green.offset) | 
           (b << fb_info.vinfo.blue.offset);
}

// Función setcolor compatible con tu gramática
void setcolor(const char* color_name) {
    vColor color = get_color_by_name(color_name);
    current_color = create_color_rgb(color.r, color.g, color.b);
}

// Función setcolor con vColor
void setcolor_vcolor(vColor color) {
    current_color = create_color_rgb(color.r, color.g, color.b);
}

// Función para dibujar un pixel (compatible con DrawPixel)
void pixel(int x, int y) {
    // Verificar límites
    if (x < 0 || x >= fb_info.vinfo.xres || y < 0 || y >= fb_info.vinfo.yres) {
        return;
    }

    // Calcular la ubicación del pixel
    long location = (x + fb_info.vinfo.xoffset) * (fb_info.vinfo.bits_per_pixel / 8) + 
                   (y + fb_info.vinfo.yoffset) * fb_info.finfo.line_length;

    // Escribir el pixel con el color actual
    *((uint32_t*)(fb_info.fbp + location)) = current_color;
}

// Función para dibujar una línea (compatible con DrawLine)
void line(int x0, int y0, int x1, int y1) {
    int dx = abs(x1 - x0);
    int dy = abs(y1 - y0);
    int sx = (x0 < x1) ? 1 : -1;
    int sy = (y0 < y1) ? 1 : -1;
    int err = dx - dy;
    int e2;

    while (1) {
        pixel(x0, y0);

        if (x0 == x1 && y0 == y1) break;

        e2 = 2 * err;
        if (e2 > -dy) {
            err -= dy;
            x0 += sx;
        }
        if (e2 < dx) {
            err += dx;
            y0 += sy;
        }
    }
}

// Función para dibujar un rectángulo (compatible con DrawRect)
void rect(int x, int y, int width, int height) {
    // Rectángulo relleno por defecto (según tu gramática parece ser así)
    for (int i = y; i < y + height && i < fb_info.vinfo.yres; i++) {
        for (int j = x; j < x + width && j < fb_info.vinfo.xres; j++) {
            pixel(j, i);
        }
    }
}

// Función para dibujar un círculo (compatible con DrawCircle)
void circle(int cx, int cy, int radius) {
    int x = 0;
    int y = radius;
    int d = 1 - radius;

    // Círculo relleno por defecto
    while (x <= y) {
        // Dibujar líneas horizontales para rellenar el círculo
        for (int i = cx - x; i <= cx + x; i++) {
            pixel(i, cy + y);
            pixel(i, cy - y);
        }
        for (int i = cx - y; i <= cx + y; i++) {
            pixel(i, cy + x);
            pixel(i, cy - x);
        }

        if (d < 0) {
            d += 2 * x + 3;
        } else {
            d += 2 * (x - y) + 5;
            y--;
        }
        x++;
    }
}

// Función clear compatible con ClearComm
void clear_screen() {
    uint32_t black = create_color_rgb(0, 0, 0);
    for (int y = 0; y < fb_info.vinfo.yres; y++) {
        for (int x = 0; x < fb_info.vinfo.xres; x++) {
            long location = (x + fb_info.vinfo.xoffset) * (fb_info.vinfo.bits_per_pixel / 8) + 
                           (y + fb_info.vinfo.yoffset) * fb_info.finfo.line_length;
            *((uint32_t*)(fb_info.fbp + location)) = black;
        }
    }
}

// Función wait compatible con WaitComm
void wait_seconds(int seconds) {
    sleep(seconds);
}

// Función wait en milisegundos
void wait_ms(int milliseconds) {
    usleep(milliseconds * 1000);
}

// Función println compatible con tu gramática
void println_int(int value) {
    printf("%d\n", value);
}

void println_string(const char* str) {
    printf("%s\n", str);
}


