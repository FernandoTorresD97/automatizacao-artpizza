// Toca um alerta de duas notas quando um pedido novo chega, sem precisar
// de nenhum arquivo de áudio — gerado na hora com a Web Audio API.
export function tocarAlertaNovoPedido() {
  try {
    const ContextoAudio = window.AudioContext || window.webkitAudioContext
    const contexto = new ContextoAudio()

    const tocarNota = (frequencia, inicioEm, duracao) => {
      const oscilador = contexto.createOscillator()
      const ganho = contexto.createGain()

      oscilador.type = 'sine'
      oscilador.frequency.value = frequencia
      oscilador.connect(ganho)
      ganho.connect(contexto.destination)

      const t0 = contexto.currentTime + inicioEm
      ganho.gain.setValueAtTime(0, t0)
      ganho.gain.linearRampToValueAtTime(0.2, t0 + 0.02)
      ganho.gain.exponentialRampToValueAtTime(0.001, t0 + duracao)

      oscilador.start(t0)
      oscilador.stop(t0 + duracao)
    }

    tocarNota(880, 0, 0.18)
    tocarNota(1174.66, 0.16, 0.25)
  } catch (erro) {
    console.warn('Não foi possível tocar o alerta sonoro:', erro)
  }
}
