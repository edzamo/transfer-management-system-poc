export class ErrorDeRed extends Error {
  constructor() {
    super('No se pudo conectar con el servidor');
    this.name = 'ErrorDeRed';
  }
}
