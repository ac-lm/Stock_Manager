import {
  claseDiferencia,
  diferenciaConSigno,
  formatearMonto,
} from '../../utils/caja'

function TarjetaTotal({ titulo, esperado, declarado, diferencia }) {
  return (
    <div className="caja-total">
      <span className="caja-total-titulo">{titulo}</span>
      <strong className="caja-total-monto">{formatearMonto(esperado)}</strong>
      <span className="caja-total-etiqueta">Esperado</span>
      {declarado != null && (
        <div className="caja-total-cierre">
          <span>Declarado: {formatearMonto(declarado)}</span>
          <span className={claseDiferencia(diferencia)}>
            Diferencia: {diferenciaConSigno(diferencia)}
          </span>
        </div>
      )}
    </div>
  )
}

function TotalesCaja({ totales }) {
  if (!totales) return null

  return (
    <div className="caja-resumen">
      <TarjetaTotal
        titulo="Efectivo"
        esperado={totales.esperadoEfectivo}
        declarado={totales.declaradoEfectivo}
        diferencia={totales.diferenciaEfectivo}
      />
      <TarjetaTotal
        titulo="Transferencia (neto)"
        esperado={totales.esperadoTransferencia}
        declarado={totales.declaradoTransferencia}
        diferencia={totales.diferenciaTransferencia}
      />
      <TarjetaTotal
        titulo="Total"
        esperado={totales.totalEsperado}
        declarado={totales.totalDeclarado}
        diferencia={totales.diferencia}
      />
    </div>
  )
}

export default TotalesCaja
