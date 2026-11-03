#!/bin/sh
cd "$(dirname "$0")"
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp cooperativa.jar coop.Pruebas
