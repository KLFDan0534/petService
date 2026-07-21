import * as THREE from "three";

export function createMaterials() {
  const texture = (base, line, { repeat = [6, 6], grid = true, grain = true } = {}) => {
    if (typeof document === "undefined") return null;

    const canvas = document.createElement("canvas");
    canvas.width = 256;
    canvas.height = 256;
    const ctx = canvas.getContext("2d");
    const baseColor = new THREE.Color(base);
    const lineColor = new THREE.Color(line);
    ctx.fillStyle = `#${baseColor.getHexString()}`;
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    if (grain) {
      for (let y = 0; y < canvas.height; y += 4) {
        for (let x = 0; x < canvas.width; x += 4) {
          const seed = (x * 31 + y * 17) % 29;
          ctx.fillStyle = seed % 2
            ? "rgba(255,255,255,0.035)"
            : "rgba(0,0,0,0.035)";
          ctx.fillRect(x, y, 4, 4);
        }
      }
    }

    if (grid) {
      ctx.strokeStyle = `#${lineColor.getHexString()}`;
      ctx.globalAlpha = 0.3;
      ctx.lineWidth = 2;
      const step = 64;
      for (let p = 0; p <= 256; p += step) {
        ctx.beginPath();
        ctx.moveTo(p, 0);
        ctx.lineTo(p, 256);
        ctx.stroke();
        ctx.beginPath();
        ctx.moveTo(0, p);
        ctx.lineTo(256, p);
        ctx.stroke();
      }
      ctx.globalAlpha = 1;
    }

    const map = new THREE.CanvasTexture(canvas);
    map.colorSpace = THREE.SRGBColorSpace;
    map.wrapS = THREE.RepeatWrapping;
    map.wrapT = THREE.RepeatWrapping;
    map.repeat.set(repeat[0], repeat[1]);
    return map;
  };

  const standard = (color, options = {}) =>
    new THREE.MeshStandardMaterial({
      color,
      map: options.map ?? null,
      roughness: options.roughness ?? 0.62,
      metalness: options.metalness ?? 0.08,
      emissive: options.emissive ?? 0x000000,
      emissiveIntensity: options.emissiveIntensity ?? 0,
      transparent: options.transparent ?? false,
      opacity: options.opacity ?? 1
    });

  return {
    floor: standard(0x26393b, { roughness: 0.78 }),
    path: standard(0x52d69a, { roughness: 0.32, emissive: 0x18583a, emissiveIntensity: 0.55 }),
    foundation: standard(0x555d59, { roughness: 0.86, map: texture(0x555d59, 0x3d4643, { repeat: [10, 7] }) }),
    floorWarm: standard(0xb9b091, { roughness: 0.72, map: texture(0xb9b091, 0x847b65, { repeat: [3, 2.4] }) }),
    wall: standard(0x6f817a, { roughness: 0.76, map: texture(0x6f817a, 0x52645e, { repeat: [4, 3], grid: false }) }),
    wallLight: standard(0xc2bea6, { roughness: 0.76, map: texture(0xc2bea6, 0x989278, { repeat: [3, 2.4], grid: false }) }),
    greenWall: standard(0x78a382, { roughness: 0.82, map: texture(0x78a382, 0x4f7459, { repeat: [3, 2.4], grid: false }) }),
    blueWall: standard(0x438da3, { roughness: 0.64, map: texture(0x438da3, 0x2a6070, { repeat: [3, 2.4], grid: false }) }),
    catWall: standard(0xb08b5d, { roughness: 0.68, map: texture(0xb08b5d, 0x7e6039, { repeat: [3, 2.4], grid: false }) }),
    glass: new THREE.MeshPhysicalMaterial({
      color: 0x9be8d0,
      roughness: 0.16,
      metalness: 0,
      transmission: 0.28,
      transparent: true,
      opacity: 0.34,
      clearcoat: 0.7
    }),
    frosted: new THREE.MeshPhysicalMaterial({
      color: 0xd8eef0,
      roughness: 0.28,
      metalness: 0,
      transmission: 0.12,
      transparent: true,
      opacity: 0.48,
      clearcoat: 0.4
    }),
    beam: standard(0x30464a, { roughness: 0.48, metalness: 0.28 }),
    wood: standard(0x8f5f3d, { roughness: 0.62, map: texture(0x8f5f3d, 0x6b4328, { repeat: [5, 1.6], grid: false }) }),
    darkWood: standard(0x3d2515, { roughness: 0.62, map: texture(0x3d2515, 0x2a170d, { repeat: [4, 1.4], grid: false }) }),
    roofRed: standard(0x9b2d1d, { roughness: 0.62, metalness: 0.12 }),
    roofCopper: standard(0xb66432, { roughness: 0.42, metalness: 0.32 }),
    roofGreen: standard(0x2f7a3a, { roughness: 0.86 }),
    roofWhite: standard(0xccf2ef, { roughness: 0.22, transparent: true, opacity: 0.56 }),
    turf: standard(0x24783b, { roughness: 0.9, map: texture(0x24783b, 0x1c5d2f, { repeat: [8, 8], grid: false }) }),
    porcelain: standard(0xe8e6da, { roughness: 0.34 }),
    water: new THREE.MeshPhysicalMaterial({
      color: 0x72d1f0,
      roughness: 0.03,
      metalness: 0,
      transmission: 0.24,
      transparent: true,
      opacity: 0.52,
      clearcoat: 0.9
    }),
    cream: standard(0xd7c28e, { roughness: 0.82 }),
    yellow: standard(0xf4bf2f, { roughness: 0.56 }),
    purple: standard(0x7652a0, { roughness: 0.58 }),
    metal: standard(0xa7aaa5, { roughness: 0.3, metalness: 0.64 }),
    labelDark: standard(0x171712, { roughness: 0.66 }),
    softGreen: standard(0x5bd99d, { roughness: 0.55, emissive: 0x0d3b26, emissiveIntensity: 0.16 }),
    washBlue: standard(0x4cb7d8, { roughness: 0.42, emissive: 0x0b3645, emissiveIntensity: 0.2 }),
    restAmber: standard(0xf0a84d, { roughness: 0.58, emissive: 0x4f2b06, emissiveIntensity: 0.12 }),
    medicalRose: standard(0xf06f86, { roughness: 0.5, emissive: 0x4d1020, emissiveIntensity: 0.14 }),
    productViolet: standard(0xa98cff, { roughness: 0.5, emissive: 0x251554, emissiveIntensity: 0.12 }),
    staffTeal: standard(0x42d4c3, { roughness: 0.46, emissive: 0x0a4844, emissiveIntensity: 0.14 }),
    staffGreen: standard(0x67c878, { roughness: 0.5, emissive: 0x0f3b1d, emissiveIntensity: 0.12 }),
    furGray: standard(0x8d9b99, { roughness: 0.72 }),
    white: standard(0xdce8e0, { roughness: 0.58 }),
    dark: standard(0x182225, { roughness: 0.7 }),
    rubber: standard(0x11171a, { roughness: 0.9 }),
    screen: standard(0x7af0c2, { roughness: 0.25, emissive: 0x2ad084, emissiveIntensity: 0.85 }),
    coral: standard(0xff8f63, { roughness: 0.48, emissive: 0x562411, emissiveIntensity: 0.12 }),
    edgeTrim: standard(0x20373a, { roughness: 0.42, metalness: 0.28 }),
    warmTrim: standard(0xe4c079, { roughness: 0.5, metalness: 0.16 }),
    terracotta: standard(0xc76342, { roughness: 0.58 }),
    rope: standard(0xc8ad75, { roughness: 0.86 }),
    soil: standard(0x4a3526, { roughness: 0.9 }),
    leafBright: standard(0x7ed17d, { roughness: 0.76, emissive: 0x173d19, emissiveIntensity: 0.06 }),
    softWhite: standard(0xf2eee2, { roughness: 0.52 }),
    matteBlack: standard(0x0f1719, { roughness: 0.86 })
  };
}
