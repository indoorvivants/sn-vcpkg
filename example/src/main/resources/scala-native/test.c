#include <czmq.h>
#include <stdio.h>

char* test_binding() {
  printf("hello!");
  zuuid_t* uuid = zuuid_new();

  return zuuid_str(uuid);
}
