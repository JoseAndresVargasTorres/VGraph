#include <stdio.h>
#include <math.h>
#include <unistd.h>

int i;
i = 0;
while ((i <= 10)) {
    printf("%d\n", (i));
    sleep(3);
    i = i + 5;
    printf("%d\n", (3));
    i = i + 1;
}
