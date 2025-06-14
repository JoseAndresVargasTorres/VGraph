#ifndef GRAPHICS_H
#define GRAPHICS_H

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

// Estructura para colores RGB
typedef struct {
    uint8_t r, g, b;
} vColor;

// Estructura global para la información del framebuffer
struct framebuffer_info {
    int fb;
    uint8_t *fbp;
    struct fb_var_screeninfo vinfo;
    struct fb_fix_screeninfo finfo;
    long screensize;
};

// Variables globales (declaradas como extern en el header)
extern struct framebuffer_info fb_info;
extern uint32_t current_color;

// Funciones de inicialización y limpieza
int init_framebuffer(void);
void cleanup_framebuffer(void);

// Funciones de manejo de colores
vColor get_color_by_name(const char* color_name);
uint32_t create_color_rgb(uint8_t r, uint8_t g, uint8_t b);
void setcolor(const char* color_name);
void setcolor_vcolor(vColor color);

// Funciones de dibujo principales (compatibles con tu gramática ANTLR4)
void pixel(int x, int y);
void line(int x0, int y0, int x1, int y1);
void rect(int x, int y, int width, int height);
void circle(int cx, int cy, int radius);

// Funciones de utilidad
void clear_screen(void);
void wait_seconds(int seconds);
void wait_ms(int milliseconds);

// Funciones de salida
void println_int(int value);
void println_string(const char* str);

#endif // GRAPHICS_H
