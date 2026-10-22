/** Reglas de arquitectura limpia del frontend (estilo clean: domain, application, infrastructure, presentation). */
const RX = '^node_modules/rxjs/';
const LAYERS = '^src/app/(domain|application|infrastructure|presentation)/';

/** @type {import('dependency-cruiser').IConfiguration} */
module.exports = {
  forbidden: [
    { name: 'no-circular', severity: 'error', from: {}, to: { circular: true } },
    {
      name: 'domain-puro',
      comment: 'domain no importa nada fuera de domain ni @angular/* (rxjs solo para el tipo Observable de los puertos).',
      severity: 'error',
      from: { path: '^src/app/domain/', pathNot: '\\.spec\\.ts$' },
      to: { pathNot: ['^src/app/domain/', RX] },
    },
    {
      name: 'application-solo-domain',
      comment: 'application: solo domain + @angular/core + rxjs.',
      severity: 'error',
      from: { path: '^src/app/application/', pathNot: '\\.spec\\.ts$' },
      to: { pathNot: ['^src/app/(application|domain)/', '^@angular/core(/|$)', RX] },
    },
    {
      name: 'infrastructure-solo-domain',
      comment: 'infrastructure: domain + @angular/common/http + @angular/core + rxjs; nunca application ni presentation.',
      severity: 'error',
      from: { path: '^src/app/infrastructure/', pathNot: '\\.spec\\.ts$' },
      to: {
        pathNot: [
          '^src/app/(infrastructure|domain)/',
          '^@angular/core(/|$)',
          '^@angular/common(/|$)',
          RX,
        ],
      },
    },
    {
      name: 'presentation-sin-infrastructure',
      comment: 'presentation no importa infrastructure ni HttpClient.',
      severity: 'error',
      from: { path: '^src/app/presentation/', pathNot: '\\.spec\\.ts$' },
      to: { path: ['^src/app/infrastructure/', '^@angular/common/http'] },
    },
    {
      name: 'presentation-solo-application-y-domain',
      severity: 'error',
      from: { path: '^src/app/presentation/', pathNot: '\\.spec\\.ts$' },
      to: { path: '^src/app/', pathNot: '^src/app/(presentation|application|domain)/' },
    },
    {
      name: 'features-aislados',
      comment: 'Un feature de presentation no importa otro feature.',
      severity: 'error',
      from: { path: '^src/app/presentation/([^/]+)/', pathNot: '\\.spec\\.ts$' },
      to: { path: '^src/app/presentation/([^/]+)/', pathNot: '^src/app/presentation/$1/' },
    },
    {
      name: 'capas-solo-composition-root',
      comment: 'Solo app.config.ts, app.routes.ts, app.ts, main.ts y *.spec.ts ven todas las capas.',
      severity: 'error',
      from: { path: LAYERS, pathNot: '\\.spec\\.ts$' },
      to: { path: '^src/app/[^/]+\\.ts$' },
    },
  ],
  options: {
    doNotFollow: { path: 'node_modules' },
    tsPreCompilationDeps: true,
    tsConfig: { fileName: 'tsconfig.json' },
    exclude: { path: '\\.d\\.ts$' },
  },
};
