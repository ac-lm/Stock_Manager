import {
  etiquetaConcepto,
  etiquetaMedio,
  formatearFechaHora,
  formatearMonto,
} from '../../utils/caja'

function MovimientosCajaTable({ movimientos }) {
  return (
    <div className="table-wrapper">
      <table className="table">
        <thead>
          <tr>
            <th>Fecha y hora</th>
            <th>Tipo</th>
            <th>Concepto</th>
            <th>Medio</th>
            <th>Monto</th>
            <th>Usuario</th>
            <th>Observaciones</th>
          </tr>
        </thead>
        <tbody>
          {movimientos.length === 0 && (
            <tr>
              <td colSpan="7" className="empty-state">
                Todavía no hay movimientos en esta sesión.
              </td>
            </tr>
          )}
          {movimientos.map((movimiento) => (
            <tr key={movimiento.id_movimiento_caja}>
              <td className="mono">
                {formatearFechaHora(movimiento.fecha, movimiento.hora)}
              </td>
              <td>{movimiento.tipo === 'INGRESO' ? 'Ingreso' : 'Egreso'}</td>
              <td>{etiquetaConcepto(movimiento.concepto)}</td>
              <td>{etiquetaMedio(movimiento.medioPago)}</td>
              <td
                className={
                  movimiento.tipo === 'INGRESO'
                    ? 'mono monto-positivo'
                    : 'mono monto-negativo'
                }
              >
                {movimiento.tipo === 'INGRESO' ? '+' : '−'}
                {formatearMonto(movimiento.monto)}
              </td>
              <td>{movimiento.usuario}</td>
              <td>{movimiento.observaciones ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default MovimientosCajaTable
