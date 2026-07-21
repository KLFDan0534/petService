import * as THREE from "three";
import { RoundedBoxGeometry } from "three/addons/geometries/RoundedBoxGeometry.js";

export function box({ name, size, position, material, castShadow = true, receiveShadow = true, radius = 0, segments = 3 }) {
  const maxRadius = Math.min(size[0], size[1], size[2]) * 0.48;
  const safeRadius = Math.min(radius, maxRadius);
  const geometry = radius > 0
    ? new RoundedBoxGeometry(size[0], size[1], size[2], segments, safeRadius)
    : new THREE.BoxGeometry(size[0], size[1], size[2]);
  const mesh = new THREE.Mesh(geometry, material);
  mesh.name = name;
  mesh.position.set(position[0], position[1], position[2]);
  mesh.castShadow = castShadow;
  mesh.receiveShadow = receiveShadow;
  return mesh;
}

export function cylinder({ name, radiusTop, radiusBottom, height, position, material, radialSegments = 24 }) {
  const geometry = new THREE.CylinderGeometry(radiusTop, radiusBottom, height, radialSegments);
  const mesh = new THREE.Mesh(geometry, material);
  mesh.name = name;
  mesh.position.set(position[0], position[1], position[2]);
  mesh.castShadow = true;
  mesh.receiveShadow = true;
  return mesh;
}

export function sphere({ name, radius, position, material, widthSegments = 24, heightSegments = 16 }) {
  const geometry = new THREE.SphereGeometry(radius, widthSegments, heightSegments);
  const mesh = new THREE.Mesh(geometry, material);
  mesh.name = name;
  mesh.position.set(position[0], position[1], position[2]);
  mesh.castShadow = true;
  mesh.receiveShadow = true;
  return mesh;
}

export function plane({ name, size, position, rotation = [-Math.PI / 2, 0, 0], material }) {
  const geometry = new THREE.PlaneGeometry(size[0], size[1]);
  const mesh = new THREE.Mesh(geometry, material);
  mesh.name = name;
  mesh.position.set(position[0], position[1], position[2]);
  mesh.rotation.set(rotation[0], rotation[1], rotation[2]);
  mesh.receiveShadow = true;
  return mesh;
}

export function createAreaLight({ color, intensity, position, distance = 8 }) {
  const light = new THREE.PointLight(color, intensity, distance, 1.7);
  light.position.set(position[0], position[1], position[2]);
  light.castShadow = false;
  return light;
}

export function createLabelSprite(text, { color = "#eafff4", accent = "#52d69a", scale = [2.2, 0.48, 1] } = {}) {
  const canvas = document.createElement("canvas");
  canvas.width = 512;
  canvas.height = 128;
  const ctx = canvas.getContext("2d");
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.fillStyle = "rgba(10, 20, 22, 0.72)";
  roundRect(ctx, 8, 16, 496, 88, 18);
  ctx.fill();
  ctx.strokeStyle = accent;
  ctx.lineWidth = 3;
  ctx.stroke();
  ctx.fillStyle = color;
  ctx.font = "700 38px Microsoft YaHei, Nunito Sans, sans-serif";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText(text, 256, 61, 440);

  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  const material = new THREE.SpriteMaterial({
    map: texture,
    transparent: true,
    depthWrite: false
  });
  const sprite = new THREE.Sprite(material);
  sprite.name = `label-${text}`;
  sprite.scale.set(scale[0], scale[1], scale[2]);
  return sprite;
}

export function createPoster(texture, { name, size = [3.2, 2], position, rotationY = 0, fallbackMaterial }) {
  const material = texture
    ? new THREE.MeshBasicMaterial({ map: texture, toneMapped: false })
    : fallbackMaterial;
  const mesh = new THREE.Mesh(new THREE.PlaneGeometry(size[0], size[1]), material);
  mesh.name = name;
  mesh.position.set(position[0], position[1], position[2]);
  mesh.rotation.y = rotationY;
  return mesh;
}

export function createPetSilhouette(materials, { position, scale = 1, colorMaterial = "coral" }) {
  const group = new THREE.Group();
  group.name = "low-poly-pet";
  group.position.set(position[0], position[1], position[2]);
  group.scale.setScalar(scale);

  const body = sphere({
    name: "pet-body",
    radius: 0.34,
    position: [0, 0.34, 0],
    material: materials[colorMaterial],
    widthSegments: 18,
    heightSegments: 12
  });
  body.scale.set(1.28, 0.72, 0.82);

  const head = sphere({
    name: "pet-head",
    radius: 0.24,
    position: [0.42, 0.54, 0.03],
    material: materials[colorMaterial],
    widthSegments: 18,
    heightSegments: 12
  });
  const earA = box({ name: "pet-ear-a", size: [0.1, 0.24, 0.08], position: [0.47, 0.77, 0.13], material: materials[colorMaterial] });
  const earB = box({ name: "pet-ear-b", size: [0.1, 0.24, 0.08], position: [0.47, 0.77, -0.13], material: materials[colorMaterial] });
  const tail = cylinder({
    name: "pet-tail",
    radiusTop: 0.035,
    radiusBottom: 0.05,
    height: 0.5,
    position: [-0.46, 0.54, 0],
    material: materials[colorMaterial],
    radialSegments: 10
  });
  tail.rotation.z = Math.PI / 2.8;

  group.add(body, head, earA, earB, tail);
  return group;
}

function roundRect(ctx, x, y, width, height, radius) {
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.arcTo(x + width, y, x + width, y + height, radius);
  ctx.arcTo(x + width, y + height, x, y + height, radius);
  ctx.arcTo(x, y + height, x, y, radius);
  ctx.arcTo(x, y, x + width, y, radius);
  ctx.closePath();
}
