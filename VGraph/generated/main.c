#include <stdio.h>
#include <math.h>
#include <unistd.h>
#include "graphics.h"

// Global variables
int centerX = 300;
int centerY = 300;
int radius = 100;
double offset = -0.349;
int x1;
int var_y1;
double angle2;
int x2;
int y2;
double angle3;
int x3;
int y3;
int midX12;
int midY12;
int midX23;
int midY23;
int midX31;
int midY31;
int vectorX;
int vectorY;
int extendedX;
int extendedY;
double heightFactor = 0.866;
int secondTriangleX;
int secondTriangleY;
int secondMidX1;
int secondMidY1;
int secondMidX2;
int secondMidY2;
int secondMidX3;
int secondMidY3;
int vectorX2;
int vectorY2;
int extendedX2;
int extendedY2;
int thirdTriangleX;
int thirdTriangleY;
int thirdMidX1;
int thirdMidY1;
int thirdMidX2;
int thirdMidY2;
int thirdMidX3;
int thirdMidY3;

int main() {
    if (init_framebuffer() != 0) {
        printf("Error: No se pudo inicializar el framebuffer\n");
        return 1;
    }

    clear_screen();

    // Variable initializations
    x1 = centerX + radius * cos(offset);
    var_y1 = centerY + radius * sin(offset);
    angle2 = offset + 2.094;
    x2 = centerX + radius * cos(angle2);
    y2 = centerY + radius * sin(angle2);
    angle3 = offset + 4.189;
    x3 = centerX + radius * cos(angle3);
    y3 = centerY + radius * sin(angle3);
    midX12 = (x1 + x2) / 2;
    midY12 = (var_y1 + y2) / 2;
    midX23 = (x2 + x3) / 2;
    midY23 = (y2 + y3) / 2;
    midX31 = (x3 + x1) / 2;
    midY31 = (y3 + var_y1) / 2;
    vectorX = midX31 - midX23;
    vectorY = midY31 - midY23;
    extendedX = midX23 + 2 * vectorX;
    extendedY = midY23 + 2 * vectorY;
    secondTriangleX = midX23 + (extendedX - midX23) / 2 + (extendedY - midY23) * heightFactor;
    secondTriangleY = midY23 + (extendedY - midY23) / 2 - (extendedX - midX23) * heightFactor;
    secondMidX1 = (midX23 + extendedX) / 2;
    secondMidY1 = (midY23 + extendedY) / 2;
    secondMidX2 = (extendedX + secondTriangleX) / 2;
    secondMidY2 = (extendedY + secondTriangleY) / 2;
    secondMidX3 = (secondTriangleX + midX23) / 2;
    secondMidY3 = (secondTriangleY + midY23) / 2;
    vectorX2 = midX31 - midX12;
    vectorY2 = midY31 - midY12;
    extendedX2 = midX12 + 2 * vectorX2;
    extendedY2 = midY12 + 2 * vectorY2;
    thirdTriangleX = midX12 + (extendedX2 - midX12) / 2 - (extendedY2 - midY12) * heightFactor;
    thirdTriangleY = midY12 + (extendedY2 - midY12) / 2 + (extendedX2 - midX12) * heightFactor;
    thirdMidX1 = (midX12 + extendedX2) / 2;
    thirdMidY1 = (midY12 + extendedY2) / 2;
    thirdMidX2 = (extendedX2 + thirdTriangleX) / 2;
    thirdMidY2 = (extendedY2 + thirdTriangleY) / 2;
    thirdMidX3 = (thirdTriangleX + midX12) / 2;
    thirdMidY3 = (thirdTriangleY + midY12) / 2;

// === FRAME START ===
setcolor("rojo");
line(x1, var_y1, x2, y2);
line(x2, y2, x3, y3);
line(x3, y3, x1, var_y1);
line(x1, var_y1, midX12, midY12);
line(midX12, midY12, midX31, midY31);
line(midX31, midY31, x1, var_y1);
line(x2, y2, midX12, midY12);
line(midX12, midY12, midX23, midY23);
line(midX23, midY23, x2, y2);
line(x3, y3, midX23, midY23);
line(midX23, midY23, midX31, midY31);
line(midX31, midY31, x3, y3);
line(midX23, midY23, extendedX, extendedY);
line(extendedX, extendedY, secondTriangleX, secondTriangleY);
line(secondTriangleX, secondTriangleY, midX23, midY23);
line(midX23, midY23, secondMidX1, secondMidY1);
line(secondMidX1, secondMidY1, secondMidX3, secondMidY3);
line(secondMidX3, secondMidY3, midX23, midY23);
line(extendedX, extendedY, secondMidX1, secondMidY1);
line(secondMidX1, secondMidY1, secondMidX2, secondMidY2);
line(secondMidX2, secondMidY2, extendedX, extendedY);
line(secondTriangleX, secondTriangleY, secondMidX2, secondMidY2);
line(secondMidX2, secondMidY2, secondMidX3, secondMidY3);
line(secondMidX3, secondMidY3, secondTriangleX, secondTriangleY);
line(midX12, midY12, extendedX2, extendedY2);
line(extendedX2, extendedY2, thirdTriangleX, thirdTriangleY);
line(thirdTriangleX, thirdTriangleY, midX12, midY12);
line(midX12, midY12, thirdMidX1, thirdMidY1);
line(thirdMidX1, thirdMidY1, thirdMidX3, thirdMidY3);
line(thirdMidX3, thirdMidY3, midX12, midY12);
line(extendedX2, extendedY2, thirdMidX1, thirdMidY1);
line(thirdMidX1, thirdMidY1, thirdMidX2, thirdMidY2);
line(thirdMidX2, thirdMidY2, extendedX2, extendedY2);
line(thirdTriangleX, thirdTriangleY, thirdMidX2, thirdMidY2);
line(thirdMidX2, thirdMidY2, thirdMidX3, thirdMidY3);
line(thirdMidX3, thirdMidY3, thirdTriangleX, thirdTriangleY);
// === FRAME END ===

    wait_seconds(3);
    cleanup_framebuffer();
    return 0;
}
