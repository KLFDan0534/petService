export const TOUR_STOPS = [
  {
    key: "daycare",
    navLabel: "训练",
    label: "H01 日托训练大厅",
    progress: 0,
    camera: [-10.85, 2.42, 0.95],
    lookAt: [-8.55, 1.15, 4.35],
    description: "敏捷训练、社会化日托和训练师看护集中在第一间房，适合展示宠物托管与行为训练能力。",
    capacity: "4宠物/场",
    devices: "6组训练器具",
    staff: "3人",
    tone: "#56d68f"
  },
  {
    key: "spa",
    navLabel: "洗护",
    label: "H02 洗护水疗房",
    progress: 0.25,
    camera: [-1.55, 2.38, 0.95],
    lookAt: [0, 1.16, 4.35],
    description: "湿区地面、圆形洗护盆、淋浴轨道、烘干管线和美容台组成完整洗护流程。",
    capacity: "3工作位",
    devices: "7件洗护设备",
    staff: "3人",
    tone: "#38bdf8"
  },
  {
    key: "boarding",
    navLabel: "寄养",
    label: "H03 安静寄养房",
    progress: 0.5,
    camera: [11.15, 2.38, 1.05],
    lookAt: [8.55, 1.14, 4.42],
    description: "独立犬舍、隔音挡板、草地区和夜间照护动线，让寄养空间看起来更完整、更可信。",
    capacity: "4间套房",
    devices: "12组传感点",
    staff: "2人",
    tone: "#f5b14c"
  },
  {
    key: "catloft",
    navLabel: "猫舍",
    label: "H04 猫咪跃层乐园",
    progress: 0.75,
    camera: [-7.85, 2.68, -7.45],
    lookAt: [-4.42, 1.32, -3.42],
    description: "二层平台、墙面跳台、猫抓柱、吊桥和行为监测屏围绕猫行为设计。",
    capacity: "4只猫",
    devices: "9组跃层设施",
    staff: "2人",
    tone: "#d8a15c"
  },
  {
    key: "conservatory",
    navLabel: "温室",
    label: "H05 小动物温室",
    progress: 1,
    camera: [8.3, 2.68, -8.05],
    lookAt: [5.62, 1.26, -3.1],
    description: "八角温室、兔子围栏、鸟类栖架、浅水龟池和中心展示岛集中展示小动物照护能力。",
    capacity: "5小动物",
    devices: "8组生态设施",
    staff: "2人",
    tone: "#7ddfbe"
  }
];

export const INTRO_PATH = {
  from: [-16.5, 14.2, 24],
  mid: [-7.5, 8.4, 14.8],
  to: TOUR_STOPS[0].camera,
  lookFrom: [0, 0.6, 0],
  lookTo: TOUR_STOPS[0].lookAt,
  duration: 4.8
};

export const AREA_MODEL_MANIFEST = {
  daycare: "assets/models/daycare-training-hall.glb",
  spa: "assets/models/spa-grooming-house.glb",
  boarding: "assets/models/boarding-kennel-lodge.glb",
  catloft: "assets/models/cat-enrichment-loft.glb",
  conservatory: "assets/models/small-animal-conservatory.glb"
};

export const HOTSPOT_DEFINITIONS = [
  {
    id: "daycare-agility",
    areaKey: "daycare",
    label: "训练器具",
    color: 0x56d68f,
    position: [-8.18, 1.42, 3.1],
    title: "敏捷训练与社会化日托区",
    functionText: "跳杆、训练隧道、草地社交区和训练师点位共同支持行为训练。",
    dataText: "单场 4 只宠物，3 名工作人员协同看护。"
  },
  {
    id: "spa-basin",
    areaKey: "spa",
    label: "洗护盆",
    color: 0x38bdf8,
    position: [-1.3, 1.52, 3.95],
    title: "圆形智能洗护盆",
    functionText: "湿区地面、淋浴轨道、恒温水面和美容台串联完整洗护流程。",
    dataText: "3 个洗护工作位，支持清洁、吹干、皮肤检查联动。"
  },
  {
    id: "boarding-suite",
    areaKey: "boarding",
    label: "独立套房",
    color: 0xf5b14c,
    position: [8.6, 1.62, 5.35],
    title: "安静寄养套房",
    functionText: "独立犬舍、软垫、前栏和隔音挡板降低寄养压力。",
    dataText: "4 间套房，当前模拟入住率 75%。"
  },
  {
    id: "cat-loft",
    areaKey: "catloft",
    label: "跃层平台",
    color: 0xd8a15c,
    position: [-4.08, 2.18, -2.72],
    title: "猫咪跃层与吊桥",
    functionText: "二层平台、错落跳台、猫抓柱和吊桥形成垂直活动路线。",
    dataText: "9 组跃层设施，适配领养展示和猫咪寄养。"
  },
  {
    id: "small-animal-habitat",
    areaKey: "conservatory",
    label: "生态照护",
    color: 0x7ddfbe,
    position: [5.98, 1.08, -2.84],
    title: "小动物温室照护岛",
    functionText: "中心展示岛、兔子围栏、鸟类栖架、浅水龟池和气候控制屏共同呈现异宠照护。",
    dataText: "5 个小动物展示点，2 名工作人员负责照护。"
  }
];
