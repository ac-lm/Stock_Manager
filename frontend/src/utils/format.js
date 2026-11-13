export const formatoNumero = (valor, decimales = 2) =>
  Number(valor ?? 0).toLocaleString('es-AR', {
    minimumFractionDigits: decimales,
    maximumFractionDigits: decimales,
  })
