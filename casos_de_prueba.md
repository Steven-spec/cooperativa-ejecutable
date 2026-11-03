| N░ | Caso de prueba | Resultado esperado | Resultado obtenido | Estado |
|----|----------------|--------------------|--------------------|--------|
|  1 | Dep¾sito de $0 | RECHAZADO | RECHAZADO: El monto debe ser mayor que cero | OK |
|  2 | Dep¾sito de monto negativo (-$10) | RECHAZADO | RECHAZADO: El monto debe ser mayor que cero | OK |
|  3 | Dep¾sito vßlido de $50 en AH-0001 ($100) | ACEPTADO saldo=150.00 | ACEPTADO saldo=150.00 | OK |
|  4 | Retiro de ahorros que deja saldo $4 (< mÝnimo $5) | RECHAZADO | RECHAZADO: La cuenta de ahorros debe mantener un saldo mÝnimo de $5.00 (o retirarse por completo para cerrarla) | OK |
|  5 | Retiro de ahorros que deja exactamente $5 | ACEPTADO saldo=5.00 | ACEPTADO saldo=5.00 | OK |
|  6 | Retiro mayor al saldo en ahorros ($150 de $100) | RECHAZADO | RECHAZADO: Saldo insuficiente (saldo $100.00) | OK |
|  7 | Retiro total en ahorros (queda $0, para poder cerrar) | ACEPTADO saldo=0.00 | ACEPTADO saldo=0.00 | OK |
|  8 | Corriente: retiro hasta sobregiro de -$200 | ACEPTADO saldo=-200.00 | ACEPTADO saldo=-200.00 | OK |
|  9 | Corriente: retiro que lleva a -$200.01 | RECHAZADO | RECHAZADO: Excede el sobregiro mßximo de $200.00 | OK |
| 10 | Transferencia a la misma cuenta | RECHAZADO | RECHAZADO: No se puede transferir a la misma cuenta | OK |
| 11 | Transferencia vßlida de $40 AH-0001 ? AH-0002 | ACEPTADO origen=60.00 destino=340.00 | ACEPTADO origen=60.00 destino=340.00 | OK |
| 12 | Transferencia que falla en origen (deja $2): ninguna cuenta cambia | RECHAZADO, saldos sin cambios | RECHAZADO, saldos sin cambios (Retiro en origen rechazado: La cuenta de ahorros debe mantener un saldo mÝnimo de $5.00 (o retirarse por completo para cerrarla)) | OK |
| 13 | Transferencia a cuenta destino inactiva: ninguna cuenta cambia | RECHAZADO, saldos sin cambios | RECHAZADO, saldos sin cambios (La cuenta AH-0002 estß inactiva) | OK |
| 14 | Cerrar cuenta con saldo distinto de cero | RECHAZADO | RECHAZADO: No se puede cerrar: el saldo es $100.00 (debe ser $0.00) | OK |
| 15 | Cerrar cuenta con saldo cero | ACEPTADO activa=false | ACEPTADO activa=false | OK |
| 16 | Eliminar cuenta CON movimientos: se marca inactiva, no se elimina | INACTIVADA existe=true activa=false | INACTIVADA existe=true activa=false | OK |
| 17 | Eliminar cuenta SIN movimientos | ELIMINADA existe=false | ELIMINADA existe=false | OK |
| 18 | Dep¾sito en cuenta inactiva | RECHAZADO | RECHAZADO: La cuenta AH-0001 estß inactiva | OK |
| 19 | Apertura de ahorros con $4 (< $5) | RECHAZADO | RECHAZADO: Una cuenta de ahorros requiere un dep¾sito inicial de al menos $5.00 | OK |
| 20 | Apertura de cuenta para socio inexistente | RECHAZADO | RECHAZADO: El socio 9999999999 no existe | OK |
| 21 | Registro de socio con cÚdula duplicada | RECHAZADO | RECHAZADO: Ya existe un socio con la cÚdula 1000000001 | OK |
| 22 | InterÚs mensual sep-2026 (4 % anual / 12) | ACEPTADO AH-0001=0.33 AH-0002=1.00 | ACEPTADO AH-0001=0.33 AH-0002=1.00 | OK |
| 23 | Los intereses no se acreditan dos veces en el mismo mes | ACEPTADO segunda aplicaci¾n=0 movimientos | ACEPTADO segunda aplicaci¾n=0 movimientos | OK |
| 24 | Cuenta corriente no genera intereses | ACEPTADO movimientos INTERES en corriente=0 | ACEPTADO movimientos INTERES en corriente=0 | OK |
| 25 | AuditorÝa con datos consistentes | ACEPTADO 0 inconsistencias | ACEPTADO 0 inconsistencias | OK |
| 26 | AuditorÝa detecta un saldo alterado manualmente | ACEPTADO 1 inconsistencias | ACEPTADO 1 inconsistencias | OK |
| 27 | Persistencia: guardar en CSV y volver a cargar | ACEPTADO saldos iguales, 0 inconsistencias | ACEPTADO saldos iguales, 0 inconsistencias | OK |

Resumen: 27 de 27 casos aprobados.
