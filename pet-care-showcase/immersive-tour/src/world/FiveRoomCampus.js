import * as THREE from "three";
import { box, createAreaLight, createLabelSprite, cylinder, sphere } from "./primitives.js";

export const ROOM_BASES = {
  daycare: [-8.6, 0, 4.65],
  spa: [0, 0, 4.65],
  boarding: [8.6, 0, 4.65],
  catloft: [-4.45, 0, -3.55],
  conservatory: [5.15, 0, -3.55]
};

export class FiveRoomCampus {
  constructor({ materials, assets, profile }) {
    this.materials = materials;
    this.assets = assets;
    this.profile = profile;
    this.group = new THREE.Group();
    this.group.name = "FiveRoomCampus";
  }

  build() {
    this.addCampusBase();
    this.addDaycareTrainingHall();
    this.addSpaGroomingHouse();
    this.addBoardingLodge();
    this.addCatLoft();
    this.addSmallAnimalConservatory();
    this.addOverviewDetails();
    return this.group;
  }

  addCampusBase() {
    this.group.add(
      box({
        name: "five-room-campus-foundation",
        size: [25.6, 0.18, 16.6],
        position: [0, -0.12, 0.1],
        material: this.materials.foundation,
        radius: 0.12,
        segments: 4
      }),
      box({
        name: "five-room-muted-courtyard",
        size: [23.4, 0.045, 14.2],
        position: [0, -0.005, 0.1],
        material: this.materials.floorWarm,
        radius: 0.08,
        segments: 4
      })
    );

    const pads = [
      { key: "daycare", base: ROOM_BASES.daycare, size: [7.25, 0.05, 5.55] },
      { key: "spa", base: ROOM_BASES.spa, size: [7.25, 0.05, 5.55] },
      { key: "boarding", base: ROOM_BASES.boarding, size: [7.25, 0.05, 5.55] },
      { key: "catloft", base: ROOM_BASES.catloft, size: [7.05, 0.05, 5.65] },
      { key: "conservatory", base: ROOM_BASES.conservatory, size: [7.15, 0.05, 7.15] }
    ];

    for (const pad of pads) {
      this.group.add(box({
        name: `${pad.key}-room-shadow-plinth`,
        size: pad.size,
        position: [pad.base[0], 0.025, pad.base[2]],
        material: this.materials.edgeTrim,
        castShadow: false,
        receiveShadow: true,
        radius: 0.1,
        segments: 4
      }));
    }

    this.group.add(
      box({
        name: "front-check-in-threshold",
        size: [8.2, 0.06, 0.52],
        position: [-4.2, 0.08, 7.45],
        material: this.materials.warmTrim,
        radius: 0.08,
        segments: 4
      }),
      box({
        name: "rear-conservatory-service-deck",
        size: [7.8, 0.05, 0.5],
        position: [4.9, 0.08, -7.25],
        material: this.materials.wood,
        radius: 0.08,
        segments: 4
      })
    );
  }

  addDaycareTrainingHall() {
    const base = ROOM_BASES.daycare;
    const room = this.createRoomGroup("daycare", base);
    this.addRectShell(room, {
      code: "H01",
      title: "日托训练大厅",
      width: 6.4,
      depth: 4.7,
      wall: this.materials.wallLight,
      roof: this.materials.roofRed,
      roofType: "gable"
    });

    this.localBox(room, "rubber-agility-lane", [-1.55, 0.12, -0.55], [2.55, 0.06, 3.25], this.materials.rubber, 0.03);
    this.localBox(room, "social-turf-zone", [1.55, 0.13, -0.2], [2.35, 0.055, 3.1], this.materials.turf, 0.03);

    for (let i = 0; i < 3; i += 1) {
      const z = -1.45 + i * 0.9;
      this.localCylinder(room, `hurdle-left-${i}`, [-2.45, 0.5, z], 0.035, 0.82, this.materials.labelDark, 14);
      this.localCylinder(room, `hurdle-right-${i}`, [-0.85, 0.5, z], 0.035, 0.82, this.materials.labelDark, 14);
      const bar = this.localCylinder(room, `hurdle-bar-${i}`, [-1.65, 0.82, z], 0.035, 1.6, this.materials.coral, 18);
      bar.rotation.z = Math.PI / 2;
    }

    const tunnel = this.localCylinder(room, "fabric-training-tunnel", [1.3, 0.44, -1.35], 0.36, 1.45, this.materials.purple, 36);
    tunnel.rotation.z = Math.PI / 2;
    this.localBox(room, "trainer-bench", [2.1, 0.58, 1.25], [0.95, 0.12, 0.44], this.materials.wood, 0.04);
    this.localBox(room, "trainer-bench-leg-a", [1.78, 0.32, 1.12], [0.08, 0.42, 0.08], this.materials.darkWood, 0.02);
    this.localBox(room, "trainer-bench-leg-b", [2.42, 0.32, 1.12], [0.08, 0.42, 0.08], this.materials.darkWood, 0.02);
    this.localBox(room, "trainer-bench-leg-c", [1.78, 0.32, 1.38], [0.08, 0.42, 0.08], this.materials.darkWood, 0.02);
    this.localBox(room, "trainer-bench-leg-d", [2.42, 0.32, 1.38], [0.08, 0.42, 0.08], this.materials.darkWood, 0.02);
    const hoop = this.localMesh(new THREE.TorusGeometry(0.45, 0.025, 12, 44), this.materials.coral, [0.42, 0.92, -1.55]);
    hoop.rotation.y = Math.PI / 2;
    room.add(hoop);
    this.localCylinder(room, "agility-hoop-stand-left", [0.42, 0.47, -2.0], 0.028, 0.82, this.materials.edgeTrim, 12);
    this.localCylinder(room, "agility-hoop-stand-right", [0.42, 0.47, -1.1], 0.028, 0.82, this.materials.edgeTrim, 12);
    for (let i = 0; i < 5; i += 1) {
      const cone = this.localCylinder(room, `slalom-cone-${i}`, [-2.55 + i * 0.45, 0.28, 1.25 + (i % 2) * 0.32], 0.105, 0.32, this.materials.yellow, 16);
      cone.scale.x = 0.72;
      cone.scale.z = 0.72;
    }
    this.addWallConsole(room, "daycare-behavior-scoreboard", [2.86, 1.46, 2.28], Math.PI, this.materials.softGreen);
    this.addBowl(room, "daycare-water", [-2.52, 0.2, 1.76], this.materials.water);
    this.addPlanter(room, "daycare-entry-planter", [2.62, 0.26, -1.82], 0.82);
    this.addPerson(room, "lead-trainer", [-2.75, 0.05, -1.95], this.materials.staffGreen || this.materials.softGreen);
    this.addPerson(room, "assistant-handler", [0.15, 0.05, 1.3], this.materials.staffTeal);
    this.addPet(room, "retriever", [-1.15, 0.08, -1.42], this.materials.restAmber, 1);
    this.addPet(room, "collie", [1.7, 0.08, 0.8], this.materials.white, 0.75);
    this.addRoomLightAndLabel(room, "训练日托", "#56d68f", [0, 2.05, -2.1], 0x7bf0b7);
    this.group.add(room);
  }

  addSpaGroomingHouse() {
    const base = ROOM_BASES.spa;
    const room = this.createRoomGroup("spa", base);
    this.addRectShell(room, {
      code: "H02",
      title: "洗护水疗房",
      width: 6.4,
      depth: 4.7,
      wall: this.materials.blueWall,
      roof: this.materials.roofCopper,
      roofType: "pyramid"
    });

    this.localBox(room, "wet-room-floor", [-1.3, 0.13, -0.2], [2.35, 0.06, 3.3], this.materials.washBlue, 0.02);
    const basin = this.localCylinder(room, "elevated-round-wash-basin", [-1.35, 0.48, -0.7], 0.68, 0.45, this.materials.porcelain, 48);
    basin.scale.y = 0.72;
    const water = this.localCylinder(room, "calm-water-surface", [-1.35, 0.73, -0.7], 0.48, 0.035, this.materials.water, 48);
    water.scale.y = 0.68;
    this.addTube(room, "arched-shower-rail", [[-1.92, 0.95, -0.7], [-1.92, 1.9, -0.7], [-1.35, 2.2, -0.7], [-0.76, 1.9, -0.7]], 0.03, this.materials.metal);
    const showerHead = this.localCylinder(room, "rainfall-shower-head", [-1.35, 2.14, -0.7], 0.17, 0.05, this.materials.metal, 36);
    showerHead.rotation.x = Math.PI / 2;

    this.localCylinder(room, "grooming-table-pedestal", [1.25, 0.52, 0.25], 0.08, 0.86, this.materials.metal, 28);
    this.localBox(room, "rounded-grooming-table", [1.25, 0.98, 0.25], [1.35, 0.12, 0.76], this.materials.porcelain, 0.08);
    this.localBox(room, "supply-shelf", [2.35, 1.15, 1.9], [0.9, 0.08, 0.25], this.materials.wood, 0.02);
    for (let i = 0; i < 4; i += 1) {
      this.localCylinder(room, `wash-bottle-${i}`, [2.02 + i * 0.22, 1.35, 1.82], 0.055, 0.28, [this.materials.yellow, this.materials.purple, this.materials.coral, this.materials.softGreen][i], 18);
    }
    this.localBox(room, "wall-mounted-dryer", [2.74, 1.58, -0.8], [0.16, 0.5, 0.42], this.materials.edgeTrim, 0.04);
    const dryerHose = this.addTube(room, "flexible-dryer-hose", [[2.62, 1.5, -0.8], [2.1, 1.24, -0.64], [1.64, 1.15, -0.18]], 0.035, this.materials.rubber);
    dryerHose.scale.set(1, 1, 1);
    this.localBox(room, "spa-back-mirror", [-2.25, 1.55, 2.28], [1.25, 0.86, 0.05], this.materials.frosted, 0.04);
    this.localBox(room, "folded-towel-stack-a", [2.2, 1.55, 1.82], [0.32, 0.08, 0.22], this.materials.softWhite, 0.025);
    this.localBox(room, "folded-towel-stack-b", [2.22, 1.65, 1.82], [0.3, 0.08, 0.22], this.materials.washBlue, 0.025);
    for (let i = 0; i < 5; i += 1) {
      this.localCylinder(room, `wet-room-drain-slot-${i}`, [-2.2 + i * 0.26, 0.17, 1.18], 0.01, 0.34, this.materials.edgeTrim, 8).rotation.z = Math.PI / 2;
    }
    this.addWallConsole(room, "spa-water-quality-console", [2.86, 1.48, 0.96], -Math.PI / 2, this.materials.washBlue);
    this.localBox(room, "frosted-recovery-cubicle", [1.9, 0.9, -1.5], [1.08, 1.62, 0.9], this.materials.frosted, 0.02);
    this.addPerson(room, "groomer", [-2.45, 0.05, -0.95], this.materials.staffTeal);
    this.addPerson(room, "skin-check", [0.68, 0.05, 0.2], this.materials.softGreen);
    this.addPet(room, "husky-in-basin", [-1.35, 0.76, -0.7], this.materials.furGray || this.materials.wall, 0.68);
    this.addPet(room, "small-dog-table", [1.25, 1.06, 0.25], this.materials.white, 0.52);
    this.addRoomLightAndLabel(room, "洗护水疗", "#38bdf8", [0, 2.05, -2.1], 0x62d7ff);
    this.group.add(room);
  }

  addBoardingLodge() {
    const base = ROOM_BASES.boarding;
    const room = this.createRoomGroup("boarding", base);
    this.addRectShell(room, {
      code: "H03",
      title: "安静寄养房",
      width: 6.4,
      depth: 4.7,
      wall: this.materials.greenWall,
      roof: this.materials.roofGreen,
      roofType: "flat"
    });

    for (let i = 0; i < 4; i += 1) {
      const x = -2.25 + i * 1.5;
      this.localBox(room, `kennel-suite-${i}`, [x, 0.47, 0.6], [1.18, 0.12, 1.2], this.materials.wood, 0.03);
      this.localBox(room, `kennel-back-${i}`, [x, 1.04, 1.18], [1.18, 1.08, 0.08], this.materials.darkWood, 0.01);
      this.localBox(room, `kennel-bed-${i}`, [x, 0.57, 0.42], [0.9, 0.08, 0.68], this.materials.cream, 0.06);
      for (let j = 0; j < 5; j += 1) {
        this.localCylinder(room, `kennel-bar-${i}-${j}`, [x - 0.45 + j * 0.22, 0.93, -0.02], 0.015, 0.82, this.materials.labelDark, 8);
      }
      this.localBox(room, `kennel-suite-number-${i}`, [x, 1.62, 1.1], [0.42, 0.18, 0.05], i % 2 ? this.materials.softGreen : this.materials.warmTrim, 0.025);
      this.localCylinder(room, `kennel-status-sensor-${i}`, [x + 0.48, 1.5, 1.08], 0.035, 0.025, this.materials.screen, 16).rotation.x = Math.PI / 2;
      this.addBowl(room, `kennel-bowl-${i}`, [x - 0.3, 0.2, -0.32], i % 2 ? this.materials.water : this.materials.restAmber);
    }

    this.localBox(room, "outdoor-run-turf", [0, 0.13, -1.45], [5.45, 0.055, 1.3], this.materials.turf, 0.03);
    this.localBox(room, "privacy-baffle-a", [-1.55, 1.05, -0.75], [0.12, 1.8, 1.28], this.materials.frosted, 0.01);
    this.localBox(room, "privacy-baffle-b", [1.55, 1.05, -0.75], [0.12, 1.8, 1.28], this.materials.frosted, 0.01);
    this.localBox(room, "acoustic-wall-pad-a", [-2.55, 1.38, 2.24], [1.0, 0.76, 0.08], this.materials.cream, 0.04);
    this.localBox(room, "acoustic-wall-pad-b", [-1.3, 1.38, 2.24], [1.0, 0.76, 0.08], this.materials.cream, 0.04);
    this.localBox(room, "night-light-rail", [0, 2.05, -1.9], [4.8, 0.08, 0.08], this.materials.screen, 0.02);
    this.addWallConsole(room, "boarding-occupancy-console", [2.86, 1.42, 2.12], Math.PI, this.materials.restAmber);
    this.addPlanter(room, "boarding-calm-planter-left", [-2.62, 0.24, -1.82], 0.78);
    this.addPlanter(room, "boarding-calm-planter-right", [2.62, 0.24, -1.82], 0.78);
    this.addPerson(room, "night-caretaker", [-2.8, 0.05, -1.3], this.materials.staffTeal);
    this.addPerson(room, "feeding-staff", [2.2, 0.05, -1.15], this.materials.softGreen);
    this.addPet(room, "corgi", [-2.25, 0.16, 0.25], this.materials.restAmber, 0.55);
    this.addPet(room, "black-lab", [0.75, 0.16, 0.2], this.materials.dark, 0.72);
    this.addPet(room, "tuxedo-cat", [2.25, 0.14, 0.22], this.materials.white, 0.5);
    this.addRoomLightAndLabel(room, "安静寄养", "#f5b14c", [0, 2.05, -2.1], 0xffbe62);
    this.group.add(room);
  }

  addCatLoft() {
    const base = ROOM_BASES.catloft;
    const room = this.createRoomGroup("catloft", base);
    this.addRectShell(room, {
      code: "H04",
      title: "猫咪跃层乐园",
      width: 6.2,
      depth: 4.8,
      wall: this.materials.catWall,
      roof: this.materials.roofRed,
      roofType: "gable"
    });

    this.localBox(room, "second-story-balcony", [0.4, 1.52, 0.8], [3.8, 0.12, 1.0], this.materials.wood, 0.04);
    const perchPoints = [[-2.45, 0.55], [-1.82, 0.95], [-1.2, 1.35], [-0.5, 1.75], [0.28, 2.05]];
    perchPoints.forEach(([x, y], i) => {
      this.localBox(room, `wall-perch-${i}`, [x, y, 2.14], [0.56, 0.08, 0.25], this.materials.cream, 0.04);
    });
    this.localCylinder(room, "rope-scratching-tower", [1.95, 0.94, -0.35], 0.13, 1.72, this.materials.darkWood, 28);
    for (let i = 0; i < 8; i += 1) {
      this.localCylinder(room, `rope-wrap-${i}`, [1.95, 0.32 + i * 0.17, -0.35], 0.136, 0.022, this.materials.cream, 24);
    }
    this.localBox(room, "high-lookout-platform", [1.95, 1.82, -0.35], [0.88, 0.1, 0.72], this.materials.cream, 0.07);
    this.addTube(room, "suspended-cat-bridge-left", [[-0.65, 1.7, 0.8], [0.95, 1.7, 0.8]], 0.015, this.materials.labelDark);
    this.addTube(room, "suspended-cat-bridge-right", [[-0.65, 1.7, 1.05], [0.95, 1.7, 1.05]], 0.015, this.materials.labelDark);
    for (let i = 0; i < 4; i += 1) {
      this.localBox(room, `cat-bridge-plank-${i}`, [-0.45 + i * 0.45, 1.68, 0.925], [0.3, 0.045, 0.32], this.materials.wood, 0.01);
    }
    for (let i = 0; i < 6; i += 1) {
      this.localBox(room, `cat-ladder-rung-${i}`, [-2.28 + i * 0.26, 0.55 + i * 0.18, -1.58], [0.36, 0.045, 0.12], this.materials.wood, 0.01);
    }
    this.addTube(room, "cat-ladder-left-rail", [[-2.5, 0.42, -1.64], [-0.92, 1.5, -1.64]], 0.022, this.materials.edgeTrim);
    this.addTube(room, "cat-ladder-right-rail", [[-2.5, 0.42, -1.48], [-0.92, 1.5, -1.48]], 0.022, this.materials.edgeTrim);
    this.localBox(room, "enclosed-litter-cabinet", [2.3, 0.45, 1.55], [0.84, 0.62, 0.62], this.materials.darkWood, 0.06);
    this.localBox(room, "litter-cabinet-entry", [2.3, 0.42, 1.23], [0.36, 0.32, 0.05], this.materials.matteBlack, 0.04);
    const toy = this.localMesh(new THREE.TorusGeometry(0.18, 0.018, 10, 28), this.materials.yellow, [-1.05, 0.24, -0.78]);
    toy.rotation.x = Math.PI / 2;
    room.add(toy);
    this.addWallConsole(room, "cat-behavior-console", [2.78, 1.42, 2.23], Math.PI, this.materials.productViolet);
    this.addPerson(room, "cat-behaviorist", [-2.65, 0.05, -1.45], this.materials.softGreen);
    this.addPet(room, "orange-tabby", [-1.2, 1.43, 2.02], this.materials.coral, 0.45);
    this.addPet(room, "calico", [0.4, 1.69, 0.82], this.materials.restAmber, 0.45);
    this.addPet(room, "black-cat", [1.95, 1.9, -0.35], this.materials.dark, 0.45);
    this.addRoomLightAndLabel(room, "猫咪跃层", "#d8a15c", [0, 2.12, -2.18], 0xffcb7a);
    this.group.add(room);
  }

  addSmallAnimalConservatory() {
    const base = ROOM_BASES.conservatory;
    const room = this.createRoomGroup("conservatory", base);
    this.addOctagonShell(room);

    this.localCylinder(room, "center-aviary-column", [-0.15, 1.18, -0.15], 0.62, 1.9, this.materials.frosted, 48);
    for (const y of [0.7, 1.05, 1.4]) {
      const perch = this.localCylinder(room, `aviary-perch-${y}`, [-0.15, y, -0.15], 0.018, 1.24, this.materials.wood, 12);
      perch.rotation.z = Math.PI / 2;
    }
    this.localBox(room, "rabbit-pen-left", [-1.85, 0.36, -0.95], [1.15, 0.1, 1.0], this.materials.wood, 0.03);
    this.localBox(room, "rabbit-pen-right", [-1.85, 0.36, 0.22], [1.15, 0.1, 1.0], this.materials.wood, 0.03);
    for (const z of [-1.45, -0.45, 0.72]) {
      const rail = this.localCylinder(room, `rabbit-pen-rail-${z}`, [-1.85, 0.72, z], 0.018, 1.2, this.materials.labelDark, 8);
      rail.rotation.z = Math.PI / 2;
    }
    this.localBox(room, "small-animal-exam-counter", [1.75, 0.86, 1.0], [1.34, 0.12, 0.58], this.materials.porcelain, 0.06);
    this.localCylinder(room, "turtle-shallow-pool", [1.55, 0.22, -1.15], 0.55, 0.1, this.materials.water, 48);
    this.localCylinder(room, "habitat-display-island", [0.82, 0.34, 0.72], 0.62, 0.18, this.materials.wood, 48);
    this.localCylinder(room, "habitat-glass-dome", [0.82, 0.68, 0.72], 0.42, 0.48, this.materials.frosted, 48);
    this.localBox(room, "climate-floor-mat", [-0.18, 0.14, 1.18], [1.25, 0.045, 0.75], this.materials.softGreen, 0.04);
    this.addWallConsole(room, "greenhouse-climate-console", [2.55, 1.32, 0.18], -Math.PI / 2, this.materials.softGreen);
    this.localBox(room, "feed-prep-drawer-a", [1.75, 0.62, 0.68], [1.05, 0.28, 0.08], this.materials.edgeTrim, 0.02);
    this.localBox(room, "feed-prep-drawer-b", [1.75, 0.62, 1.32], [1.05, 0.28, 0.08], this.materials.edgeTrim, 0.02);
    for (let i = 0; i < 8; i += 1) {
      const angle = (i / 8) * Math.PI * 2;
      this.localLeaf(room, `greenhouse-leaf-${i}`, [2.15 + Math.cos(angle) * 0.35, 0.62, -0.05 + Math.sin(angle) * 0.35], angle);
    }
    this.addPlanter(room, "greenhouse-planter-left", [-2.45, 0.24, 1.55], 0.72);
    this.addPlanter(room, "greenhouse-planter-right", [2.34, 0.24, -1.82], 0.72);
    const roofFan = this.localCylinder(room, "greenhouse-roof-fan-core", [0, 2.35, 0], 0.12, 0.06, this.materials.edgeTrim, 24);
    roofFan.rotation.x = Math.PI / 2;
    for (let i = 0; i < 3; i += 1) {
      const blade = this.localBox(room, `greenhouse-roof-fan-blade-${i}`, [0, 2.35, 0], [0.85, 0.025, 0.08], this.materials.frosted, 0.01);
      blade.rotation.y = (i / 3) * Math.PI * 2;
    }
    this.addPerson(room, "exotics-vet", [1.0, 0.05, 1.1], this.materials.staffTeal);
    this.addPet(room, "lop-rabbit", [-1.85, 0.48, -0.95], this.materials.cream, 0.42);
    this.addPet(room, "blue-bird", [0.25, 1.16, -0.15], this.materials.washBlue, 0.32);
    this.addPet(room, "pond-turtle", [1.55, 0.34, -1.15], this.materials.roofGreen, 0.34);
    this.addRoomLightAndLabel(room, "小动物温室", "#7ddfbe", [0, 2.2, -2.25], 0x8cffdf);
    this.group.add(room);
  }

  addOverviewDetails() {
    const label = createLabelSprite("PetService 五房间养护中心", { accent: "#56d68f", scale: [4.6, 0.72, 1] });
    label.position.set(0, 2.55, 7.58);
    this.group.add(label);

    for (let i = 0; i < 8; i += 1) {
      const x = -11 + i * 3.1;
      this.group.add(cylinder({
        name: `campus-planter-${i}`,
        radiusTop: 0.22,
        radiusBottom: 0.19,
        height: 0.34,
        position: [x, 0.18, -7],
        material: this.materials.porcelain,
        radialSegments: 24
      }));
      this.group.add(sphere({
        name: `campus-plant-${i}`,
        radius: 0.18,
        position: [x, 0.48, -7],
        material: this.materials.roofGreen,
        widthSegments: 18,
        heightSegments: 10
      }));
    }
  }

  createRoomGroup(key, base) {
    const group = new THREE.Group();
    group.name = `FiveRoom-${key}`;
    group.position.set(base[0], base[1], base[2]);
    group.userData.areaKey = key;
    return group;
  }

  addRectShell(room, { code, title, width, depth, wall, roof, roofType }) {
    this.localBox(room, `${code}-foundation`, [0, -0.02, 0], [width + 0.55, 0.14, depth + 0.55], this.materials.foundation, 0.04);
    this.localBox(room, `${code}-floor`, [0, 0.07, 0], [width, 0.08, depth], this.materials.floorWarm, 0.03);
    this.localBox(room, `${code}-rear-wall`, [0, 1.18, depth / 2], [width, 2.35, 0.16], wall, 0.02);
    this.localBox(room, `${code}-left-wall`, [-width / 2, 1.18, 0], [0.16, 2.35, depth], wall, 0.02);
    this.localBox(room, `${code}-right-wall`, [width / 2, 1.18, 0], [0.16, 2.35, depth], wall, 0.02);
    this.localBox(room, `${code}-front-low-wall`, [0, 0.42, -depth / 2], [width, 0.72, 0.12], wall, 0.02);
    this.localBox(room, `${code}-front-safety-glass`, [0, 1.42, -depth / 2 - 0.035], [width * 0.82, 1.55, 0.04], this.materials.glass, 0.01);
    this.localBox(room, `${code}-rear-baseboard`, [0, 0.24, depth / 2 - 0.11], [width - 0.26, 0.16, 0.08], this.materials.edgeTrim, 0.01);
    this.localBox(room, `${code}-left-baseboard`, [-width / 2 + 0.11, 0.24, 0], [0.08, 0.16, depth - 0.3], this.materials.edgeTrim, 0.01);
    this.localBox(room, `${code}-right-baseboard`, [width / 2 - 0.11, 0.24, 0], [0.08, 0.16, depth - 0.3], this.materials.edgeTrim, 0.01);
    this.localBox(room, `${code}-front-threshold`, [0, 0.14, -depth / 2 - 0.18], [1.6, 0.08, 0.32], this.materials.warmTrim, 0.04);
    this.localBox(room, `${code}-glass-top-rail`, [0, 2.23, -depth / 2 - 0.055], [width * 0.88, 0.08, 0.08], this.materials.edgeTrim, 0.01);
    this.localBox(room, `${code}-glass-bottom-rail`, [0, 0.66, -depth / 2 - 0.055], [width * 0.88, 0.08, 0.08], this.materials.edgeTrim, 0.01);

    for (const x of [-width / 2 + 0.45, 0, width / 2 - 0.45]) {
      this.localBox(room, `${code}-glass-mullion-${x}`, [x, 1.36, -depth / 2 - 0.06], [0.055, 1.75, 0.055], this.materials.labelDark, 0.005);
    }

    for (const x of [-width / 2 + 0.08, width / 2 - 0.08]) {
      this.localBox(room, `${code}-front-corner-post-${x}`, [x, 1.28, -depth / 2 - 0.05], [0.16, 2.28, 0.16], this.materials.edgeTrim, 0.02);
      this.localBox(room, `${code}-rear-corner-post-${x}`, [x, 1.28, depth / 2 - 0.05], [0.16, 2.28, 0.16], this.materials.edgeTrim, 0.02);
    }

    for (const x of [-width / 2 + 1.05, width / 2 - 1.05]) {
      this.localBox(room, `${code}-rear-window-${x}`, [x, 1.42, depth / 2 + 0.01], [0.82, 0.78, 0.04], this.materials.frosted, 0.02);
      this.localBox(room, `${code}-rear-window-sill-${x}`, [x, 0.95, depth / 2 - 0.08], [0.98, 0.07, 0.12], this.materials.warmTrim, 0.015);
    }

    this.addTube(room, `${code}-warm-ceiling-strip-left`, [[-width / 2 + 0.45, 2.24, -depth / 2 + 0.45], [-width / 2 + 0.45, 2.24, depth / 2 - 0.45]], 0.018, this.materials.screen);
    this.addTube(room, `${code}-warm-ceiling-strip-right`, [[width / 2 - 0.45, 2.24, -depth / 2 + 0.45], [width / 2 - 0.45, 2.24, depth / 2 - 0.45]], 0.018, this.materials.screen);

    if (roofType === "gable") {
      this.addGableRoof(room, `${code}-gable-roof`, width + 0.58, depth + 0.62, 3.02, 3.82, roof);
    } else if (roofType === "pyramid") {
      this.addPyramidRoof(room, `${code}-pyramid-roof`, width + 0.58, depth + 0.62, 3.02, 3.86, roof);
    } else {
      this.localBox(room, `${code}-flat-roof`, [0, 3.1, 0], [width + 0.55, 0.18, depth + 0.55], roof, 0.03);
      this.localBox(room, `${code}-living-roof-pad`, [0, 3.22, 0], [width + 0.08, 0.04, depth + 0.08], this.materials.turf, 0.02);
    }

    const titleSprite = createLabelSprite(title, { scale: [2.5, 0.45, 1] });
    titleSprite.position.set(0, 2.0, -depth / 2 - 0.18);
    room.add(titleSprite);
  }

  addOctagonShell(room) {
    this.localCylinder(room, "H05-octagon-foundation", [0, -0.02, 0], 3.45, 0.14, this.materials.foundation, 8).rotation.y = Math.PI / 8;
    this.localCylinder(room, "H05-cork-floor", [0, 0.08, 0], 3.2, 0.08, this.materials.floorWarm, 8).rotation.y = Math.PI / 8;
    const radius = 3.05;
    const points = Array.from({ length: 8 }, (_, i) => {
      const a = THREE.MathUtils.degToRad(22.5 + i * 45);
      return [Math.cos(a) * radius, Math.sin(a) * radius];
    });

    points.forEach((point, index) => {
      const next = points[(index + 1) % points.length];
      const cx = (point[0] + next[0]) / 2;
      const cz = (point[1] + next[1]) / 2;
      const length = Math.hypot(next[0] - point[0], next[1] - point[1]);
      const angle = Math.atan2(next[1] - point[1], next[0] - point[0]);
      const wallMat = cz < 2.1 ? this.materials.glass : this.materials.greenWall;
      const wall = this.localBox(room, `H05-octagon-wall-${index}`, [cx, 1.18, cz], [length, 2.2, 0.08], wallMat, 0.01);
      wall.rotation.y = -angle;
      this.localBox(room, `H05-mullion-${index}`, [point[0], 1.22, point[1]], [0.055, 2.25, 0.055], this.materials.labelDark, 0.005);
    });

    this.localBox(room, "H05-front-threshold", [0, 0.15, -3.1], [1.34, 0.08, 0.28], this.materials.warmTrim, 0.04);
    this.localBox(room, "H05-front-door-left", [-0.72, 1.08, -3.02], [0.08, 1.7, 0.08], this.materials.edgeTrim, 0.01);
    this.localBox(room, "H05-front-door-right", [0.72, 1.08, -3.02], [0.08, 1.7, 0.08], this.materials.edgeTrim, 0.01);
    this.localBox(room, "H05-front-door-top", [0, 1.92, -3.02], [1.52, 0.08, 0.08], this.materials.edgeTrim, 0.01);
    this.addTube(room, "H05-interior-grow-light-ring", [[-1.4, 2.08, 0], [-0.55, 2.08, 0.72], [0.55, 2.08, 0.72], [1.4, 2.08, 0], [0.55, 2.08, -0.72], [-0.55, 2.08, -0.72], [-1.4, 2.08, 0]], 0.018, this.materials.screen);
    this.addOctagonRoof(room, "H05-greenhouse-roof", 3.38, 2.86, 3.6, this.materials.roofWhite);
    const titleSprite = createLabelSprite("小动物温室", { accent: "#7ddfbe", scale: [1.9, 0.36, 1] });
    titleSprite.position.set(-1.15, 1.95, -3.08);
    room.add(titleSprite);
  }

  addGableRoof(room, name, width, depth, eaveY, ridgeY, material) {
    const x0 = -width / 2;
    const x1 = width / 2;
    const z0 = -depth / 2;
    const z1 = depth / 2;
    const left = [[x0, eaveY, z0], [x0, eaveY, z1], [0, ridgeY, z1], [0, ridgeY, z0]];
    const right = [[0, ridgeY, z0], [0, ridgeY, z1], [x1, eaveY, z1], [x1, eaveY, z0]];
    room.add(this.createMeshPanel(`${name}-left`, left, material), this.createMeshPanel(`${name}-right`, right, material));
    this.addTube(room, `${name}-ridge`, [[0, ridgeY + 0.04, z0 - 0.08], [0, ridgeY + 0.04, z1 + 0.08]], 0.035, this.materials.labelDark);
  }

  addPyramidRoof(room, name, width, depth, eaveY, ridgeY, material) {
    const corners = [[-width / 2, -depth / 2], [width / 2, -depth / 2], [width / 2, depth / 2], [-width / 2, depth / 2]];
    corners.forEach((corner, index) => {
      const next = corners[(index + 1) % corners.length];
      room.add(this.createMeshPanel(`${name}-face-${index}`, [[corner[0], eaveY, corner[1]], [next[0], eaveY, next[1]], [0, ridgeY, 0]], material));
    });
  }

  addOctagonRoof(room, name, radius, eaveY, ridgeY, material) {
    const points = Array.from({ length: 8 }, (_, i) => {
      const a = THREE.MathUtils.degToRad(22.5 + i * 45);
      return [Math.cos(a) * radius, Math.sin(a) * radius];
    });
    points.forEach((point, index) => {
      const next = points[(index + 1) % points.length];
      room.add(this.createMeshPanel(`${name}-face-${index}`, [[point[0], eaveY, point[1]], [next[0], eaveY, next[1]], [0, ridgeY, 0]], material));
    });
  }

  createMeshPanel(name, vertices, material) {
    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute("position", new THREE.Float32BufferAttribute(vertices.flat(), 3));
    geometry.setIndex(vertices.length === 4 ? [0, 1, 2, 0, 2, 3] : [0, 1, 2]);
    geometry.computeVertexNormals();
    const mesh = new THREE.Mesh(geometry, material);
    mesh.name = name;
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    return mesh;
  }

  addRoomLightAndLabel(room, text, accent, position, lightColor) {
    const label = createLabelSprite(text, { accent, scale: [2.2, 0.45, 1] });
    label.position.set(position[0], position[1], position[2]);
    room.add(label);
    room.add(createAreaLight({ color: lightColor, intensity: 1.95, position: [0, 2.45, 0], distance: 7 }));
  }

  localBox(room, name, position, size, material, radius = 0, segments = 3) {
    const mesh = box({ name, size, position, material, radius, segments });
    room.add(mesh);
    return mesh;
  }

  localCylinder(room, name, position, radius, height, material, radialSegments = 24) {
    const mesh = cylinder({
      name,
      radiusTop: radius,
      radiusBottom: radius,
      height,
      position,
      material,
      radialSegments
    });
    room.add(mesh);
    return mesh;
  }

  addTube(room, name, points, radius, material) {
    const curve = new THREE.CatmullRomCurve3(points.map((point) => new THREE.Vector3(point[0], point[1], point[2])));
    const tube = new THREE.Mesh(new THREE.TubeGeometry(curve, 32, radius, 8, false), material);
    tube.name = name;
    tube.castShadow = true;
    tube.receiveShadow = true;
    room.add(tube);
    return tube;
  }

  addPerson(room, name, position, uniformMaterial) {
    const group = new THREE.Group();
    group.name = `person-${name}`;
    group.position.set(position[0], position[1], position[2]);
    group.add(this.localMesh(new THREE.CylinderGeometry(0.16, 0.18, 0.72, 24), uniformMaterial, [0, 0.82, 0]));
    group.add(this.localMesh(new THREE.SphereGeometry(0.14, 24, 12), this.materials.cream, [0, 1.25, 0]));
    group.add(this.localMesh(new THREE.SphereGeometry(0.145, 18, 8), this.materials.dark, [0, 1.34, 0.02], [1, 0.75, 0.45]));
    for (const x of [-0.08, 0.08]) {
      group.add(this.localMesh(new THREE.CylinderGeometry(0.042, 0.046, 0.62, 12), this.materials.dark, [x, 0.35, 0]));
      group.add(this.localMesh(new THREE.CylinderGeometry(0.028, 0.028, 0.58, 12), this.materials.cream, [x * 2.4, 0.85, -0.04], [1, 1, 1], [0, 0, x > 0 ? -0.5 : 0.5]));
    }
    room.add(group);
    return group;
  }

  addPet(room, name, position, material, scale = 1) {
    const group = new THREE.Group();
    group.name = `pet-${name}`;
    group.position.set(position[0], position[1], position[2]);
    group.scale.setScalar(scale);
    group.add(this.localMesh(new THREE.SphereGeometry(0.34, 28, 14), material, [0, 0.27, 0], [1.28, 0.62, 0.75]));
    group.add(this.localMesh(new THREE.SphereGeometry(0.18, 24, 12), material, [-0.36, 0.42, 0]));
    group.add(this.localMesh(new THREE.SphereGeometry(0.08, 16, 8), this.materials.cream, [-0.52, 0.39, 0]));
    group.add(this.localMesh(new THREE.SphereGeometry(0.026, 10, 6), this.materials.matteBlack, [-0.59, 0.4, 0]));
    group.add(this.localMesh(new THREE.SphereGeometry(0.024, 10, 6), this.materials.matteBlack, [-0.49, 0.47, -0.075]));
    group.add(this.localMesh(new THREE.SphereGeometry(0.024, 10, 6), this.materials.matteBlack, [-0.49, 0.47, 0.075]));
    const collar = this.localMesh(new THREE.TorusGeometry(0.155, 0.012, 8, 32), this.materials.warmTrim, [-0.24, 0.43, 0]);
    collar.rotation.y = Math.PI / 2;
    group.add(collar);
    for (const z of [-0.11, 0.11]) {
      group.add(this.localMesh(new THREE.SphereGeometry(0.05, 14, 8), material, [-0.36, 0.57, z], [0.8, 1.4, 0.55]));
    }
    for (const x of [-0.16, 0.16]) {
      for (const z of [-0.1, 0.1]) {
        group.add(this.localMesh(new THREE.CylinderGeometry(0.028, 0.032, 0.26, 10), material, [x, 0.12, z]));
      }
    }
    const tail = this.localMesh(new THREE.CylinderGeometry(0.022, 0.035, 0.42, 10), material, [0.42, 0.38, 0]);
    tail.rotation.z = -0.82;
    group.add(tail);
    room.add(group);
    return group;
  }

  localLeaf(room, name, position, angle) {
    const leaf = sphere({
      name,
      radius: 0.13,
      position,
      material: this.materials.roofGreen,
      widthSegments: 18,
      heightSegments: 8
    });
    leaf.scale.set(0.5, 1.4, 0.18);
    leaf.rotation.y = angle;
    room.add(leaf);
  }

  addBowl(room, name, position, fillMaterial = this.materials.restAmber) {
    const bowl = this.localCylinder(room, `${name}-bowl`, position, 0.18, 0.08, this.materials.metal, 32);
    bowl.scale.y = 0.45;
    const fill = this.localCylinder(room, `${name}-fill`, [position[0], position[1] + 0.055, position[2]], 0.13, 0.02, fillMaterial, 24);
    fill.scale.y = 0.42;
    return bowl;
  }

  addWallConsole(room, name, position, rotationY, accentMaterial = this.materials.screen) {
    const group = new THREE.Group();
    group.name = name;
    group.position.set(position[0], position[1], position[2]);
    group.rotation.y = rotationY;
    group.add(this.localMesh(new THREE.BoxGeometry(0.82, 0.5, 0.055), this.materials.edgeTrim, [0, 0, 0]));
    group.add(this.localMesh(new THREE.BoxGeometry(0.58, 0.28, 0.065), accentMaterial, [0, 0.06, -0.015]));
    for (let i = 0; i < 3; i += 1) {
      group.add(this.localMesh(new THREE.CylinderGeometry(0.03, 0.03, 0.012, 12), this.materials.warmTrim, [-0.24 + i * 0.24, -0.18, -0.04], [1, 1, 1], [Math.PI / 2, 0, 0]));
    }
    room.add(group);
    return group;
  }

  addPlanter(room, name, position, scale = 1) {
    const pot = this.localCylinder(room, `${name}-pot`, position, 0.16 * scale, 0.24 * scale, this.materials.terracotta, 24);
    pot.scale.y = 0.78;
    this.localCylinder(room, `${name}-soil`, [position[0], position[1] + 0.11 * scale, position[2]], 0.13 * scale, 0.025 * scale, this.materials.soil, 20);
    for (let i = 0; i < 5; i += 1) {
      const angle = (i / 5) * Math.PI * 2;
      this.localLeaf(room, `${name}-leaf-${i}`, [
        position[0] + Math.cos(angle) * 0.13 * scale,
        position[1] + 0.23 * scale,
        position[2] + Math.sin(angle) * 0.13 * scale
      ], angle);
    }
    return pot;
  }

  localMesh(geometry, material, position, scale = [1, 1, 1], rotation = [0, 0, 0]) {
    const mesh = new THREE.Mesh(geometry, material);
    mesh.position.set(position[0], position[1], position[2]);
    mesh.scale.set(scale[0], scale[1], scale[2]);
    mesh.rotation.set(rotation[0], rotation[1], rotation[2]);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    return mesh;
  }
}
