import AppShield from './NativeAppShield';

export function multiply(a: number, b: number): number {
  return AppShield.multiply(a, b);
}
