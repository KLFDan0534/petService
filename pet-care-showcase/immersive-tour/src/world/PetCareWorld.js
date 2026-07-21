import * as THREE from "three";
import { HOTSPOT_DEFINITIONS } from "../config/tourStops.js";
import { Hotspot } from "../interaction/Hotspot.js";
import { FiveRoomCampus, ROOM_BASES } from "./FiveRoomCampus.js";
import { createMaterials } from "./materials.js";

export class PetCareWorld {
  constructor({ assets, profile }) {
    this.assets = assets;
    this.profile = profile;
    this.materials = createMaterials(profile);
    this.group = new THREE.Group();
    this.group.name = "PetCareWorld";
    this.areas = new Map();
    this.hotspots = [];
    this.hotspotTargets = [];
    this.loadedAreaModels = new Set();
  }

  build() {
    this.addEnvironmentLighting();

    if (this.assets.centerModel) {
      this.addExternalCenterModel(this.assets.centerModel.scene);
    } else {
      const campus = new FiveRoomCampus({
        materials: this.materials,
        assets: this.assets,
        profile: this.profile
      });
      this.group.add(campus.build());
    }
    this.buildHotspots();
    return this;
  }

  addEnvironmentLighting() {
    const hemisphere = new THREE.HemisphereLight(0xc9fff1, 0x253532, 1.65);
    hemisphere.name = "environment-hemisphere-light";
    this.group.add(hemisphere);

    const sun = new THREE.DirectionalLight(0xfff3d6, this.profile.isMobile ? 1.3 : 2.1);
    sun.name = "cinematic-key-light";
    sun.position.set(-7, 12, 8);
    sun.castShadow = this.profile.shadows;
    if (sun.castShadow) {
      sun.shadow.mapSize.set(this.profile.shadowMapSize, this.profile.shadowMapSize);
      sun.shadow.camera.near = 1;
      sun.shadow.camera.far = 45;
      sun.shadow.camera.left = -18;
      sun.shadow.camera.right = 18;
      sun.shadow.camera.top = 18;
      sun.shadow.camera.bottom = -18;
    }
    this.group.add(sun);

    const fill = new THREE.DirectionalLight(0x77b7ff, 0.6);
    fill.name = "cool-fill-light";
    fill.position.set(8, 5, -10);
    this.group.add(fill);
  }

  addExternalCenterModel(scene) {
    const model = scene.clone(true);
    model.name = "ExternalPetCareCenterGLB";
    model.position.set(0, 0, 0);
    model.scale.setScalar(1);
    this.group.add(model);
  }

  buildHotspots() {
    for (const data of HOTSPOT_DEFINITIONS) {
      const hotspot = new Hotspot(data, { profile: this.profile });
      this.hotspots.push(hotspot);
      this.hotspotTargets.push(...hotspot.clickTargets);
      this.group.add(hotspot.group);
    }
  }

  addAreaModel(areaKey, gltf) {
    if (!gltf || this.loadedAreaModels.has(areaKey)) return;
    const area = this.areas.get(areaKey);

    const model = gltf.scene.clone(true);
    model.name = `ExternalAreaModel-${areaKey}`;
    model.scale.setScalar(1);
    if (area) {
      model.position.set(0, 0, 0);
      area.node.add(model);
      area.externalModel = model;
    } else {
      const base = ROOM_BASES[areaKey] || [0, 0, 0];
      model.position.set(base[0], base[1], base[2]);
      this.group.add(model);
    }
    this.loadedAreaModels.add(areaKey);
  }

  update(delta, elapsed, camera) {
    for (const hotspot of this.hotspots) {
      hotspot.update(delta, elapsed, camera);
    }
  }

  getHotspotTargets(areaKey = null) {
    if (!areaKey) return this.hotspotTargets;
    return this.hotspots
      .filter((hotspot) => hotspot.data.areaKey === areaKey)
      .flatMap((hotspot) => hotspot.clickTargets);
  }

  setActiveHotspotArea(areaKey) {
    for (const hotspot of this.hotspots) {
      hotspot.setActive(!areaKey || hotspot.data.areaKey === areaKey);
    }
  }
}
