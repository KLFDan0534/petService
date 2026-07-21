import * as THREE from "three";

export function clamp(value, min = 0, max = 1) {
  return Math.min(max, Math.max(min, value));
}

export function easeOutCubic(t) {
  const n = clamp(t);
  return 1 - Math.pow(1 - n, 3);
}

export function smoothstep(edge0, edge1, value) {
  const x = clamp((value - edge0) / (edge1 - edge0));
  return x * x * (3 - 2 * x);
}

export function toVector3(value) {
  if (value instanceof THREE.Vector3) return value.clone();
  return new THREE.Vector3(value[0], value[1], value[2]);
}

export function mixVectorArrays(a, b, t) {
  return toVector3(a).lerp(toVector3(b), clamp(t));
}

export function findTimelineSegment(stops, progress) {
  const value = clamp(progress);
  for (let i = 0; i < stops.length - 1; i += 1) {
    const from = stops[i];
    const to = stops[i + 1];
    if (value >= from.progress && value <= to.progress) {
      return {
        from,
        to,
        t: smoothstep(from.progress, to.progress, value)
      };
    }
  }

  const last = stops[stops.length - 1];
  return { from: last, to: last, t: 1 };
}
