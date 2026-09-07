# Respuestas del Laboratorio — DeepBlue Rescue

## Paso 4 — Analizar las relaciones

### 1. ¿Dónde debería estar la FK entre `RescueCenter` y `RescueCase`?
En la tabla `rescue_cases` (el lado N de la relación). Una FK `rescue_center_id` que referencia a `rescue_centers.id`.

### 2. ¿Dónde debería estar la FK entre `Animal` y `MedicalRecord`?
En la tabla `medical_records` (el lado dependiente). Una FK `animal_id` que referencia a `animals.id`.

### 3. ¿Qué constraint necesitamos para convertir esa FK en una verdadera relación 1:1?
UNIQUE sobre la columna FK (`animal_id`). Esto garantiza que un mismo animal no pueda tener más de un expediente médico.

### 4. ¿Por qué `Specialist` y `Expertise` necesitan una tabla intermedia?
Porque PostgreSQL (y el modelo relacional en general) no almacena listas directamente. Una tabla intermedia `specialist_expertise` con FKs a ambas tablas permite representar la relación N:M: un especialista puede tener múltiples áreas de experiencia, y una misma área puede pertenecer a múltiples especialistas.

### 5. ¿Dónde deberían estar las FK de `Treatment`?
En la propia tabla `treatments`, con columnas `animal_id` y `specialist_id`.

### 6. ¿Puede un tratamiento existir sin Animal?
No. La FK `animal_id` tiene `NOT NULL`, por lo que todo tratamiento debe estar asociado a un animal.

### 7. ¿Puede existir sin Specialist?
No. La FK `specialist_id` también tiene `NOT NULL`, requiriendo siempre un especialista asociado.

---

## Paso 7 — Configurar application.yml

### ¿Por qué utilizamos `ddl-auto: validate` en lugar de `ddl-auto: update`?
Porque queremos que **Flyway sea el único responsable** de crear y evolucionar el esquema. `validate` simplemente verifica que las entidades JPA coincidan con las tablas existentes; `update` modificaría el esquema automáticamente según las entidades, lo cual:
- Mezcla responsabilidades (Flyway pierde control).
- No genera scripts versionados ni reproducible.
- Puede hacer cambios destructivos sin control.

Con `validate`, si una migración está mal o falta, el arranque falla — lo que es deseado.

---

## Paso 10 — Crear animals

### ¿Por qué también debe ser UNIQUE?
Porque queremos garantizar `RescueCase 1:1 Animal`. Si `rescue_case_id` fuera solo FK (sin UNIQUE), varios animales podrían apuntar al mismo caso, convirtiéndolo en 1:N. Con UNIQUE, cada caso puede tener **a lo sumo** un animal.

---

## Paso 14 — Crear specialist_expertise

### ¿Qué problema evita la PK compuesta?
Evita filas duplicadas. Sin ella, se podría insertar `(1, 2)` múltiples veces, asociando erróneamente la misma expertise al mismo especialista más de una vez.

---

## Paso 16 — Crear índices

### ¿Por qué no necesitamos crear manualmente otro índice para las PK?
Porque PostgreSQL (y la mayoría de los motores) crea automáticamente un índice único para cada PRIMARY KEY al definirla. Sería redundante.

---

## Paso 23 — Implementar Animal 1:1 MedicalRecord

### ¿Por qué es conveniente que el método actualice ambos lados?
Porque JPA/Hibernate trabaja en memoria. Si solo actualizamos un lado (ej. `medicalRecord.setAnimal(this)` pero no `this.medicalRecord = medicalRecord`), la referencia en memoria queda inconsistente hasta que se recargue de la BD. Mantener ambos lados sincronizados garantiza coherencia inmediata.

---

## CHECKPOINT 2 — Relaciones 1:1

### RescueCase ↔ Animal — ¿Quién tiene `@JoinColumn`?
**Animal** es quien tiene `@JoinColumn(name = "rescue_case_id")` porque es el lado que físicamente mantiene la FK en la tabla `animals`.

### Animal ↔ MedicalRecord — ¿Quién tiene `@JoinColumn`?
**MedicalRecord** es quien tiene `@JoinColumn(name = "animal_id")` porque es el lado que físicamente mantiene la FK en la tabla `medical_records`.

---

## Paso 40 — Comparar JPQL y SQL

### ¿Por qué en JPQL escribimos `Specialist` y no `specialists`?
Porque JPQL trabaja con **entidades Java** (nombres de clase), no con tablas físicas. `Specialist` es el nombre de la entidad. Hibernate traduce automáticamente al nombre de tabla real (`specialists`) según el mapeo `@Entity`/`@Table`.

---

## Paso 60 — Probar FK

### ¿JPA permite llegar fácilmente a esta situación?
No fácilmente. Con `nullable = false` en `@JoinColumn`, JPA no permite establecer `null`. Sin embargo, si se omite la asignación y se intenta persistir, la excepción vendrá del constraint de PostgreSQL (`NOT NULL`), demostrando que la validación real está en la BD.

### ¿Qué papel cumple `nullable = false`?
Indica a JPA/Hibernate que la columna no admite nulos. Aunque Hibernate podría intentar insertar igualmente, la restricción en BD rechazará la operación.

### ¿Qué papel cumple realmente el constraint PostgreSQL?
Es la **protección definitiva**. Aunque la aplicación tenga bugs o intente evadir las validaciones de JPA, PostgreSQL rechazará cualquier operación que viole la integridad referencial. Es la última línea de defensa.

---

## Paso 61 — Probar CHECK

### Si Java usa un enum, ¿por qué sigue siendo útil mantener el CHECK en PostgreSQL?
Porque:
1. **Otras aplicaciones** (scripts, migraciones, otros lenguajes) pueden escribir directamente en BD sin pasar por Java.
2. **El enum Java** solo protege a nivel de aplicación; si alguien modifica la BD directamente, el CHECK lo impide.
3. **Defensa en profundidad**: nunca depender de una sola capa de validación.

---

## Parte XIV — Verificación Final

### 1. ¿Qué diferencia existe entre JPA, Hibernate, Spring Data JPA y PostgreSQL?

| Tecnología | Rol |
|-----------|-----|
| **JPA** | Especificación (contrato/interfaz) que define cómo mapear objetos a tablas |
| **Hibernate** | Implementación concreta de JPA; ejecuta las operaciones ORM |
| **Spring Data JPA** | Capa de abstracción sobre JPA que reduce boilerplate (repositorios automáticos) |
| **PostgreSQL** | Motor de base de datos relacional donde se almacenan los datos físicamente |

### 2. ¿Qué componente crea las tablas?
**Flyway**, ejecutando los scripts SQL de migración.

### 3. ¿Qué componente ejecuta las migraciones?
**Flyway** (integrado vía Spring Boot Starter Flyway).

### 4. ¿Qué hace `ddl-auto=validate`?
Verifica que las entidades JPA coincidan con el esquema existente en BD. Si no coinciden, la aplicación falla al arrancar. **No modifica el esquema**.

### 5. ¿Qué significa `mappedBy`?
Indica que **esta parte de la relación no es la propietaria**. El otro lado (el que tiene `@JoinColumn`) es quien controla físicamente la FK. `mappedBy` es solo una referencia en memoria para navegabilidad bidireccional.

### 6. ¿Cómo identificas al propietario de una relación?
El propietario es el lado que:
- Tiene `@JoinColumn`
- Mantiene físicamente la FK en la tabla
- En relaciones 1:1, generalmente el lado "débil" (el que sin el otro no tiene sentido)

### 7. ¿Dónde está físicamente la FK de `RescueCenter 1:N RescueCase`?
En la tabla `rescue_cases`, columna `rescue_center_id`.

### 8. ¿Qué permite que `Animal 1:1 MedicalRecord` sea realmente 1:1 en PostgreSQL?
El constraint `UNIQUE` sobre la columna FK `animal_id` en `medical_records`. Sin él, múltiples registros médicos podrían apuntar al mismo animal.

### 9. ¿Por qué `Specialist N:M Expertise` requiere una tabla intermedia?
Porque el modelo relacional no soporta listas/arrays nativos. La tabla `specialist_expertise` con FKs a ambas tablas descompone la relación N:M en dos relaciones 1:N manejables.

### 10. ¿Qué diferencia existe entre `findById()` y `findByCaseCode()`?
- `findById()`: método heredado de `JpaRepository`, busca por la PK.
- `findByCaseCode()`: Query Method personalizado que busca usando un campo único no-PK (`case_code`).

### 11. ¿Qué es un Query Method?
Un método declarado en el repositorio cuyo nombre sigue una convención (`findBy + Campo + Condición`). Spring Data JPA genera automáticamente la consulta SQL/JPQL sin necesidad de escribir `@Query` manualmente.

### 12. ¿Qué significa navegar asociaciones mediante `findByRescueCaseRescueCenterCode(...)`?
Que el método navega desde `Animal` → `rescueCase` (relación) → `rescueCenter` (relación anidada) → `code` (campo final). Spring interpreta los puntos como navegación de propiedades JPA.

### 13. ¿Qué es `@Query`?
Una anotación que permite escribir manualmente una consulta JPQL (o SQL nativo) para casos donde un Query Method no es suficiente o sería demasiado complejo.

### 14. ¿Qué es JPQL?
**Java Persistence Query Language**: un lenguaje de consultas orientado a objetos, similar a SQL pero opera sobre entidades Java (nombres de clase/campo) en lugar de tablas/columnas físicas.

### 15. ¿Por qué JPQL utiliza `Specialist` en vez de `specialists`?
Porque JPQL trabaja con **nombres de entidad Java**, no con nombres de tabla. Hibernate traduce internamente a `specialists` según el mapeo.

### 16. ¿Qué diferencia existe entre `save()` y `saveAndFlush()`?
- `save()`: registra la entidad en el contexto de persistencia (pendiente de flush).
- `saveAndFlush()`: además de registrarea, **inmediatamente sincroniza** con la BD (ejecuta el INSERT/UPDATE). Útil cuando necesitas el ID generado o quieres forzar constraints inmediatamente.

### 17. ¿Por qué estamos probando constraints con PostgreSQL y no únicamente mediante Java?
Porque:
- Java solo valida a nivel de aplicación.
- PostgreSQL protege la integridad de los datos incluso si otra aplicación o script accede directamente.
- Los tests con Testcontainers demuestran que los constraints reales funcionan en producción.

### 18. ¿Por qué Testcontainers es útil?
Porque permite ejecutar tests de integración contra **una base de datos real** (PostgreSQL) en lugar de una en memoria (H2) que podría comportarse diferente. Garantiza que el código funciona contra el motor real de producción.

---

## Paso 67 — Clasificar cada consulta

| Necesidad | Mecanismo |
|-----------|-----------|
| Buscar una entidad por ID | Método heredado (`findById`) |
| Buscar caso por código | Query Method (`findByCaseCode`) |
| Casos según status | Query Method (`findByStatusOrderByRescueDateAsc`) |
| Animales de determinado centro | Query Method (`findByRescueCaseRescueCenterCode`) |
| Especialistas según expertise | @Query + JPQL |
| Tratamientos en intervalo | @Query + JPQL |
| Tratamientos por expertise del especialista | @Query + JPQL |

---

## Paso 75-76 — Reto sin guía

### ¿Query Method o @Query + JPQL?
**@Query + JPQL** es la alternativa apropiada porque:
- La consulta navega **múltiples relaciones** (Animal → RescueCase → status, Animal → Treatment → Specialist → Expertise).
- Requiere condiciones en campos de entidades relacionadas.
- Necesita `DISTINCT` para evitar duplicados.
- Un Query Method sería extremadamente largo y difícil de leer.

La consulta implementada:
```java
@Query("""
    select distinct a
    from Animal a
    join a.rescueCase rc
    join a.treatments t
    join t.specialist s
    join s.expertiseAreas e
    where rc.status = :status
      and lower(e.name) = lower(:expertiseName)
""")
List<Animal> findInRehabilitationWithTreatmentsByExpertise(RescueStatus status, String expertiseName);
```
