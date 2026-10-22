// Falla si algun archivo .ts de src/ usa el tipo `any` (anotacion, genérico o aserción).
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';

const ANY_TYPE = /(:\s*any\b|\bas\s+any\b|<any>|<any[,\]>\s]|\bany\[\])/;
const offenders = [];

const walk = (dir) => {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) walk(path);
    else if (path.endsWith('.ts')) scan(path);
  }
};

const scan = (path) => {
  readFileSync(path, 'utf8')
    .split('\n')
    .forEach((line, i) => {
      const code = line.replace(/\/\/.*$/, '').replace(/'[^']*'|"[^"]*"/g, "''");
      if (ANY_TYPE.test(code)) offenders.push(`${path}:${i + 1}: ${line.trim()}`);
    });
};

walk('src');
if (offenders.length > 0) {
  console.error('Uso de `any` prohibido:\n' + offenders.join('\n'));
  process.exit(1);
}
console.log('Sin `any` en src/');
