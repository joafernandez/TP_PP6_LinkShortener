# ADR-0023 – Código QR generado en el servidor

- **Estado:** Aceptada (supuesto del equipo, pendiente de confirmación del Cliente – D8)
- **Fecha:** 2026-10-05
- **Etapa:** 1
- **Origen:** decisión del equipo ante una ambigüedad de la consigna ([minuta 01](../etapa-1/minutas/minuta-01-elucidacion.md), D8)

## Contexto
La consigna indica que *"tanto la página web como el complemento deben generar y mostrar un código QR"* que codifique la URL corta. La redacción literal podría interpretarse como generación en el cliente. **Es el supuesto con más riesgo y debe confirmarse con el Cliente.**

## Decisión
- El QR se genera **en el backend** con la librería **ZXing**, que se agrega en el incremento 5.
- Lo expone un endpoint que devuelve la imagen PNG del QR de un enlace vigente: `GET /api/v1/links/{alias}/qr`. El detalle se define en el contrato OpenAPI.
- La página web y la extensión lo muestran con un `<img>`.

## Alternativas consideradas
- **Generación en cada cliente con una librería JavaScript:** coincide con la lectura literal de la consigna, pero duplica la lógica en la web y en la extensión, y no se puede probar con los tests del backend.

## Consecuencias
- Una sola implementación, probada con JUnit, y clientes más simples.
- Cualquier cliente futuro obtiene el QR sin esfuerzo adicional.
- Si el Cliente exige generarlo en el cliente, el cambio afecta a `app.qr`, que se elimina, y a los dos clientes, que pasan a incluir la librería JS. El resto del sistema no cambia.
